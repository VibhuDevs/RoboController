package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // Bluetooth Objects
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private final UUID SPP_UUID = UUID.fromString("00001101-0000-0000-1000-800000805F9B");

    // UI Elements
    private TextView tvStatus, tvCommand;
    private Button btnConnect;

    // Speech Object
    private SpeechRecognizer speechRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        tvCommand = findViewById(R.id.tvCommand);
        btnConnect = findViewById(R.id.btnConnect);
        FloatingActionButton btnMic = findViewById(R.id.btnMic);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Request Permissions
        checkPermissions();

        // Bluetooth Connect Button
        btnConnect.setOnClickListener(v -> showDevicePicker());

        // Mic Button
        setupSpeechRecognizer();
        btnMic.setOnClickListener(v -> startListening());
    }

    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.RECORD_AUDIO);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
        } else {
            permissions.add(Manifest.permission.BLUETOOTH);
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), 1);
    }

    private boolean hasBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // BLUETOOTH and BLUETOOTH_ADMIN are granted on install below API 31
    }

    private void showDevicePicker() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasBluetoothPermission()) {
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
            checkPermissions();
            return;
        }

        @SuppressLint("MissingPermission") 
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        ArrayList<String> deviceNames = new ArrayList<>();
        ArrayList<BluetoothDevice> devices = new ArrayList<>();

        for (BluetoothDevice device : pairedDevices) {
            @SuppressLint("MissingPermission")
            String name = device.getName();
            deviceNames.add(name + "\n" + device.getAddress());
            devices.add(device);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select your ESP32");
        builder.setItems(deviceNames.toArray(new CharSequence[0]), (dialog, which) -> connectToDevice(devices.get(which)));
        builder.show();
    }

    private void connectToDevice(BluetoothDevice device) {
        new Thread(() -> {
            try {
                if (!hasBluetoothPermission()) return;

                @SuppressLint("MissingPermission")
                BluetoothSocket tempSocket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                socket = tempSocket;
                
                @SuppressLint("MissingPermission")
                boolean connected = false;
                try {
                    socket.connect();
                    connected = true;
                } catch (IOException e) {
                    try { socket.close(); } catch (IOException ignored) {}
                }

                if (connected) {
                    outputStream = socket.getOutputStream();
                    runOnUiThread(() -> {
                        tvStatus.setText(R.string.status_connected);
                        tvStatus.setTextColor(0xFF4CAF50); // Green
                        btnConnect.setText(R.string.disconnect);
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, R.string.connection_failed, Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, R.string.connection_failed, Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String command = matches.get(0).toLowerCase();
                    processCommand(command);
                }
            }
            @Override public void onReadyForSpeech(Bundle params) { tvCommand.setText(R.string.listening); }
            @Override public void onError(int error) { tvCommand.setText(R.string.error_retry); }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startListening() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            checkPermissions();
            return;
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizer.startListening(intent);
    }

    private void processCommand(String command) {
        tvCommand.setText(getString(R.string.command_display, command.toUpperCase()));
        char code = ' ';

        if (command.contains("forward")) code = 'F';
        else if (command.contains("backward")) code = 'B';
        else if (command.contains("left")) code = 'L';
        else if (command.contains("right")) code = 'R';
        else if (command.contains("stop")) code = 'S';
        else if (command.contains("go")) code = 'G';

        if (code != ' ') {
            sendCommand(code);
        }
    }

    private void sendCommand(char c) {
        if (outputStream != null) {
            try {
                outputStream.write(c);
            } catch (IOException e) {
                tvStatus.setText(R.string.status_disconnected);
                tvStatus.setTextColor(0xFFFF5252); // Red
            }
        } else {
            Toast.makeText(this, R.string.connect_first, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) speechRecognizer.destroy();
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}