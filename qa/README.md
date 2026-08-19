# Spoofer QA Automation

This directory contains the End-to-End (E2E) Appium testing framework for the Spoofer app.

## Tech Stack
* **Python 3.x**
* **Pytest** (Test runner)
* **Appium-Python-Client** (UI Automation)

## Setup Instructions

1. **Start Appium Server:**
   Ensure you have NodeJS and Appium installed.
   ```bash
   appium
   ```

2. **Start Android Emulator:**
   Launch an Android Virtual Device (AVD) via Android Studio or the command line. Ensure USB Debugging is enabled.

3. **Install Dependencies:**
   Navigate to this `qa/` directory and create a virtual environment:
   ```bash
   cd qa
   python3 -m venv venv
   source venv/bin/activate
   pip install -r requirements.txt
   ```

4. **Run the Tests:**
   The `conftest.py` will automatically locate the built APK (`app-debug.apk`) from the `app/build/` directory and install it on the emulator.
   
   Run all tests:
   ```bash
   pytest tests/
   ```
   
   Run specific suites using markers:
   ```bash
   pytest -m smoke
   pytest -m sanity
   pytest -m e2e
   ```
