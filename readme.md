# Robot Voice Control App

A simple Android application that controls an ESP32-based self-balancing two-wheeled robot using voice commands over Bluetooth.

## Overview

This project was developed as part of a robotics project involving a self-balancing robot. Instead of attaching a microphone directly to the robot, voice commands are given through an Android application. The app uses Android Speech Recognition to convert spoken commands into Bluetooth messages that are sent to the ESP32.

## Features

* Bluetooth connection to ESP32
* Voice command recognition
* Simple and user-friendly interface
* Real-time command transmission
* Large microphone button for easy operation
* Connection status display

## Supported Voice Commands

| Voice Command | Sent to ESP32 |
| ------------- | ------------- |
| Forward       | F             |
| Backward      | B             |
| Left          | L             |
| Right         | R             |
| Stop          | S             |

## How It Works

1. Pair the Android device with the ESP32.
2. Open the Robot Voice Control App.
3. Connect to the ESP32 using Bluetooth.
4. Press the microphone button.
5. Speak a command such as:

   * Forward
   * Backward
   * Left
   * Right
   * Stop
6. The app converts the command into a single-character instruction and sends it to the ESP32.
7. The robot performs the corresponding action.

## Technology Stack

* Java
* Android Studio
* Android Speech Recognition API
* Bluetooth Classic
* ESP32

## Project Structure

```text
app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── MainActivity.java
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_main.xml
│   │   │   └── values/
│   │   │       └── themes.xml
│   │   └── AndroidManifest.xml
```

## Permissions Used

* Bluetooth
* Bluetooth Connect
* Bluetooth Scan
* Record Audio


