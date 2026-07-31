package com.yplayer.ui

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.yplayer.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.POST_NOTIFICATIONS,
    )

    @Test
    fun searchField_acceptsInput() {
        composeRule.onNodeWithText("yPlayer").assertIsDisplayed()
        composeRule.onNodeWithText("Search").performClick()
        composeRule.onNodeWithText("Search songs, artists, albums").assertIsDisplayed()
        composeRule.onNodeWithText("Search songs, artists, albums").performTextInput("weeknd")
        composeRule.onNodeWithText("weeknd").assertIsDisplayed()
    }
}
