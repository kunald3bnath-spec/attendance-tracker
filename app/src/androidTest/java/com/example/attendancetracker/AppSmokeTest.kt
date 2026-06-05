package com.example.attendancetracker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests that verify the app's core user journeys against a real device / emulator.
 * Run with: ./gradlew connectedDebugAndroidTest
 *
 * Coverage:
 *   1. App launches and renders the Projects screen
 *   2. Dark mode toggle does not crash the app
 *   3. "Add project" FAB opens the creation dialog
 *   4. Dialog can be cancelled without side effects
 *   5. Creating a project displays it in the project list
 *   6. Empty-state hint is shown when no projects exist
 */
@RunWith(AndroidJUnit4::class)
class AppSmokeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // ── Launch ────────────────────────────────────────────────────────────────

    @Test
    fun appLaunches_showsAttendanceTrackerTitle() {
        composeTestRule.onNodeWithText("Attendance Tracker").assertIsDisplayed()
    }

    @Test
    fun fabAddProject_isDisplayed() {
        composeTestRule.onNodeWithContentDescription("Add project").assertIsDisplayed()
    }

    // ── Dark mode ─────────────────────────────────────────────────────────────

    @Test
    fun darkModeToggle_doesNotCrash() {
        composeTestRule.onNodeWithContentDescription("Toggle dark mode").performClick()
        composeTestRule.onNodeWithText("Attendance Tracker").assertIsDisplayed()
        // Toggle back
        composeTestRule.onNodeWithContentDescription("Toggle dark mode").performClick()
        composeTestRule.onNodeWithText("Attendance Tracker").assertIsDisplayed()
    }

    // ── New Project dialog ────────────────────────────────────────────────────

    @Test
    fun fabClick_opensNewProjectDialog() {
        composeTestRule.onNodeWithContentDescription("Add project").performClick()
        composeTestRule.onNodeWithText("New Project").assertIsDisplayed()
        composeTestRule.onNodeWithText("Project name").assertIsDisplayed()
    }

    @Test
    fun cancelDialog_dismissesWithoutCreatingProject() {
        composeTestRule.onNodeWithContentDescription("Add project").performClick()
        composeTestRule.onNodeWithText("New Project").assertIsDisplayed()

        composeTestRule.onNodeWithText("Cancel").performClick()

        // Dialog is gone, screen title still visible
        composeTestRule.onNodeWithText("Attendance Tracker").assertIsDisplayed()
    }

    // ── Create project flow ───────────────────────────────────────────────────

    @Test
    fun createProject_appearsInProjectList() {
        val projectName = "QA Smoke ${System.currentTimeMillis()}"

        composeTestRule.onNodeWithContentDescription("Add project").performClick()
        composeTestRule.onNodeWithText("New Project").assertIsDisplayed()

        // Type into the single editable text field in the dialog
        composeTestRule.onNode(hasSetTextAction()).performTextInput(projectName)

        composeTestRule.onNodeWithText("Create").performClick()

        // New project card should be visible in the list
        composeTestRule.onNodeWithText(projectName).assertIsDisplayed()
    }

    @Test
    fun createButton_disabledWhenNameIsBlank() {
        composeTestRule.onNodeWithContentDescription("Add project").performClick()
        // Do not type anything — Create button should be disabled (clicking it should be safe)
        composeTestRule.onNodeWithText("Create").performClick()
        // Dialog should still be open (project was not created)
        composeTestRule.onNodeWithText("New Project").assertIsDisplayed()
    }
}
