# QA Test Cases

This document outlines the manual test scenarios designed to validate the core features of the Spoofer app.

## Test Suite: Smoke Testing
**TC001: App Launch**
* **Description:** Verify the app launches without crashing and renders the map.
* **Pre-conditions:** App is installed, Location permission is granted.
* **Steps:**
  1. Launch the Spoofer app.
* **Expected Result:** The map is displayed, and the "Start spoofing" button is visible at the bottom.

## Test Suite: Sanity Testing
**TC002: Toggle Mock Location (Static)**
* **Description:** Verify the user can start and stop spoofing a static location.
* **Steps:**
  1. Open the app.
  2. Tap "Start spoofing".
  3. Verify the button changes to "Stop spoofing".
  4. Verify the Status Chip appears at the top.
  5. Tap "Stop spoofing".
* **Expected Result:** The service toggles successfully, and the UI reflects the active state.

## Test Suite: End-to-End (E2E) Journeys
**TC003: Directions Route Spoofing**
* **Description:** Verify a user can search for a destination, view a route, and start moving along it.
* **Steps:**
  1. Tap the Search bar.
  2. Enter a valid city name (e.g., "San Francisco").
  3. Select the destination from the dropdown.
  4. Open the Bottom Sheet and select the "Directions" tab.
  5. Tap "Start spoofing".
* **Expected Result:** The mock location updates incrementally along the blue route line according to the selected speed.
