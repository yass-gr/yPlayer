package com.yplayer.ui

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.yplayer.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.POST_NOTIFICATIONS,
    )

    @Test
    fun bottomNavSwitchesBetweenTabs() {
        composeRule.onNodeWithText("Songs placeholder").assertIsDisplayed()
        composeRule.onNodeWithText("Albums").performClick()
        composeRule.onNodeWithText("Albums placeholder").assertIsDisplayed()
        composeRule.onNodeWithText("Artists").performClick()
        composeRule.onNodeWithText("Artists placeholder").assertIsDisplayed()
        composeRule.onNodeWithText("Playlists").performClick()
        composeRule.onNodeWithText("Playlists placeholder").assertIsDisplayed()
        composeRule.onNodeWithText("Songs").performClick()
        composeRule.onNodeWithText("Songs placeholder").assertIsDisplayed()
    }
}
