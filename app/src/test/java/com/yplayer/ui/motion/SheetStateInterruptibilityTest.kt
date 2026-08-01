package com.yplayer.ui.motion

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class SheetStateInterruptibilityTest {

    @Test
    fun reDragMidSettleCancelsSpringAndSnapsToFinger() = runComposeUiTest {
        lateinit var state: SheetState
        setContent { state = rememberSheetState() }

        runOnIdle { state.expand() }
        mainClock.advanceTimeBy(100)

        runOnIdle {
            state.onDragStart()
            state.applyDrag(-1f)
        }
        mainClock.advanceTimeBy(1000)

        runOnIdle {
            assertTrue("spring must be cancelled by re-drag", state.progress.value < 0.5f)
        }
    }
}
