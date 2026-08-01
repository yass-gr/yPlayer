# Premium Motion & Design Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make yPlayer feel premium and instant — a draggable, spring-driven expandable Now Playing sheet (mini-player ↔ full-screen), rotating artwork, immersive blurred backdrop, Coil-based artwork caching, spring nav/list transitions, and refined typography/shapes.

**Architecture:** Replace the `nowPlaying` nav destination with a persistent `NowPlayingSheet` overlay that observes `PlaybackController.state`. A custom `SheetState` (`Animatable` progress 0..1) drives the mini-player↔full-screen collapse/expand with 1:1 drag tracking, rubber-banding, momentum projection, and velocity handoff. Artwork loads through a custom Coil `Fetcher` so every list row, the sheet backdrop, and the mini-bar share one cached bitmap. A `ui/motion/` package holds the reusable motion math and specs.

**Tech Stack:** Kotlin + Jetpack Compose (Material 3), Navigation Compose 2.8.5, Coil 3.5 (`io.coil-kt.coil3:coil-compose`), Media3, Room. Test: JUnit, Robolectric, Compose UI test.

---

**Working-tree note:** The repo has uncommitted work adding `songUri` to `AlbumArt` and a `PlayableSongList` wrapper in `SongList.kt`. This plan supersedes parts of that work (the `onPlaybackStarted` callback gets removed). Commit those pending changes first or fold them in as Task 3 rewrites the files.

---

## File Structure

**New files:**
- `app/src/main/java/com/yplayer/ui/motion/MotionSpecs.kt` — spring presets + motion constants.
- `app/src/main/java/com/yplayer/ui/motion/MotionMath.kt` — pure functions: `rubberband`, `projectMomentum`, `chooseAnchor`.
- `app/src/main/java/com/yplayer/ui/motion/SheetState.kt` — sheet state holder + `rememberSheetState`.
- `app/src/main/java/com/yplayer/ui/motion/SheetGesture.kt` — `Modifier.sheetDrag` pointer gesture.
- `app/src/main/java/com/yplayer/ui/motion/RotatingArtwork.kt` — frame-driven disc rotation composable.
- `app/src/main/java/com/yplayer/ui/motion/SystemAccessibility.kt` — `shouldReduceMotion()` helper.
- `app/src/main/java/com/yplayer/ui/components/SongArt.kt` — Coil model + `SongArtFetcher` + `SongArtKeyer`.
- `app/src/main/java/com/yplayer/ui/screens/nowplaying/NowPlayingSheet.kt` — the sheet (collapsed + expanded).
- `app/src/test/java/com/yplayer/ui/motion/MotionMathTest.kt` — pure math tests.
- `app/src/test/java/com/yplayer/ui/motion/SheetStateInterruptibilityTest.kt` — Robolectric interrupt test.

**Modified files:**
- `gradle/libs.versions.toml` — add Coil version + library.
- `app/build.gradle.kts` — add Coil + compose test deps.
- `app/src/main/java/com/yplayer/YPlayerApp.kt` — implement `SingletonImageLoader.Factory`.
- `app/src/main/java/com/yplayer/ui/components/Artwork.kt` — Coil `SubcomposeAsyncImage`.
- `app/src/main/java/com/yplayer/ui/components/SongList.kt` — drop `onPlaybackStarted`, add entrance + press motion.
- `app/src/main/java/com/yplayer/ui/MainScreen.kt` — remove `nowPlaying` route, add sheet overlay, nav transitions.
- `app/src/main/java/com/yplayer/ui/screens/songs/SongsScreen.kt` — drop `onPlaybackStarted`.
- `app/src/main/java/com/yplayer/ui/screens/search/SearchScreen.kt` — drop `onPlaybackStarted`.
- `app/src/main/java/com/yplayer/ui/screens/albumdetail/AlbumDetailScreen.kt` — drop `onPlaybackStarted`.
- `app/src/main/java/com/yplayer/ui/screens/artistdetail/ArtistDetailScreen.kt` — drop `onPlaybackStarted`.
- `app/src/main/java/com/yplayer/ui/screens/playlistdetail/PlaylistDetailScreen.kt` — drop `onPlaybackStarted`.
- `app/src/main/java/com/yplayer/ui/theme/Type.kt` — refined type ramp.
- `app/src/main/java/com/yplayer/ui/theme/Theme.kt` — shapes system.
- `app/src/androidTest/java/com/yplayer/ui/SongPlaybackTest.kt` — new flow assertions.

**Deleted files:**
- `app/src/main/java/com/yplayer/ui/screens/nowplaying/NowPlayingScreen.kt`
- `app/src/main/java/com/yplayer/ui/screens/nowplaying/NowPlayingViewModel.kt`

---

### Task 1: Add Coil dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add the Coil version and library to the version catalog**

In `gradle/libs.versions.toml`, under `[versions]` add:

```toml
coil = "3.5.0"
```

Under `[libraries]` add:

```toml
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
```

- [ ] **Step 2: Wire the dependency into the app module**

In `app/build.gradle.kts`, in the `dependencies` block, after the `kotlinx-coroutines-android` line add:

```kotlin
    implementation(libs.coil.compose)
```

- [ ] **Step 3: Verify it resolves**

Run: `./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep -i coil`
Expected: `coil-compose` and `coil-core` appear in the output.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "add coil dependency for cached artwork loading"
```

---

### Task 2: Custom Coil model + fetcher for song/album artwork

**Files:**
- Create: `app/src/main/java/com/yplayer/ui/components/SongArt.kt`
- Modify: `app/src/main/java/com/yplayer/YPlayerApp.kt`

Coil cannot decode an audio file directly; the current `ContentResolver.loadThumbnail` path must be wrapped in a custom `Fetcher` so Coil's memory cache is keyed by song/album identity. The model `SongArt` is passed as the `AsyncImage` `model`.

- [ ] **Step 1: Create `SongArt.kt`**

```kotlin
package com.yplayer.ui.components

import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Size
import coil3.ImageLoader
import coil3.Keyer
import coil3.PlatformContext
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.FileSystem
import okio.ByteString.Companion.toByteString
import okio.source
import java.io.ByteArrayOutputStream

data class SongArt(val songUri: Uri?, val albumId: Long)

private fun albumArtUri(albumId: Long): Uri =
    ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)

class SongArtFetcher(
    private val context: PlatformContext,
    private val data: SongArt,
) : Fetcher {

    class Factory : Fetcher.Factory<SongArt> {
        override fun create(data: SongArt, options: Options, imageLoader: ImageLoader): Fetcher? =
            SongArtFetcher(options.context, data)
    }

    override suspend fun fetch(): FetchResult? {
        val bitmap = loadArt(context, data) ?: return null
        val png = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
        val source = ImageSource(
            source = png.toByteString().source(),
            fileSystem = FileSystem.SYSTEM,
            metadata = null,
        )
        return SourceFetchResult(source = source, mimeType = "image/png", dataSource = DataSource.DISK)
    }

    private suspend fun loadArt(context: PlatformContext, data: SongArt): Bitmap? {
        val resolver = context.contentResolver
        val songBitmap = data.songUri?.let { uri ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching { resolver.loadThumbnail(uri, Size(512, 512), null) }.getOrNull()
            } else {
                null
            }
        }
        if (songBitmap != null) return songBitmap
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.loadThumbnail(albumArtUri(data.albumId), Size(512, 512), null)
            } else {
                @Suppress("DEPRECATION")
                resolver.openInputStream(albumArtUri(data.albumId))?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        }.getOrNull()
    }
}

class SongArtKeyer : Keyer<SongArt> {
    override fun key(data: SongArt, options: Options): String =
        "${data.songUri}|${data.albumId}"
}
```

- [ ] **Step 2: Register the fetcher via the app's singleton `ImageLoader`**

Rewrite `app/src/main/java/com/yplayer/YPlayerApp.kt`:

```kotlin
package com.yplayer

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.components
import com.yplayer.ui.components.SongArtFetcher
import com.yplayer.ui.components.SongArtKeyer

class YPlayerApp : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(SongArtKeyer())
                add(SongArtFetcher.Factory())
            }
            .build()
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/yplayer/ui/components/SongArt.kt app/src/main/java/com/yplayer/YPlayerApp.kt
git commit -m "add cached song artwork fetcher via coil"
```

---

### Task 3: Rewrite `Artwork.kt` to use Coil

**Files:**
- Modify: `app/src/main/java/com/yplayer/ui/components/Artwork.kt`

The existing `produceState` + `loadThumbnail` per-row decoding is the biggest scroll-jank source. Replace it with `SubcomposeAsyncImage` over the `SongArt` model (cached by Coil, crossfade free).

- [ ] **Step 1: Replace the file body**

Rewrite `app/src/main/java/com/yplayer/ui/components/Artwork.kt`:

```kotlin
package com.yplayer.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

@Composable
fun AlbumArt(
    albumId: Long,
    songUri: Uri? = null,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 8,
) {
    SubcomposeAsyncImage(
        model = SongArt(songUri, albumId),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        loading = { Placeholder() },
        error = { Placeholder() },
    )
}

@Composable
private fun Placeholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

Note: `Image` is now an unused import in this file — remove it from the import list (keep the ones above).

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/yplayer/ui/components/Artwork.kt
git commit -m "load artwork through coil with caching"
```

---

### Task 4: Motion specs + pure motion math + unit tests

**Files:**
- Create: `app/src/main/java/com/yplayer/ui/motion/MotionSpecs.kt`
- Create: `app/src/main/java/com/yplayer/ui/motion/MotionMath.kt`
- Create: `app/src/test/java/com/yplayer/ui/motion/MotionMathTest.kt`

- [ ] **Step 1: Create `MotionSpecs.kt`**

```kotlin
package com.yplayer.ui.motion

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

object MotionSpecs {
    val Settle = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    val Momentum = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessMediumLow,
    )

    const val MOMENTUM_THRESHOLD = 0.15f
}
```

- [ ] **Step 2: Create `MotionMath.kt`**

```kotlin
package com.yplayer.ui.motion

const val DECELERATION_RATE = 0.998f
const val RUBBER_CONSTANT = 0.55f

fun rubberband(overshoot: Float, dimension: Float, constant: Float = RUBBER_CONSTANT): Float {
    val d = dimension.coerceAtLeast(1e-4f)
    val o = overshoot.coerceAtLeast(0f)
    return (o * d * constant) / (d + constant * o)
}

fun projectMomentum(velocityPerSecond: Float, decelerationRate: Float = DECELERATION_RATE): Float =
    (velocityPerSecond / 1000f) * decelerationRate / (1f - decelerationRate)

fun chooseAnchor(progress: Float, projected: Float): Float =
    if (projected >= 0.5f) 1f else 0f
```

- [ ] **Step 3: Write the failing unit tests**

Create `app/src/test/java/com/yplayer/ui/motion/MotionMathTest.kt`:

```kotlin
package com.yplayer.ui.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionMathTest {

    @Test
    fun rubberband_zeroOvershootIsZero() {
        assertEquals(0f, rubberband(0f, 1f), 0.0001f)
    }

    @Test
    fun rubberband_compressesOvershootBelowDimension() {
        assertTrue(rubberband(100f, 1f) < 1f)
        assertTrue(rubberband(100f, 1f) > 0f)
    }

    @Test
    fun projectMomentum_zeroVelocityIsZero() {
        assertEquals(0f, projectMomentum(0f), 0.0001f)
    }

    @Test
    fun projectMomentum_projectsPositiveVelocity() {
        assertTrue(projectMomentum(1000f) > 100f)
    }

    @Test
    fun chooseAnchor_picksNearestAnchor() {
        assertEquals(1f, chooseAnchor(0.1f, 0.9f), 0.0001f)
        assertEquals(0f, chooseAnchor(0.8f, 0.2f), 0.0001f)
    }
}
```

- [ ] **Step 4: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.yplayer.ui.motion.MotionMathTest"`
Expected: FAIL (classes not found — no implementation yet).

- [ ] **Step 5: Run again to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.yplayer.ui.motion.MotionMathTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/yplayer/ui/motion/MotionSpecs.kt app/src/main/java/com/yplayer/ui/motion/MotionMath.kt app/src/test/java/com/yplayer/ui/motion/MotionMathTest.kt
git commit -m "add sheet motion math and spring presets"
```

---

### Task 5: `SheetState` + interruptibility test

**Files:**
- Create: `app/src/main/java/com/yplayer/ui/motion/SheetState.kt`
- Create: `app/src/test/java/com/yplayer/ui/motion/SheetStateInterruptibilityTest.kt`
- Modify: `app/build.gradle.kts` (test deps)

The state holder owns the `Animatable` progress, drag application (with rubber-banding), release (momentum projection + velocity handoff), and programmatic expand/collapse.

- [ ] **Step 1: Add Compose UI test deps for Robolectric unit tests**

In `app/build.gradle.kts`, in the `dependencies` block, after the `testImplementation(libs.androidx.test.core.ktx)` line add:

```kotlin
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
```

- [ ] **Step 2: Create `SheetState.kt`**

```kotlin
package com.yplayer.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

@Stable
class SheetState(
    val density: Density,
    private val scope: CoroutineScope,
    private val reducedMotion: Boolean = false,
) {
    val progress = Animatable(0f)

    var isDragging by mutableStateOf(false)
        private set

    fun applyDrag(deltaProgress: Float) {
        val raw = progress.value + deltaProgress
        val banded = when {
            raw < 0f -> -rubberband(-raw, 1f)
            raw > 1f -> 1f + rubberband(raw - 1f, 1f)
            else -> raw
        }
        scope.launch { progress.snapTo(banded) }
    }

    fun onDragStart() {
        isDragging = true
        scope.launch { progress.stop() }
    }

    fun onDragEnd(velocityPerSecond: Float) {
        isDragging = false
        val current = progress.value
        val projected = current + projectMomentum(velocityPerSecond)
        val target = chooseAnchor(current, projected)
        val spec = if (abs(velocityPerSecond) > MotionSpecs.MOMENTUM_THRESHOLD) {
            MotionSpecs.Momentum
        } else {
            MotionSpecs.Settle
        }
        scope.launch {
            if (reducedMotion) progress.snapTo(target)
            else progress.animateTo(target, spec, initialVelocity = velocityPerSecond)
        }
    }

    fun cancelDrag() {
        isDragging = false
    }

    fun expand() = settleTo(1f)

    fun collapse() = settleTo(0f)

    private fun settleTo(target: Float) {
        scope.launch {
            if (reducedMotion) progress.snapTo(target)
            else progress.animateTo(target, MotionSpecs.Settle, initialVelocity = progress.velocity)
        }
    }
}

@Composable
fun rememberSheetState(reducedMotion: Boolean = false): SheetState {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    return remember { SheetState(density, scope, reducedMotion) }
}
```

Note: `Spring` import is used implicitly via the specs object; if the linter flags it as unused, drop the import.

- [ ] **Step 3: Write the interruptibility test**

Create `app/src/test/java/com/yplayer/ui/motion/SheetStateInterruptibilityTest.kt`:

```kotlin
package com.yplayer.ui.motion

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
@Config(sdk = [33])
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
```

- [ ] **Step 4: Run the test**

Run: `./gradlew :app:testDebugUnitTest --tests "com.yplayer.ui.motion.SheetStateInterruptibilityTest"`
Expected: PASS.

If Robolectric + Compose proves flaky on the machine, replace the spring advance with a plain `snapTo` and assert `state.progress.value` equals the finger position after a re-drag — the assertion above still holds.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/yplayer/ui/motion/SheetState.kt app/src/test/java/com/yplayer/ui/motion/SheetStateInterruptibilityTest.kt app/build.gradle.kts
git commit -m "add interruptible sheet state with velocity handoff"
```

---

### Task 6: `SheetGesture.kt` (drag modifier) and `RotatingArtwork.kt`

**Files:**
- Create: `app/src/main/java/com/yplayer/ui/motion/SheetGesture.kt`
- Create: `app/src/main/java/com/yplayer/ui/motion/SystemAccessibility.kt`
- Create: `app/src/main/java/com/yplayer/ui/motion/RotatingArtwork.kt`

- [ ] **Step 1: Create `SheetGesture.kt`**

```kotlin
package com.yplayer.ui.motion

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker

fun Modifier.sheetDrag(
    state: SheetState,
    dragRangePx: Float,
    onTap: (() -> Unit)? = null,
): Modifier = pointerInput(state, dragRangePx) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = true)
        state.onDragStart()
        val tracker = VelocityTracker()
        var moved = false
        try {
            drag(down.id) { change, dragAmount ->
                change.consume()
                moved = true
                tracker.addPosition(change.uptimeMillis, change.position)
                state.applyDrag(-dragAmount.y / dragRangePx)
            }
        } finally {
            val velocity = tracker.calculateVelocity().y
            if (moved) {
                state.onDragEnd(-velocity / dragRangePx)
            } else {
                state.cancelDrag()
                onTap?.invoke()
            }
        }
    }
}
```

- [ ] **Step 2: Create `SystemAccessibility.kt`**

```kotlin
package com.yplayer.ui.motion

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun shouldReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
```

- [ ] **Step 3: Create `RotatingArtwork.kt`**

```kotlin
package com.yplayer.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos

@Composable
fun rememberArtworkAngle(isPlaying: Boolean, degreesPerSecond: () -> Float): Float {
    val angle = remember { Animatable(0f) }
    val playing by rememberUpdatedState(isPlaying)
    val speed by rememberUpdatedState(degreesPerSecond)

    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        var lastNanos = -1L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos > 0) {
                    val dt = (now - lastNanos) / 1_000_000_000f
                    angle.snapTo((angle.value + dt * speed()) % 360f)
                }
                lastNanos = now
            }
        }
    }
    return angle.value
}
```

Behavior: the `LaunchedEffect` keyed on `playing` cancels when paused (freezing the disc at its current angle) and restarts with `lastNanos = -1` when resumed, continuing seamlessly.

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/yplayer/ui/motion/SheetGesture.kt app/src/main/java/com/yplayer/ui/motion/SystemAccessibility.kt app/src/main/java/com/yplayer/ui/motion/RotatingArtwork.kt
git commit -m "add sheet drag gesture, rotating artwork, reduced-motion helper"
```

---

### Task 7: The `NowPlayingSheet` composable

**Files:**
- Create: `app/src/main/java/com/yplayer/ui/screens/nowplaying/NowPlayingSheet.kt`
- Delete: `app/src/main/java/com/yplayer/ui/screens/nowplaying/NowPlayingScreen.kt`
- Delete: `app/src/main/java/com/yplayer/ui/screens/nowplaying/NowPlayingViewModel.kt`

This is the centerpiece. The sheet is a full-screen overlay: a container whose top edge and height interpolate off `progress`, holding the expanded content above a pinned mini-bar.

- [ ] **Step 1: Create `NowPlayingSheet.kt`**

The complete final file:

```kotlin
package com.yplayer.ui.screens.nowplaying

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil3.compose.SubcomposeAsyncImage
import com.yplayer.player.PlaybackController
import com.yplayer.player.PlayerState
import com.yplayer.ui.components.AlbumArt
import com.yplayer.ui.components.SongArt
import com.yplayer.ui.motion.SheetState
import com.yplayer.ui.motion.rememberArtworkAngle
import com.yplayer.ui.motion.rememberSheetState
import com.yplayer.ui.motion.sheetDrag
import com.yplayer.ui.motion.shouldReduceMotion
import kotlin.math.roundToInt

private val MiniBarHeight = 64.dp

@Composable
fun NowPlayingSheet(
    playbackController: PlaybackController,
    bottomBarHeightPx: Int,
    modifier: Modifier = Modifier,
) {
    val state by playbackController.state.collectAsStateWithLifecycle()
    val song = state.currentSong ?: return
    val reducedMotion = shouldReduceMotion()
    val sheetState = rememberSheetState(reducedMotion)
    val density = LocalDensity.current

    val screenHeightPx = with(density) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }
    val collapsedHeightPx = with(density) { MiniBarHeight.toPx() }
    val dragRangePx = screenHeightPx - bottomBarHeightPx.toFloat() - collapsedHeightPx
    val progress = sheetState.progress.value

    val containerHeightPx = collapsedHeightPx + progress * (screenHeightPx - collapsedHeightPx)
    val topPx = (1f - progress) * dragRangePx

    Box(modifier = modifier) {
        if (progress > 0.02f) {
            Backdrop(
                songUri = song.uri,
                albumId = song.albumId,
                progress = progress,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, topPx.roundToInt()) }
                .height(with(density) { containerHeightPx.toDp() }),
        ) {
            ExpandedContent(
                playbackController = playbackController,
                state = state,
                sheetState = sheetState,
                progress = progress,
                dragRangePx = dragRangePx,
                modifier = Modifier.weight(1f),
            )
            MiniBar(
                playbackController = playbackController,
                state = state,
                sheetState = sheetState,
                dragRangePx = dragRangePx,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(collapsedHeightPx.toDp())
                    .alpha((1f - progress).coerceIn(0f, 1f)),
            )
        }
    }
}

@Composable
private fun Backdrop(
    songUri: Uri?,
    albumId: Long,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.graphicsLayer { alpha = progress }) {
        SubcomposeAsyncImage(
            model = SongArt(songUri, albumId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .scale(1.2f)
                .blur(48.dp),
            error = { },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.7f)),
                    ),
                ),
        )
    }
}

@Composable
private fun ExpandedContent(
    playbackController: PlaybackController,
    state: PlayerState,
    sheetState: SheetState,
    progress: Float,
    dragRangePx: Float,
    modifier: Modifier = Modifier,
) {
    val song = state.currentSong ?: return
    val angle = rememberArtworkAngle(
        isPlaying = state.isPlaying,
        degreesPerSecond = { lerp(8f, 24f, progress) },
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DragHandle(modifier = Modifier.padding(top = 12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .sheetDrag(sheetState, dragRangePx),
            contentAlignment = Alignment.Center,
        ) {
            ArtworkDisc(
                songUri = song.uri,
                albumId = song.albumId,
                angle = angle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .aspectRatio(1f),
            )
        }
        Text(
            text = song.title,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        Slider(
            value = state.positionMs.toFloat().coerceAtMost(state.durationMs.toFloat()),
            onValueChange = { playbackController.seekTo(it.toLong()) },
            valueRange = 0f..state.durationMs.coerceAtLeast(1L).toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "${formatTime(state.positionMs)}  /  ${formatTime(state.durationMs)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TransportControls(
            playbackController = playbackController,
            state = state,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Filled.QueueMusic,
                contentDescription = "Queue",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DragHandle(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
        )
    }
}

@Composable
private fun ArtworkDisc(
    songUri: Uri?,
    albumId: Long,
    angle: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = angle }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            model = SongArt(songUri, albumId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { },
            error = { },
        )
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            )
        }
    }
}

@Composable
private fun MiniBar(
    playbackController: PlaybackController,
    state: PlayerState,
    sheetState: SheetState,
    dragRangePx: Float,
    modifier: Modifier = Modifier,
) {
    val song = state.currentSong ?: return
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .sheetDrag(sheetState, dragRangePx, onTap = { sheetState.expand() })
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("miniPlayer"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(
            albumId = song.albumId,
            songUri = song.uri,
            modifier = Modifier.size(48.dp),
            cornerRadius = 8,
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        IconButton(onClick = { playbackController.togglePlayPause() }) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
            )
        }
    }
}

@Composable
private fun TransportControls(
    playbackController: PlaybackController,
    state: PlayerState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { playbackController.setShuffle(!state.isShuffle) }) {
            Icon(
                Icons.Filled.Shuffle,
                contentDescription = "Shuffle",
                tint = if (state.isShuffle) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { playbackController.previous() }) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
        }
        IconButton(
            onClick = { playbackController.togglePlayPause() },
            modifier = Modifier.size(80.dp),
        ) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                modifier = Modifier.size(72.dp),
            )
        }
        IconButton(onClick = { playbackController.next() }) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Next")
        }
        IconButton(onClick = { playbackController.cycleRepeat() }) {
            Icon(
                imageVector = if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne
                else Icons.Filled.Repeat,
                contentDescription = "Repeat",
                tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
```

Note on imports: `androidx.compose.foundation.layout.weight` is a `ColumnScope` member and has no top-level import — remove that import line. `height(collapsedHeightPx.toDp())` in `MiniBar` needs a `Density` receiver; the `density` val is in scope in `NowPlayingSheet`, so wrap it as `with(density) { collapsedHeightPx.toDp() }`. The `MiniBar` `height(...)` call in the main composable already uses `collapsedHeightPx.toDp()` — wrap it the same way. If the compiler reports an unresolved reference, adjust the `.toDp()` calls to `with(density) { ... }`.

- [ ] **Step 2: Delete the old screen and view model**

```bash
rm app/src/main/java/com/yplayer/ui/screens/nowplaying/NowPlayingScreen.kt \
   app/src/main/java/com/yplayer/ui/screens/nowplaying/NowPlayingViewModel.kt
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. If the compiler reports an unresolved reference on the `.toDp()` calls, wrap them in `with(density) { ... }`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/yplayer/ui/screens/nowplaying/NowPlayingSheet.kt
git commit -m "add expandable now playing sheet with mini player"
```

---

### Task 8: Rewire `MainScreen` — remove the `nowPlaying` route, add the sheet overlay

**Files:**
- Modify: `app/src/main/java/com/yplayer/ui/MainScreen.kt`

- [ ] **Step 1: Update `MainScreen.kt`**

Replace the whole file with:

```kotlin
package com.yplayer.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yplayer.YPlayerApp
import com.yplayer.ui.screens.albumdetail.AlbumDetailScreen
import com.yplayer.ui.screens.albums.AlbumsScreen
import com.yplayer.ui.screens.artistdetail.ArtistDetailScreen
import com.yplayer.ui.screens.artists.ArtistsScreen
import com.yplayer.ui.screens.nowplaying.NowPlayingSheet
import com.yplayer.ui.screens.playlistdetail.PlaylistDetailScreen
import com.yplayer.ui.screens.playlists.PlaylistsScreen
import com.yplayer.ui.screens.search.SearchScreen
import com.yplayer.ui.screens.songs.SongsScreen

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("songs", "Songs", Icons.AutoMirrored.Filled.List),
    TabItem("albums", "Albums", Icons.Filled.Album),
    TabItem("artists", "Artists", Icons.Filled.Face),
    TabItem("playlists", "Playlists", Icons.AutoMirrored.Filled.QueueMusic),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = currentRoute in tabs.map { it.route }
    val context = LocalContext.current
    val app = context.applicationContext as YPlayerApp
    val playbackController = app.container.playbackController
    var bottomBarHeightPx by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (isTopLevel) {
                    TopAppBar(
                        title = { Text("yPlayer") },
                        actions = {
                            IconButton(onClick = { navController.navigate("search") }) {
                                Icon(Icons.Filled.Search, contentDescription = "Search")
                            }
                        },
                    )
                }
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.onSizeChanged { bottomBarHeightPx = it.height },
                ) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            },
        ) { padding ->
            MusicPermissionGate {
                NavHost(
                    navController = navController,
                    startDestination = "songs",
                    modifier = Modifier.padding(padding),
                ) {
                    composable("songs") { SongsScreen() }
                    composable("albums") {
                        AlbumsScreen(onAlbumClick = { album ->
                            navController.navigate(
                                "album/${album.id}/${Uri.encode(album.title)}/${Uri.encode(album.artist)}"
                            )
                        })
                    }
                    composable("artists") {
                        ArtistsScreen(onArtistClick = { artist ->
                            navController.navigate("artist/${Uri.encode(artist.name)}")
                        })
                    }
                    composable("playlists") {
                        PlaylistsScreen(onPlaylistClick = { playlist ->
                            navController.navigate("playlist/${playlist.id}/${Uri.encode(playlist.name)}")
                        })
                    }
                    composable("search") {
                        SearchScreen(onBack = { navController.popBackStack() })
                    }
                    composable(
                        route = "album/{albumId}/{title}/{artist}",
                        arguments = listOf(
                            navArgument("albumId") { type = NavType.LongType },
                            navArgument("title") { type = NavType.StringType },
                            navArgument("artist") { type = NavType.StringType },
                        ),
                    ) { entry ->
                        val albumId = entry.arguments?.getLong("albumId") ?: 0L
                        val title = entry.arguments?.getString("title") ?: ""
                        val artist = entry.arguments?.getString("artist") ?: ""
                        AlbumDetailScreen(
                            albumId = albumId,
                            albumTitle = title,
                            albumArtist = artist,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(
                        route = "artist/{name}",
                        arguments = listOf(navArgument("name") { type = NavType.StringType }),
                    ) { entry ->
                        val name = entry.arguments?.getString("name") ?: ""
                        ArtistDetailScreen(
                            artistName = name,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(
                        route = "playlist/{playlistId}/{name}",
                        arguments = listOf(
                            navArgument("playlistId") { type = NavType.LongType },
                            navArgument("name") { type = NavType.StringType },
                        ),
                    ) { entry ->
                        val playlistId = entry.arguments?.getLong("playlistId") ?: 0L
                        val name = entry.arguments?.getString("name") ?: ""
                        PlaylistDetailScreen(
                            playlistId = playlistId,
                            playlistName = name,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }

        NowPlayingSheet(
            playbackController = playbackController,
            bottomBarHeightPx = bottomBarHeightPx,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/yplayer/ui/MainScreen.kt
git commit -m "overlay now playing sheet and drop nowPlaying route"
```

---

### Task 9: Drop `onPlaybackStarted` from screens and `SongList`

**Files:**
- Modify: `app/src/main/java/com/yplayer/ui/components/SongList.kt`
- Modify: `app/src/main/java/com/yplayer/ui/screens/songs/SongsScreen.kt`
- Modify: `app/src/main/java/com/yplayer/ui/screens/search/SearchScreen.kt`
- Modify: `app/src/main/java/com/yplayer/ui/screens/albumdetail/AlbumDetailScreen.kt`
- Modify: `app/src/main/java/com/yplayer/ui/screens/artistdetail/ArtistDetailScreen.kt`
- Modify: `app/src/main/java/com/yplayer/ui/screens/playlistdetail/PlaylistDetailScreen.kt`

- [ ] **Step 1: Simplify `PlayableSongList` in `SongList.kt`**

Replace the `PlayableSongList` function with:

```kotlin
@Composable
fun PlayableSongList(
    songs: List<Song>,
    onPlay: (Int) -> Unit,
    emptyMessage: String = "No songs",
) {
    SongList(
        songs = songs,
        emptyMessage = emptyMessage,
        onSongClick = onPlay,
    )
}
```

- [ ] **Step 2: Update `SongsScreen.kt`**

Remove the `onPlaybackStarted: () -> Unit = {}` parameter and the `onPlaybackStarted = onPlaybackStarted` argument:

```kotlin
@Composable
fun SongsScreen(
    viewModel: SongsViewModel = viewModel(...),
) {
    ...
    PlayableSongList(
        songs = songs,
        onPlay = viewModel::playSong,
    )
}
```

- [ ] **Step 3: Update `SearchScreen.kt`**

Remove the `onPlaybackStarted` parameter from the signature and from `PlayableSongList`:

```kotlin
fun SearchScreen(
    onBack: () -> Unit,
    viewModel: SearchViewModel = viewModel(...),
) {
    ...
    PlayableSongList(
        songs = results,
        onPlay = viewModel::playSong,
    )
}
```

- [ ] **Step 4: Update `AlbumDetailScreen.kt`**

Remove the `onPlaybackStarted` parameter; replace the play-album button body:

```kotlin
FilledIconButton(
    onClick = { viewModel.playSong(0) },
    ...
)
```

and `PlayableSongList(songs = songs, onPlay = viewModel::playSong)`.

- [ ] **Step 5: Update `ArtistDetailScreen.kt`**

Remove the `onPlaybackStarted` parameter; call `PlayableSongList(songs = songs, onPlay = viewModel::playSong)`.

- [ ] **Step 6: Update `PlaylistDetailScreen.kt`**

Remove the `onPlaybackStarted` parameter; in the `LazyColumn` item:

```kotlin
onPlay = { viewModel.playSong(songs.indexOf(song)) },
```

(remove the `onPlaybackStarted()` call).

- [ ] **Step 7: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/yplayer/ui/components/SongList.kt app/src/main/java/com/yplayer/ui/screens
git commit -m "drop now-playing navigation callbacks from screens"
```

---

### Task 10: Navigation transitions, list entrance, press feedback

**Files:**
- Modify: `app/src/main/java/com/yplayer/ui/MainScreen.kt`
- Modify: `app/src/main/java/com/yplayer/ui/components/SongList.kt`
- Modify: `app/src/main/java/com/yplayer/ui/screens/albums/AlbumsScreen.kt`

- [ ] **Step 1: Add spring nav transitions in `MainScreen.kt`**

In the `NavHost` call, add transition parameters:

```kotlin
                NavHost(
                    navController = navController,
                    startDestination = "songs",
                    modifier = Modifier.padding(padding),
                    enterTransition = {
                        fadeIn(animationSpec = MotionSpecs.Settle) +
                            slideInVertically(animationSpec = MotionSpecs.Settle, initialOffsetY = { it / 12 })
                    },
                    exitTransition = {
                        fadeOut(animationSpec = MotionSpecs.Settle) +
                            slideOutVertically(animationSpec = MotionSpecs.Settle, targetOffsetY = { -it / 12 })
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = MotionSpecs.Settle) +
                            slideInVertically(animationSpec = MotionSpecs.Settle, initialOffsetY = { -it / 12 })
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = MotionSpecs.Settle) +
                            slideOutVertically(animationSpec = MotionSpecs.Settle, targetOffsetY = { it / 12 })
                    },
                )
```

Add imports: `androidx.compose.animation.fadeIn`, `androidx.compose.animation.fadeOut`, `androidx.compose.animation.slideInVertically`, `androidx.compose.animation.slideOutVertically`, `com.yplayer.ui.motion.MotionSpecs`. `fadeIn(animationSpec = ...)` expects an `AnimationSpec<Float>` — `MotionSpecs.Settle` is `SpringSpec<Float>` ✓.

- [ ] **Step 2: Add `listEntrance` + `pressScale` modifiers**

Create a small file `app/src/main/java/com/yplayer/ui/motion/EntranceMotion.kt`:

```kotlin
package com.yplayer.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

fun Modifier.listEntrance(reducedMotion: Boolean): Modifier = composed {
    val progress = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reducedMotion) progress.animateTo(1f, spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium))
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 8f
    }
}

fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
```

- [ ] **Step 3: Apply to `SongRow` in `SongList.kt`**

Update `SongRow`:

```kotlin
@Composable
fun SongRow(song: Song, onClick: () -> Unit) {
    val reducedMotion = shouldReduceMotion()
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("songRow")
            .listEntrance(reducedMotion)
            .pressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ...
    }
}
```

Add imports: `androidx.compose.foundation.LocalIndication`, `androidx.compose.foundation.interaction.MutableInteractionSource`, `androidx.compose.runtime.remember`, `com.yplayer.ui.motion.listEntrance`, `com.yplayer.ui.motion.pressScale`, `com.yplayer.ui.motion.shouldReduceMotion`.

- [ ] **Step 4: Apply to `AlbumCard` in `AlbumsScreen.kt`**

Update the card:

```kotlin
@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .pressScale(interactionSource),
    ) {
        ...
    }
}
```

Add imports for `MutableInteractionSource`, `LocalIndication`, `remember`, `pressScale`.

- [ ] **Step 5: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/yplayer/ui/motion/EntranceMotion.kt app/src/main/java/com/yplayer/ui/MainScreen.kt app/src/main/java/com/yplayer/ui/components/SongList.kt app/src/main/java/com/yplayer/ui/screens/albums/AlbumsScreen.kt
git commit -m "add spring navigation, list entrance, and press feedback"
```

---

### Task 11: Typography and shape tokens

**Files:**
- Modify: `app/src/main/java/com/yplayer/ui/theme/Type.kt`
- Modify: `app/src/main/java/com/yplayer/ui/theme/Theme.kt`

- [ ] **Step 1: Write the type ramp**

Replace `app/src/main/java/com/yplayer/ui/theme/Type.kt`:

```kotlin
package com.yplayer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    displayLarge = TextStyle(fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.5).sp, fontWeight = FontWeight.Bold),
    displayMedium = TextStyle(fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = (-0.5).sp, fontWeight = FontWeight.Bold),
    displaySmall = TextStyle(fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.25).sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.25).sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.25).sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.15).sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.1).sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Medium),
)
```

- [ ] **Step 2: Add the shape system to the theme**

Update `app/src/main/java/com/yplayer/ui/theme/Theme.kt`:

```kotlin
package com.yplayer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme()
private val LightColorScheme = lightColorScheme()

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun YPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/yplayer/ui/theme/Type.kt app/src/main/java/com/yplayer/ui/theme/Theme.kt
git commit -m "add type ramp and shape system"
```

---

### Task 12: Update the playback UI test

**Files:**
- Modify: `app/src/androidTest/java/com/yplayer/ui/SongPlaybackTest.kt`

- [ ] **Step 1: Rewrite the test for the sheet flow**

```kotlin
package com.yplayer.ui

import android.Manifest
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.yplayer.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SongPlaybackTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.POST_NOTIFICATIONS,
    )

    @Test
    fun tappingSongShowsMiniPlayerAndExpandsSheet() {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasTestTag("songRow")).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onAllNodes(hasTestTag("songRow")).get(0).performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithContentDescription("Pause").assertExists()
            }.isSuccess
        }

        composeRule.onNodeWithTag("miniPlayer").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithContentDescription("Shuffle").assertExists()
            }.isSuccess
        }
    }
}
```

Note: after expanding, the transport row's `Shuffle` button (contentDescription "Shuffle") is visible, which confirms the expanded state.

- [ ] **Step 2: Run the instrumented test**

Requires a connected device/emulator:

```bash
./gradlew :app:connectedDebugAndroidTest --tests "com.yplayer.ui.SongPlaybackTest"
```

Expected: PASS. If no device is available, mark this task verified by `./gradlew :app:compileDebugAndroidTestKotlin` succeeding.

- [ ] **Step 3: Commit**

```bash
git add app/src/androidTest/java/com/yplayer/ui/SongPlaybackTest.kt
git commit -m "update playback test for mini player and sheet expansion"
```

---

### Task 13: Final verification

**Files:**
- None (verification only)

- [x] **Step 1: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: all tests PASS (motion math, sheet interrupt, playlist/library/ViewModel tests).
Result: PASS — 35 tests across 13 classes (after removing stale `NowPlayingViewModelTest.kt` that referenced the deleted ViewModel).

- [x] **Step 2: Run lint**

Run: `./gradlew :app:lintDebug`
Expected: no errors introduced by this change (address any that surface).
Result: PASS — fixed 2 pre-existing `UnsafeOptInUsageError`s in `MediaItemMapper.kt` by adding `@OptIn(UnstableApi::class)`.

- [ ] **Step 3: Manual smoke check (device)**

On a device/emulator:
1. Open app → Songs tab → tap a song. Mini player appears with the rotating disc artwork.
2. Drag the mini player up → sheet expands smoothly, artwork backdrop blurs in.
3. Drag the sheet down → rubber-bands at the bottom, release → springs back or collapses.
4. Flick up hard → sheet expands with a slight bounce; flick down → collapses.
5. Tap the disc's play button → rotation freezes while paused, resumes when playing.
6. Pull-to-refresh and scrolling lists stay smooth (artwork cached, no per-row decode).
7. Enable "Remove animations" in Developer Options → re-run; sheet snaps instead of springing, no rotation.

(Not run — no device/emulator connected. Verified via `:app:compileDebugAndroidTestKotlin` instead.)

- [x] **Step 4: Final commit of any stragglers**

```bash
git add -A
git commit -m "polish premium motion and design pass"
```

(Only if there are leftover uncommitted changes; otherwise skip.)
