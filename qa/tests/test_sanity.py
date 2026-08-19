import pytest
from appium.webdriver.common.appiumby import AppiumBy
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import time

@pytest.mark.sanity
def test_toggle_spoofing(driver):
    """
    Verify that tapping the FAB toggles the spoofing state.
    """
    # Find the FAB
    xpath = "//*[contains(@text, 'Start spoofing') or contains(@text, 'Stop spoofing')]"
    fab = WebDriverWait(driver, 10).until(
        EC.element_to_be_clickable((AppiumBy.XPATH, xpath))
    )
    
    initial_text = fab.text
    fab.click()
    
    # Wait for state to change
    time.sleep(2)
    
    fab = driver.find_element(AppiumBy.XPATH, xpath)
    assert fab.text != initial_text
    
    # Toggle back
    fab.click()
