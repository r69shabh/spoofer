import pytest
from appium import webdriver
from appium.options.android import UiAutomator2Options
import os

@pytest.fixture(scope="session")
def driver():
    """
    Setup Appium WebDriver for Android.
    Assumes an emulator is running and Appium server is listening on port 4723.
    """
    apk_path = os.path.abspath(
        os.path.join(os.path.dirname(__file__), "..", "app", "build", "outputs", "apk", "debug", "app-debug.apk")
    )

    options = UiAutomator2Options()
    options.platform_name = "Android"
    options.automation_name = "UiAutomator2"
    # Wait for the app to load
    options.app_wait_activity = "com.spoofer.MainActivity"
    
    # If the APK exists locally, install it
    if os.path.exists(apk_path):
        options.app = apk_path
    else:
        # Otherwise assume it's already installed
        options.app_package = "com.spoofer"
        options.app_activity = ".MainActivity"
    
    options.no_reset = False
    options.new_command_timeout = 300

    driver = webdriver.Remote("http://127.0.0.1:4723", options=options)
    driver.implicitly_wait(10)
    
    yield driver
    
    driver.quit()
