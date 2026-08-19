import pytest
from appium.webdriver.common.appiumby import AppiumBy
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

@pytest.mark.smoke
def test_app_launches(driver):
    """
    Verify that the app launches and the main map screen is visible.
    """
    # Wait for the Start spoofing button (or Stop if it was already running)
    xpath = "//*[contains(@text, 'Start spoofing') or contains(@text, 'Stop spoofing')]"
    fab = WebDriverWait(driver, 10).until(
        EC.presence_of_element_located((AppiumBy.XPATH, xpath))
    )
    assert fab is not None
