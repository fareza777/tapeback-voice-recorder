package com.f7developer.tapeback

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.f7developer.tapeback.domain.model.AppSettings
import com.f7developer.tapeback.ui.components.KeySymbol
import com.f7developer.tapeback.ui.components.TransportKey
import com.f7developer.tapeback.ui.components.TransportKeys
import com.f7developer.tapeback.ui.theme.TapeBackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransportKeysSemanticsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun keysExposeDescriptionsLatchedStateAndClicks() {
        var playPresses = 0
        rule.setContent {
            TapeBackTheme(AppSettings()) {
                TransportKeys(
                    keys = listOf(
                        TransportKey(KeySymbol.REC, "REC", "Record", latched = false, enabled = true, onPress = {}),
                        TransportKey(KeySymbol.PLAY, "PLAY", "Play", latched = true, enabled = true, onPress = { playPresses++ }),
                        TransportKey(KeySymbol.PAUSE, "PAUSE", "Pause", latched = false, enabled = false, onPress = {}),
                    ),
                )
            }
        }
        rule.onNodeWithContentDescription("Record").assertHasClickAction().assertIsNotSelected()
        rule.onNodeWithContentDescription("Play").assertIsSelected().performClick()
        rule.onNodeWithContentDescription("Pause").assertIsNotEnabled()
        rule.runOnIdle { assertEquals(1, playPresses) }
    }
}
