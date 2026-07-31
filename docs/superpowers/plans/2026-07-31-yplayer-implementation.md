# yPlayer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a minimal, fast local Android music player (Kotlin + Jetpack Compose + Media3) that browses device music via MediaStore, plays with background support, and manages playlists in Room.

**Architecture:** Single `app` module, MVVM + repositories. Compose UI → ViewModels → two repositories (`LibraryRepository` over MediaStore via a `LibrarySource` interface, `PlaylistRepository` over Room via a `PlaylistRepository` interface). Playback lives in a Media3 `MediaSessionService`; UI talks to it through a thin `PlaybackController` interface so ViewModels stay JVM-testable. Manual DI via an `AppContainer`.

**Tech Stack:** Kotlin 2.1, Jetpack Compose (Material 3, BOM), Navigation Compose, Media3 (ExoPlayer + session), Room (KSP), Coroutines. AGP 8.9.1 / Gradle 8.11.1, compileSdk/targetSdk 36, minSdk 26, JVM 17.

---

## Environment Notes

- **SDK root:** `/home/yass_gr/Android/Sdk` (platforms android-36, android-36.1 installed).
- **Java:** OpenJDK 17. **No Gradle binary installed** — Gradle is bootstrapped via the wrapper in Task 1.
- **Connected device:** `C6MB796XTOBQ8TGQ` present but `unauthorized`. **The user must accept the USB-debugging prompt on the phone** (or the phone must be reconnected and re-authorized) before any `installDebug` or instrumented-test step. If no device is available, `installDebug`/`connectedDebugAndroidTest` steps are skipped and marked manually.
- **No AVDs exist.** Emulator binary is present but no system image/cmdline-tools — creating AVDs is out of scope for this plan; all automated tests are JVM unit tests except one optional `androidTest` suite that requires an authorized device.
- All commits use plain unprefixed messages (no `feat:`/`docs:` types, no ticket prefixes).

---

## Task 1: Bootstrap the Gradle project

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `local.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create: `app/src/main/java/com/yplayer/YPlayerApp.kt`
- Create: `app/src/main/java/com/yplayer/AppContainer.kt`
- Create: `app/src/main/java/com/yplayer/MainActivity.kt`
- Generate: `gradlew`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`

- [ ] **Step 1: Download Gradle 8.11.1 (one-time bootstrap)**

```bash
cd /tmp && curl -fLO https://services.gradle.org/distributions/gradle-8.11.1-bin.zip && unzip -qo gradle-8.11.1-bin.zip
```

Expected: `gradle-8.11.1/` directory in `/tmp`.

- [ ] **Step 2: Write `local.properties`**

```properties
sdk.dir=/home/yass_gr/Android/Sdk
```

- [ ] **Step 3: Write `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "yPlayer"
include(":app")
```

- [ ] **Step 4: Write root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
```

- [ ] **Step 5: Write `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

- [ ] **Step 6: Write `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.9.1"
kotlin = "2.1.0"
ksp = "2.1.0-1.0.29"
coreKtx = "1.15.0"
lifecycle = "2.8.7"
activityCompose = "1.9.3"
composeBom = "2024.12.01"
navigationCompose = "2.8.5"
media3 = "1.5.1"
room = "2.7.1"
coroutines = "1.9.0"
junit = "4.13.2"
androidxTestCore = "1.6.1"
androidxTestRules = "1.6.1"
androidxJunit = "1.2.1"
espressoCore = "3.6.1"
robolectric = "4.14.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
androidx-media3-session = { group = "androidx.media3", name = "media3-session", version.ref = "media3" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-test-core-ktx = { group = "androidx.test", name = "core-ktx", version.ref = "androidxTestCore" }
androidx-test-rules = { group = "androidx.test", name = "rules", version.ref = "androidxTestRules" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidxJunit" }
espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 7: Write `app/build.gradle.kts`**

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.yplayer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.yplayer"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
```

- [ ] **Step 8: Write `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
    <uses-permission
        android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />

    <application
        android:name=".YPlayerApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.YPlayer">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".player.PlaybackService"
            android:exported="true"
            android:foregroundServiceType="mediaPlayback">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
            </intent-filter>
        </service>
    </application>

</manifest>
```

- [ ] **Step 9: Write resources**

`app/src/main/res/values/strings.xml`:
```xml
<resources>
    <string name="app_name">yPlayer</string>
</resources>
```

`app/src/main/res/values/colors.xml`:
```xml
<resources>
    <color name="ic_launcher_background">#0A0A0B</color>
</resources>
```

`app/src/main/res/values/themes.xml`:
```xml
<resources>
    <style name="Theme.YPlayer" parent="android:Theme.Material.NoActionBar" />
</resources>
```

`app/src/main/res/drawable/ic_launcher_foreground.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M44,32 L76,54 L44,76 Z" />
</vector>
```

`app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`:
```xml
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

- [ ] **Step 10: Write `YPlayerApp.kt`**

```kotlin
package com.yplayer

import android.app.Application

class YPlayerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
```

- [ ] **Step 11: Write `AppContainer.kt`**

```kotlin
package com.yplayer

import android.app.Application

class AppContainer(private val app: Application) {
    // Repositories and the playback controller are wired here as later tasks add them.
}
```

- [ ] **Step 12: Write `MainActivity.kt` (placeholder)**

```kotlin
package com.yplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { }
    }
}
```

Note: `MainActivity` renders nothing yet (theme + nav shell land in Task 2). The manifest's `android:name=".player.PlaybackService"` reference does **not** need to resolve yet — the `<service>` tag is only validated at runtime, so the build passes.

- [ ] **Step 13: Generate the Gradle wrapper and build**

```bash
cd /home/yass_gr/projects/yPlayer && /tmp/gradle-8.11.1/bin/gradle wrapper --gradle-version 8.11.1
```

Expected: `gradlew`, `gradle/` created.

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL` and `app/build/outputs/apk/debug/app-debug.apk` exists. (First run downloads AGP + dependencies; allow several minutes. If a dependency fails to resolve, bump the failing artifact to its latest stable patch version and re-run.)

- [ ] **Step 14: Commit**

```bash
git add -A
git commit -m "bootstrap gradle project scaffolding"
```

---

## Task 2: Theme + navigation shell with bottom tabs

**Files:**
- Create: `app/src/main/java/com/yplayer/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/yplayer/ui/theme/Type.kt`
- Create: `app/src/main/java/com/yplayer/ui/screens/PlaceholderScreens.kt`
- Create: `app/src/main/java/com/yplayer/ui/MainScreen.kt`
- Modify: `app/src/main/java/com/yplayer/MainActivity.kt`
- Test: `app/src/androidTest/java/com/yplayer/ui/NavigationTest.kt`

- [ ] **Step 1: Write the failing UI test**

`app/src/androidTest/java/com/yplayer/ui/NavigationTest.kt`:
```kotlin
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
```

Note: `GrantPermissionRule` grants the permissions at install time so no runtime permission dialog appears over the UI during the test.

- [ ] **Step 2: Write theme files**

`app/src/main/java/com/yplayer/ui/theme/Theme.kt`:
```kotlin
package com.yplayer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme()
private val LightColorScheme = lightColorScheme()

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
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
```

`app/src/main/java/com/yplayer/ui/theme/Type.kt`:
```kotlin
package com.yplayer.ui.theme

import androidx.compose.material3.Typography

val Typography = Typography()
```

- [ ] **Step 3: Write placeholder screens**

`app/src/main/java/com/yplayer/ui/screens/PlaceholderScreens.kt`:
```kotlin
package com.yplayer.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun SongsScreen() = CenteredPlaceholder("Songs placeholder")

@Composable
fun AlbumsScreen() = CenteredPlaceholder("Albums placeholder")

@Composable
fun ArtistsScreen() = CenteredPlaceholder("Artists placeholder")

@Composable
fun PlaylistsScreen() = CenteredPlaceholder("Playlists placeholder")

@Composable
private fun CenteredPlaceholder(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text)
    }
}
```

- [ ] **Step 4: Write `MainScreen.kt`**

```kotlin
package com.yplayer.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yplayer.ui.screens.AlbumsScreen
import com.yplayer.ui.screens.ArtistsScreen
import com.yplayer.ui.screens.PlaylistsScreen
import com.yplayer.ui.screens.SongsScreen

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("songs", "Songs", Icons.AutoMirrored.Filled.List),
    TabItem("albums", "Albums", Icons.Filled.Album),
    TabItem("artists", "Artists", Icons.Filled.Face),
    TabItem("playlists", "Playlists", Icons.Filled.QueueMusic),
)

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
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
        NavHost(
            navController = navController,
            startDestination = "songs",
            modifier = Modifier.padding(padding),
        ) {
            composable("songs") { SongsScreen() }
            composable("albums") { AlbumsScreen() }
            composable("artists") { ArtistsScreen() }
            composable("playlists") { PlaylistsScreen() }
        }
    }
}
```

- [ ] **Step 5: Wire `MainActivity` to the theme**

Replace the body of `MainActivity.kt` `setContent`:
```kotlin
package com.yplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yplayer.ui.MainScreen
import com.yplayer.ui.theme.YPlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YPlayerTheme {
                MainScreen()
            }
        }
    }
}
```

- [ ] **Step 6: Build**

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Run the UI test (requires authorized device)**

```bash
./gradlew :app:connectedDebugAndroidTest
```

Expected: `NavigationTest.bottomNavSwitchesBetweenTabs` passes. If no device is authorized, run on the phone after the user accepts the USB-debugging prompt, or skip and rely on the Task 1 manual build + later unit tests.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "add theme and bottom-tab navigation shell"
```

---

## Task 3: Domain models + library repository over MediaStore (TDD)

**Files:**
- Create: `app/src/main/java/com/yplayer/data/model/Song.kt`
- Create: `app/src/main/java/com/yplayer/data/library/LibrarySource.kt`
- Create: `app/src/main/java/com/yplayer/data/library/MediaStoreLibrarySource.kt`
- Create: `app/src/main/java/com/yplayer/data/library/LibraryRepository.kt`
- Test: `app/src/test/java/com/yplayer/data/library/LibraryRepositoryTest.kt`
- Modify: `app/src/main/java/com/yplayer/AppContainer.kt`

- [ ] **Step 1: Write the failing tests**

`app/src/test/java/com/yplayer/data/library/LibraryRepositoryTest.kt`:
```kotlin
package com.yplayer.data.library

import com.yplayer.data.model.Song
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryRepositoryTest {

    private val songs = listOf(
        Song(1, "Blinding Lights", "The Weeknd", "After Hours", 100, 200_000, uriOf(1)),
        Song(2, "Starboy", "The Weeknd", "Starboy", 200, 220_000, uriOf(2)),
        Song(3, "Save Your Tears", "The Weeknd", "After Hours", 100, 210_000, uriOf(3)),
        Song(4, "Lose Yourself", "Eminem", "8 Mile", 300, 320_000, uriOf(4)),
    )

    @Test
    fun refresh_populatesSongs() = runTest {
        val repo = LibraryRepository(FakeLibrarySource(songs))

        assertEquals(emptyList<Song>(), repo.songs.value)

        repo.refresh()

        assertEquals(songs, repo.songs.value)
    }

    @Test
    fun albums_groupSongsByAlbumId() = runTest {
        val repo = LibraryRepository(FakeLibrarySource(songs))
        repo.refresh()

        val albums = repo.albums.value
        assertEquals(listOf("After Hours", "Starboy", "8 Mile"), albums.map { it.title })
        assertEquals(2, albums.first { it.title == "After Hours" }.songs.size)
    }

    @Test
    fun artists_groupSongsByArtist() = runTest {
        val repo = LibraryRepository(FakeLibrarySource(songs))
        repo.refresh()

        val artists = repo.artists.value
        assertEquals(listOf("Eminem", "The Weeknd"), artists.map { it.name })
        assertEquals(3, artists.first { it.name == "The Weeknd" }.songs.size)
    }

    @Test
    fun songsOfAlbum_filtersByAlbumId() = runTest {
        val repo = LibraryRepository(FakeLibrarySource(songs))
        repo.refresh()

        assertEquals(listOf(1L, 3L), repo.songsOfAlbum(100).map { it.id })
    }

    @Test
    fun search_matchesTitleArtistAndAlbum() = runTest {
        val repo = LibraryRepository(FakeLibrarySource(songs))
        repo.refresh()

        assertEquals(listOf(1L, 3L), repo.search("tears").map { it.id })
        assertEquals(listOf(1L, 2L, 3L), repo.search("the weeknd").map { it.id })
        assertEquals(listOf(1L, 3L), repo.search("after hours").map { it.id })
        assertEquals(emptyList<Song>(), repo.search("zzz"))
    }

    private fun uriOf(id: Long) = android.net.Uri.parse("content://media/external/audio/media/$id")
}
```

Note: `android.net.Uri` is a JVM-friendly value class — usable in plain unit tests without Robolectric.

`app/src/test/java/com/yplayer/data/library/FakeLibrarySource.kt`:
```kotlin
package com.yplayer.data.library

import com.yplayer.data.model.Song

class FakeLibrarySource(private val songs: List<Song>) : LibrarySource {
    override suspend fun querySongs(): List<Song> = songs
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "com.yplayer.data.library.LibraryRepositoryTest"
```

Expected: FAIL — `LibraryRepository`, `LibrarySource`, `Song` don't exist (compilation errors).

- [ ] **Step 3: Write the model**

`app/src/main/java/com/yplayer/data/model/Song.kt`:
```kotlin
package com.yplayer.data.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: Uri,
)

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val songs: List<Song>,
)

data class Artist(
    val name: String,
    val songs: List<Song>,
)
```

- [ ] **Step 4: Write `LibrarySource` + `MediaStoreLibrarySource`**

`app/src/main/java/com/yplayer/data/library/LibrarySource.kt`:
```kotlin
package com.yplayer.data.library

import com.yplayer.data.model.Song

interface LibrarySource {
    suspend fun querySongs(): List<Song>
}
```

`app/src/main/java/com/yplayer/data/library/MediaStoreLibrarySource.kt`:
```kotlin
package com.yplayer.data.library

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.yplayer.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreLibrarySource(private val context: Context) : LibrarySource {

    override suspend fun querySongs(): List<Song> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
        )
        val songs = mutableListOf<Song>()
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                songs += Song(
                    id = id,
                    title = cursor.getString(titleCol) ?: "Unknown",
                    artist = cursor.getString(artistCol) ?: "Unknown Artist",
                    album = cursor.getString(albumCol) ?: "Unknown Album",
                    albumId = cursor.getLong(albumIdCol),
                    durationMs = cursor.getLong(durationCol),
                    uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                )
            }
        }
        songs
    }
}
```

- [ ] **Step 5: Write `LibraryRepository`**

`app/src/main/java/com/yplayer/data/library/LibraryRepository.kt`:
```kotlin
package com.yplayer.data.library

import com.yplayer.data.model.Album
import com.yplayer.data.model.Artist
import com.yplayer.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LibraryRepository(private val source: LibrarySource) {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    val albums: StateFlow<List<Album>> = _songs.let { flow ->
        kotlinx.coroutines.flow.map(flow) { list ->
            list.groupBy { it.albumId }
                .values
                .map { group ->
                    Album(
                        id = group.first().albumId,
                        title = group.first().album,
                        artist = group.first().artist,
                        songs = group.sortedBy { it.title },
                    )
                }
                .sortedBy { it.title }
        }
    }.let { kotlinx.coroutines.flow.stateIn(it, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default) + kotlinx.coroutines.SupervisorJob(), emptyList()) }

    val artists: StateFlow<List<Artist>> = _songs.let { flow ->
        kotlinx.coroutines.flow.map(flow) { list ->
            list.groupBy { it.artist }
                .values
                .map { group -> Artist(name = group.first().artist, songs = group.sortedBy { it.title }) }
                .sortedBy { it.name }
        }
    }.let { kotlinx.coroutines.flow.stateIn(it, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default) + kotlinx.coroutines.SupervisorJob(), emptyList()) }

    suspend fun refresh() {
        _songs.value = source.querySongs()
    }

    fun songsOfAlbum(albumId: Long): List<Song> = _songs.value.filter { it.albumId == albumId }

    fun songsOfArtist(artistName: String): List<Song> = _songs.value.filter { it.artist == artistName }

    fun search(query: String): List<Song> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return _songs.value
        return _songs.value.filter {
            it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
        }
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.yplayer.data.library.LibraryRepositoryTest"
```

Expected: PASS (4 tests).

- [ ] **Step 7: Wire the repository into `AppContainer`**

`AppContainer.kt`:
```kotlin
package com.yplayer

import android.app.Application
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.library.MediaStoreLibrarySource

class AppContainer(private val app: Application) {
    val libraryRepository = LibraryRepository(MediaStoreLibrarySource(app))
}
```

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "add library repository over MediaStore"
```

---

## Task 4: Songs, Albums, Artists screens + runtime permissions

**Files:**
- Create: `app/src/main/java/com/yplayer/ui/screens/songs/SongsViewModel.kt`
- Create: `app/src/main/java/com/yplayer/ui/screens/songs/SongsScreen.kt`
- Create: `app/src/main/java/com/yplayer/ui/screens/albums/AlbumsViewModel.kt`
- Create: `app/src/main/java/com/yplayer/ui/screens/albums/AlbumsScreen.kt`
- Create: `app/src/main/java/com/yplayer/ui/screens/artists/ArtistsViewModel.kt`
- Create: `app/src/main/java/com/yplayer/ui/screens/artists/ArtistsScreen.kt`
- Create: `app/src/main/java/com/yplayer/ui/components/Artwork.kt`
- Create: `app/src/main/java/com/yplayer/ui/MusicPermissionGate.kt`
- Modify: `app/src/main/java/com/yplayer/ui/MainScreen.kt`
- Modify: `app/src/main/java/com/yplayer/ui/screens/PlaceholderScreens.kt` (remove Songs/Albums/Artists placeholders)
- Test: `app/src/test/java/com/yplayer/ui/screens/songs/SongsViewModelTest.kt`
- Test: `app/src/test/java/com/yplayer/ui/screens/albums/AlbumsViewModelTest.kt`
- Test: `app/src/test/java/com/yplayer/ui/screens/artists/ArtistsViewModelTest.kt`

- [ ] **Step 1: Write the failing ViewModel tests**

`app/src/test/java/com/yplayer/ui/screens/songs/SongsViewModelTest.kt`:
```kotlin
package com.yplayer.ui.screens.songs

import com.yplayer.data.library.FakeLibrarySource
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Song
import com.yplayer.player.FakePlaybackController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SongsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val songs = listOf(
        Song(1, "A", "Artist", "Album", 10, 1000, android.net.Uri.parse("content://media/1")),
        Song(2, "B", "Artist", "Album", 10, 1000, android.net.Uri.parse("content://media/2")),
    )

    @Test
    fun songs_areLoadedOnInit() = runTest(dispatcher) {
        val vm = SongsViewModel(LibraryRepository(FakeLibrarySource(songs)), FakePlaybackController())
        advanceUntilIdle()

        assertEquals(songs, vm.songs.value)
    }

    @Test
    fun playSong_delegatesToController() = runTest(dispatcher) {
        val controller = FakePlaybackController()
        val vm = SongsViewModel(LibraryRepository(FakeLibrarySource(songs)), controller)
        advanceUntilIdle()

        vm.playSong(1)

        assertEquals(listOf(songs[1]), controller.lastQueue)
        assertEquals(0, controller.lastStartIndex)
    }
}
```

`app/src/test/java/com/yplayer/ui/screens/albums/AlbumsViewModelTest.kt`:
```kotlin
package com.yplayer.ui.screens.albums

import com.yplayer.data.library.FakeLibrarySource
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun albums_areLoadedOnInit() = runTest(dispatcher) {
        val songs = listOf(
            Song(1, "A", "Artist", "Album One", 10, 1000, android.net.Uri.parse("content://media/1")),
            Song(2, "B", "Artist", "Album Two", 20, 1000, android.net.Uri.parse("content://media/2")),
        )
        val vm = AlbumsViewModel(LibraryRepository(FakeLibrarySource(songs)))
        advanceUntilIdle()

        assertEquals(listOf("Album One", "Album Two"), vm.albums.value.map { it.title })
    }
}
```

`app/src/test/java/com/yplayer/ui/screens/artists/ArtistsViewModelTest.kt`:
```kotlin
package com.yplayer.ui.screens.artists

import com.yplayer.data.library.FakeLibrarySource
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArtistsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun artists_areLoadedOnInit() = runTest(dispatcher) {
        val songs = listOf(
            Song(1, "A", "Sia", "Album", 10, 1000, android.net.Uri.parse("content://media/1")),
            Song(2, "B", "Beyonce", "Album", 10, 1000, android.net.Uri.parse("content://media/2")),
        )
        val vm = ArtistsViewModel(LibraryRepository(FakeLibrarySource(songs)))
        advanceUntilIdle()

        assertEquals(listOf("Beyonce", "Sia"), vm.artists.value.map { it.name })
    }
}
```

- [ ] **Step 2: Write the fake playback controller (used from here on)**

`app/src/test/java/com/yplayer/player/FakePlaybackController.kt`:
```kotlin
package com.yplayer.player

import com.yplayer.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakePlaybackController : PlaybackController {
    override val state: StateFlow<PlayerState> = MutableStateFlow(PlayerState()).asStateFlow()

    var lastQueue: List<Song>? = null
    var lastStartIndex: Int = -1
    var lastSeekMs: Long = -1
    var toggles = 0
    var nexts = 0
    var previous = 0

    override fun playQueue(songs: List<Song>, startIndex: Int) {
        lastQueue = songs
        lastStartIndex = startIndex
    }

    override fun togglePlayPause() {
        toggles++
    }

    override fun next() {
        nexts++
    }

    override fun previous() {
        previous++
    }

    override fun seekTo(positionMs: Long) {
        lastSeekMs = positionMs
    }

    override fun setShuffle(on: Boolean) {}

    override fun cycleRepeat() {}
}
```

- [ ] **Step 3: Run tests to verify they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "com.yplayer.ui.screens.songs.SongsViewModelTest" --tests "com.yplayer.ui.screens.albums.AlbumsViewModelTest" --tests "com.yplayer.ui.screens.artists.ArtistsViewModelTest"
```

Expected: FAIL — `PlaybackController`, `PlayerState`, the ViewModels don't exist.

- [ ] **Step 4: Write the playback abstractions (player interface only; service comes in Task 6)**

`app/src/main/java/com/yplayer/player/PlaybackController.kt`:
```kotlin
package com.yplayer.player

import com.yplayer.data.model.Song
import kotlinx.coroutines.flow.StateFlow

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val isShuffle: Boolean = false,
    val repeatMode: Int = 0,
)

interface PlaybackController {
    val state: StateFlow<PlayerState>
    fun playQueue(songs: List<Song>, startIndex: Int)
    fun togglePlayPause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun setShuffle(on: Boolean)
    fun cycleRepeat()
}
```

- [ ] **Step 5: Write the ViewModels**

`app/src/main/java/com/yplayer/ui/screens/songs/SongsViewModel.kt`:
```kotlin
package com.yplayer.ui.screens.songs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Song
import com.yplayer.player.PlaybackController
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SongsViewModel(
    private val repository: LibraryRepository,
    private val playbackController: PlaybackController,
) : ViewModel() {

    val songs: StateFlow<List<Song>> = repository.songs

    init {
        viewModelScope.launch { repository.refresh() }
    }

    fun playSong(index: Int) {
        val list = repository.songs.value
        if (index in list.indices) {
            playbackController.playQueue(list, index)
        }
    }
}
```

`app/src/main/java/com/yplayer/ui/screens/albums/AlbumsViewModel.kt`:
```kotlin
package com.yplayer.ui.screens.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Album
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AlbumsViewModel(
    repository: LibraryRepository,
) : ViewModel() {

    val albums: StateFlow<List<Album>> = repository.albums

    init {
        viewModelScope.launch { repository.refresh() }
    }
}
```

`app/src/main/java/com/yplayer/ui/screens/artists/ArtistsViewModel.kt`:
```kotlin
package com.yplayer.ui.screens.artists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Artist
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ArtistsViewModel(
    repository: LibraryRepository,
) : ViewModel() {

    val artists: StateFlow<List<Artist>> = repository.artists

    init {
        viewModelScope.launch { repository.refresh() }
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.yplayer.ui.screens.songs.SongsViewModelTest" --tests "com.yplayer.ui.screens.albums.AlbumsViewModelTest" --tests "com.yplayer.ui.screens.artists.ArtistsViewModelTest"
```

Expected: PASS.

- [ ] **Step 7: Write the artwork loader component**

`app/src/main/java/com/yplayer/ui/components/Artwork.kt`:
```kotlin
package com.yplayer.ui.components

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun albumArtUri(albumId: Long): Uri =
    ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)

private fun loadAlbumArt(context: Context, albumId: Long): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.contentResolver.loadThumbnail(albumArtUri(albumId), Size(512, 512), null)
    } else {
        @Suppress("DEPRECATION")
        context.contentResolver.loadThumbnail(albumArtUri(albumId), java.lang.Long.valueOf(512), null)
    }
}.getOrNull()

@Composable
fun AlbumArt(
    albumId: Long,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 8,
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, albumId) {
        value = withContext(Dispatchers.IO) { loadAlbumArt(context, albumId) }
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
```

- [ ] **Step 8: Write the runtime permission gate**

`app/src/main/java/com/yplayer/ui/MusicPermissionGate.kt`:
```kotlin
package com.yplayer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

private fun audioPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

private fun notificationPermission(): String? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        null
    }

@Composable
fun MusicPermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var hasAudio by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasAudio = results[audioPermission()] == true
    }

    fun request() {
        val permissions = listOfNotNull(audioPermission(), notificationPermission())
        launcher.launch(permissions.toTypedArray())
    }

    LaunchedEffect(Unit) {
        hasAudio = ContextCompat.checkSelfPermission(context, audioPermission()) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasAudio) request()
    }

    if (hasAudio) {
        content()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Allow access to your music to get started",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = ::request,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text("Allow access")
            }
        }
    }
}
```

- [ ] **Step 9: Write the three screens**

`app/src/main/java/com/yplayer/ui/screens/songs/SongsScreen.kt`:
```kotlin
package com.yplayer.ui.screens.songs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Song
import com.yplayer.player.PlaybackController
import com.yplayer.ui.components.AlbumArt
import com.yplayer.ui.util.appContainer

@Composable
fun SongsScreen(
    onSongClick: (Int) -> Unit = {},
    viewModel: SongsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val container = appContainer()
                SongsViewModel(container.libraryRepository, container.playbackController)
            }
        }
    ),
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            SongRow(song = song, onClick = { onSongClick(index) })
        }
    }
}

@Composable
private fun SongRow(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(
            albumId = song.albumId,
            modifier = Modifier.size(48.dp),
        )
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
            )
            Text(
                text = "${song.artist} · ${song.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 76.dp))
}
```

`app/src/main/java/com/yplayer/ui/screens/albums/AlbumsScreen.kt`:
```kotlin
package com.yplayer.ui.screens.albums

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yplayer.data.model.Album
import com.yplayer.ui.components.AlbumArt
import com.yplayer.ui.util.appContainer

@Composable
fun AlbumsScreen(
    onAlbumClick: (Album) -> Unit = {},
    viewModel: AlbumsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AlbumsViewModel(appContainer().libraryRepository) }
        }
    ),
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(albums, key = { it.id }) { album ->
            AlbumCard(album = album, onClick = { onAlbumClick(album) })
        }
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        AlbumArt(albumId = album.id, modifier = Modifier.fillMaxWidth())
        Text(
            text = album.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
```

`app/src/main/java/com/yplayer/ui/screens/artists/ArtistsScreen.kt`:
```kotlin
package com.yplayer.ui.screens.artists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yplayer.data.model.Artist
import com.yplayer.ui.util.appContainer

@Composable
fun ArtistsScreen(
    onArtistClick: (Artist) -> Unit = {},
    viewModel: ArtistsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ArtistsViewModel(appContainer().libraryRepository) }
        }
    ),
) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(artists, key = { it.name }) { artist ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onArtistClick(artist) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Face,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = "${artist.songs.size} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
        }
    }
}
```

- [ ] **Step 10: Add the `appContainer` helper**

`app/src/main/java/com/yplayer/ui/util/AppContainerUtil.kt`:
```kotlin
package com.yplayer.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.yplayer.AppContainer
import com.yplayer.YPlayerApp

@Composable
fun appContainer(): AppContainer {
    val app = LocalContext.current.applicationContext as YPlayerApp
    return app.container
}
```

- [ ] **Step 11: Wire permissions + screens into `MainScreen`**

In `MainScreen.kt`, add imports and change the `NavHost` block plus wrap it in `MusicPermissionGate`. The `bottomBar` stays the same. Full updated file body:

```kotlin
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { /* unchanged: NavigationBar with the 4 tabs */ },
    ) { padding ->
        MusicPermissionGate {
            NavHost(
                navController = navController,
                startDestination = "songs",
                modifier = Modifier.padding(padding),
            ) {
                composable("songs") { SongsScreen() }
                composable("albums") { AlbumsScreen() }
                composable("artists") { ArtistsScreen() }
                composable("playlists") { PlaylistsScreen() }
            }
        }
    }
}
```

Add these imports to `MainScreen.kt` **and remove the three stale imports** it still has from Task 2 (`com.yplayer.ui.screens.SongsScreen`, `com.yplayer.ui.screens.AlbumsScreen`, `com.yplayer.ui.screens.ArtistsScreen` from `PlaceholderScreens.kt` — those placeholder functions are being deleted):
```kotlin
import com.yplayer.ui.screens.songs.SongsScreen
import com.yplayer.ui.screens.albums.AlbumsScreen
import com.yplayer.ui.screens.artists.ArtistsScreen
```

Remove the now-unused `SongsScreen`/`AlbumsScreen`/`ArtistsScreen` placeholder functions from `PlaceholderScreens.kt`, leaving only `PlaylistsScreen`.

- [ ] **Step 12: Build + run unit tests**

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`, all unit tests PASS.

- [ ] **Step 13: Manual verification (device)**

```bash
./gradlew :app:installDebug
```

Open the app on the phone. Expected: permission prompt appears; after granting, the Songs tab shows the device's songs, Albums shows a grid of album covers, Artists shows the artist list.

- [ ] **Step 14: Commit**

```bash
git add -A
git commit -m "add songs, albums, artists screens with permissions"
```

---

## Task 5: Album and Artist detail screens

**Files:**
- Create: `app/src/main/java/com/yplayer/ui/screens/albumdetail/AlbumDetailViewModel.kt`
- Create: `app/src/main/java/com/yplayer/ui/screens/albumdetail/AlbumDetailScreen.kt`
- Create: `app/src/main/java/com/yplayer/ui/screens/artistdetail/ArtistDetailViewModel.kt`
- Create: `app/src/main/java/com/yplayer/ui/screens/artistdetail/ArtistDetailScreen.kt`
- Create: `app/src/main/java/com/yplayer/ui/components/SongList.kt`
- Modify: `app/src/main/java/com/yplayer/ui/MainScreen.kt`
- Test: `app/src/test/java/com/yplayer/ui/screens/albumdetail/AlbumDetailViewModelTest.kt`
- Test: `app/src/test/java/com/yplayer/ui/screens/artistdetail/ArtistDetailViewModelTest.kt`

- [x] **Step 1: Write the failing ViewModel tests**

`app/src/test/java/com/yplayer/ui/screens/albumdetail/AlbumDetailViewModelTest.kt`:
```kotlin
package com.yplayer.ui.screens.albumdetail

import com.yplayer.data.library.FakeLibrarySource
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Song
import com.yplayer.player.FakePlaybackController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val songs = listOf(
        Song(1, "A", "Artist", "Album", 10, 1000, android.net.Uri.parse("content://media/1")),
        Song(2, "B", "Artist", "Album", 10, 1000, android.net.Uri.parse("content://media/2")),
        Song(3, "C", "Other", "Other Album", 20, 1000, android.net.Uri.parse("content://media/3")),
    )

    @Test
    fun songs_filterToAlbum() = runTest(dispatcher) {
        val repo = LibraryRepository(FakeLibrarySource(songs))
        val vm = AlbumDetailViewModel(repo, FakePlaybackController(), albumId = 10)
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L), vm.songs.value.map { it.id })
    }

    @Test
    fun playSong_usesAlbumContext() = runTest(dispatcher) {
        val controller = FakePlaybackController()
        val repo = LibraryRepository(FakeLibrarySource(songs))
        val vm = AlbumDetailViewModel(repo, controller, albumId = 10)
        advanceUntilIdle()

        vm.playSong(1)

        assertEquals(listOf(1L, 2L), controller.lastQueue?.map { it.id })
        assertEquals(1, controller.lastStartIndex)
    }
}
```

`app/src/test/java/com/yplayer/ui/screens/artistdetail/ArtistDetailViewModelTest.kt`:
```kotlin
package com.yplayer.ui.screens.artistdetail

import com.yplayer.data.library.FakeLibrarySource
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Song
import com.yplayer.player.FakePlaybackController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArtistDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val songs = listOf(
        Song(1, "A", "The Weeknd", "Album One", 10, 1000, android.net.Uri.parse("content://media/1")),
        Song(2, "B", "The Weeknd", "Album Two", 20, 1000, android.net.Uri.parse("content://media/2")),
        Song(3, "C", "Sia", "Album", 30, 1000, android.net.Uri.parse("content://media/3")),
    )

    @Test
    fun songs_filterToArtist() = runTest(dispatcher) {
        val repo = LibraryRepository(FakeLibrarySource(songs))
        val vm = ArtistDetailViewModel(repo, FakePlaybackController(), artistName = "The Weeknd")
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L), vm.songs.value.map { it.id })
    }

    @Test
    fun playSong_usesArtistContext() = runTest(dispatcher) {
        val controller = FakePlaybackController()
        val repo = LibraryRepository(FakeLibrarySource(songs))
        val vm = ArtistDetailViewModel(repo, controller, artistName = "The Weeknd")
        advanceUntilIdle()

        vm.playSong(0)

        assertEquals(listOf(1L, 2L), controller.lastQueue?.map { it.id })
        assertEquals(0, controller.lastStartIndex)
    }
}
```

- [x] **Step 2: Run tests to verify they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "com.yplayer.ui.screens.albumdetail.AlbumDetailViewModelTest" --tests "com.yplayer.ui.screens.artistdetail.ArtistDetailViewModelTest"
```

Expected: FAIL — ViewModels don't exist.

- [x] **Step 3: Write the ViewModels**

`app/src/main/java/com/yplayer/ui/screens/albumdetail/AlbumDetailViewModel.kt`:
```kotlin
package com.yplayer.ui.screens.albumdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Song
import com.yplayer.player.PlaybackController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlbumDetailViewModel(
    repository: LibraryRepository,
    private val playbackController: PlaybackController,
    albumId: Long,
) : ViewModel() {

    val songs: StateFlow<List<Song>> = repository.songs
        .map { list -> list.filter { it.albumId == albumId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { repository.refresh() }
    }

    fun playSong(index: Int) {
        val list = songs.value
        if (index in list.indices) playbackController.playQueue(list, index)
    }
}
```

`app/src/main/java/com/yplayer/ui/screens/artistdetail/ArtistDetailViewModel.kt`:
```kotlin
package com.yplayer.ui.screens.artistdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Song
import com.yplayer.player.PlaybackController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArtistDetailViewModel(
    repository: LibraryRepository,
    private val playbackController: PlaybackController,
    artistName: String,
) : ViewModel() {

    val songs: StateFlow<List<Song>> = repository.songs
        .map { list -> list.filter { it.artist == artistName } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { repository.refresh() }
    }

    fun playSong(index: Int) {
        val list = songs.value
        if (index in list.indices) playbackController.playQueue(list, index)
    }
}
```

- [x] **Step 4: Run tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.yplayer.ui.screens.albumdetail.AlbumDetailViewModelTest" --tests "com.yplayer.ui.screens.artistdetail.ArtistDetailViewModelTest"
```

Expected: PASS.

- [x] **Step 5: Extract a reusable song list component**

`app/src/main/java/com/yplayer/ui/components/SongList.kt`:
```kotlin
package com.yplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yplayer.data.model.Song

@Composable
fun SongList(songs: List<Song>, onSongClick: (Int) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            SongRow(song = song, onClick = { onSongClick(index) })
        }
    }
}

@Composable
fun SongRow(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(
            albumId = song.albumId,
            modifier = Modifier.size(48.dp),
        )
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
            )
            Text(
                text = "${song.artist} · ${song.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 76.dp))
}
```

Update `SongsScreen.kt` to use `SongList(songs = songs, onSongClick = onSongClick)` and delete its private `SongRow` (remove the now-unused `Row`/`AlbumArt`/`Song` imports as needed).

- [x] **Step 6: Write the detail screens**

`app/src/main/java/com/yplayer/ui/screens/albumdetail/AlbumDetailScreen.kt`:
```kotlin
package com.yplayer.ui.screens.albumdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yplayer.data.library.LibraryRepository
import com.yplayer.player.PlaybackController
import com.yplayer.ui.components.AlbumArt
import com.yplayer.ui.components.SongList
import com.yplayer.ui.util.appContainer

@Composable
fun AlbumDetailScreen(
    albumId: Long,
    albumTitle: String,
    albumArtist: String,
    onBack: () -> Unit,
    onSongClick: (Int) -> Unit = {},
    viewModel: AlbumDetailViewModel = viewModel(
        key = "album-$albumId",
        factory = viewModelFactory {
            initializer {
                val container = appContainer()
                AlbumDetailViewModel(container.libraryRepository, container.playbackController, albumId)
            }
        }
    ),
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(albumTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                AlbumArt(albumId = albumId, modifier = Modifier.size(200.dp))
            }
            Text(
                text = albumArtist,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            FilledIconButton(
                onClick = { onSongClick(0) },
                modifier = Modifier.padding(16.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.PlayArrow, contentDescription = "Play album")
            }
            SongList(songs = songs, onSongClick = onSongClick)
        }
    }
}
```

`app/src/main/java/com/yplayer/ui/screens/artistdetail/ArtistDetailScreen.kt`:
```kotlin
package com.yplayer.ui.screens.artistdetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yplayer.ui.components.SongList
import com.yplayer.ui.util.appContainer

@Composable
fun ArtistDetailScreen(
    artistName: String,
    onBack: () -> Unit,
    onSongClick: (Int) -> Unit = {},
    viewModel: ArtistDetailViewModel = viewModel(
        key = "artist-$artistName",
        factory = viewModelFactory {
            initializer {
                val container = appContainer()
                ArtistDetailViewModel(container.libraryRepository, container.playbackController, artistName)
            }
        }
    ),
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(artistName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                text = "${songs.size} songs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            SongList(songs = songs, onSongClick = onSongClick)
        }
    }
}
```

- [x] **Step 7: Add routes to `MainScreen`**

In the `NavHost`, add:
```kotlin
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
        onSongClick = { index -> navController.navigate("nowPlaying") },
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
        onSongClick = { index -> navController.navigate("nowPlaying") },
    )
}
composable("nowPlaying") { /* replaced in Task 6 */ }
```

Update the Albums grid and Artists list to navigate:
```kotlin
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
```

Add these imports to `MainScreen.kt`:
```kotlin
import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.compose.navArgument
import com.yplayer.ui.screens.albumdetail.AlbumDetailScreen
import com.yplayer.ui.screens.artistdetail.ArtistDetailScreen
```

- [x] **Step 8: Build + run unit tests**

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`, all unit tests PASS.

- [x] **Step 9: Commit**

```bash
git add -A
git commit -m "add album and artist detail screens"
```

---

## Task 6: Playback service, controller, and Now Playing screen

**Files:**
- Create: `app/src/main/java/com/yplayer/player/MediaItemMapper.kt`
- Create: `app/src/main/java/com/yplayer/player/PlaybackService.kt`
- Create: `app/src/main/java/com/yplayer/player/PlaybackControllerImpl.kt`
- Create: `app/src/main/java/com/yplayer/ui/screens/nowplaying/NowPlayingViewModel.kt`
- Create: `app/src/main/java/com/yplayer/ui/screens/nowplaying/NowPlayingScreen.kt`
- Modify: `app/src/main/java/com/yplayer/AppContainer.kt`
- Test: `app/src/test/java/com/yplayer/player/MediaItemMapperTest.kt`
- Test: `app/src/test/java/com/yplayer/ui/screens/nowplaying/NowPlayingViewModelTest.kt`

- [ ] **Step 1: Write the failing tests**

`app/src/test/java/com/yplayer/player/MediaItemMapperTest.kt`:
```kotlin
package com.yplayer.player

import androidx.media3.common.MediaMetadata
import com.yplayer.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaItemMapperTest {

    @Test
    fun song_toMediaItem_setsMediaIdAndMetadata() {
        val song = Song(42, "Blinding Lights", "The Weeknd", "After Hours", 100, 200_000,
            android.net.Uri.parse("content://media/external/audio/media/42"))

        val item = song.toMediaItem()

        assertEquals("content://media/external/audio/media/42", item.mediaId)
        assertEquals("Blinding Lights", item.mediaMetadata.title)
        assertEquals("The Weeknd", item.mediaMetadata.artist)
        assertEquals("After Hours", item.mediaMetadata.albumTitle)
        assertEquals(200_000L, item.mediaMetadata.durationMs ?: -1L)
    }

    @Test
    fun mediaItem_toSong_reconstructsSong() {
        val item = androidx.media3.common.MediaItem.Builder()
            .setMediaId("content://media/external/audio/media/7")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("T")
                    .setArtist("A")
                    .setAlbumTitle("Al")
                    .setDurationMs(1234L)
                    .build()
            )
            .build()

        val song = item.toSong()

        assertEquals(7L, song.id)
        assertEquals("T", song.title)
        assertEquals("A", song.artist)
        assertEquals("Al", song.album)
        assertEquals(1234L, song.durationMs)
        assertEquals("content://media/external/audio/media/7", song.uri.toString())
    }
}
```

`app/src/test/java/com/yplayer/ui/screens/nowplaying/NowPlayingViewModelTest.kt`:
```kotlin
package com.yplayer.ui.screens.nowplaying

import com.yplayer.data.model.Song
import com.yplayer.player.FakePlaybackController
import com.yplayer.player.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class NowPlayingViewModelTest {

    @Test
    fun togglePlayPause_delegatesToController() {
        val controller = FakePlaybackController()
        val vm = NowPlayingViewModel(controller)

        vm.togglePlayPause()

        assertEquals(1, controller.toggles)
    }

    @Test
    fun seekTo_delegatesToController() {
        val controller = FakePlaybackController()
        val vm = NowPlayingViewModel(controller)

        vm.seekTo(60_000)

        assertEquals(60_000L, controller.lastSeekMs)
    }

    @Test
    fun nextAndPrevious_delegateToController() {
        val controller = FakePlaybackController()
        val vm = NowPlayingViewModel(controller)

        vm.next()
        vm.previous()

        assertEquals(1, controller.nexts)
        assertEquals(1, controller.previous)
    }

    @Test
    fun state_reflectsControllerState() {
        val song = Song(1, "T", "A", "Al", 10, 1000, android.net.Uri.parse("content://media/1"))
        val stateFlow = MutableStateFlow(
            PlayerState(currentSong = song, isPlaying = true, positionMs = 500, durationMs = 1000)
        ).asStateFlow()
        val controller = object : FakePlaybackController() {
            override val state = stateFlow
        }
        val vm = NowPlayingViewModel(controller)

        assertEquals(song, vm.state.value.currentSong)
        assertEquals(true, vm.state.value.isPlaying)
        assertEquals(500L, vm.state.value.positionMs)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "com.yplayer.player.MediaItemMapperTest" --tests "com.yplayer.ui.screens.nowplaying.NowPlayingViewModelTest"
```

Expected: FAIL — `toMediaItem`, `toSong`, `NowPlayingViewModel` don't exist.

- [ ] **Step 3: Write the media item mapper**

`app/src/main/java/com/yplayer/player/MediaItemMapper.kt`:
```kotlin
package com.yplayer.player

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.yplayer.data.model.Song

fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(uri.toString())
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setDurationMs(durationMs)
            .build()
    )
    .build()

fun MediaItem.toSong(): Song {
    val uri = Uri.parse(mediaId)
    val id = uri.lastPathSegment?.toLongOrNull() ?: 0L
    return Song(
        id = id,
        title = mediaMetadata.title?.toString() ?: "",
        artist = mediaMetadata.artist?.toString() ?: "",
        album = mediaMetadata.albumTitle?.toString() ?: "",
        albumId = 0L,
        durationMs = mediaMetadata.durationMs ?: 0L,
        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
    )
}
```

- [ ] **Step 4: Write the playback service**

`app/src/main/java/com/yplayer/player/PlaybackService.kt`:
```kotlin
package com.yplayer.player

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    @Suppress("unused")
    fun playerPlaceholder(): MediaItem? = null
}
```

- [ ] **Step 5: Write the playback controller implementation**

`app/src/main/java/com/yplayer/player/PlaybackControllerImpl.kt`:
```kotlin
package com.yplayer.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.yplayer.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackControllerImpl(context: Context) : PlaybackController, Player.Listener {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var positionTicker: Job? = null

    init {
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        future.addListener({
            controller = future.get().also { it.addListener(this) }
        }, MoreExecutors.directExecutor())
    }

    override fun playQueue(songs: List<Song>, startIndex: Int) {
        val c = controller ?: return
        val items = songs.map { it.toMediaItem() }
        c.setMediaItems(items, startIndex.coerceIn(items.indices), 0L)
        c.prepare()
        c.play()
    }

    override fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    override fun next() {
        controller?.seekToNextMediaItem()
    }

    override fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    override fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    override fun setShuffle(on: Boolean) {
        controller?.shuffleModeEnabled = on
    }

    override fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_MEDIA_ITEM_TRANSITION,
                Player.EVENT_REPEAT_MODE_CHANGED,
                Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
            )
        ) {
            syncState()
        }
    }

    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
        // A file was deleted or became unreadable: skip to the next playable track.
        controller?.seekToNextMediaItem()
        syncState()
    }

    private fun syncState() {
        val c = controller ?: return
        _state.value = PlayerState(
            currentSong = c.currentMediaItem?.toSong(),
            isPlaying = c.isPlaying,
            positionMs = c.currentPosition,
            durationMs = c.duration.coerceAtLeast(0L),
            isShuffle = c.shuffleModeEnabled,
            repeatMode = c.repeatMode,
        )
        updateTicker(c.isPlaying)
    }

    private fun updateTicker(isPlaying: Boolean) {
        if (isPlaying) {
            positionTicker?.cancel()
            positionTicker = scope.launch {
                while (isActive) {
                    _state.value = _state.value.copy(positionMs = controller?.currentPosition ?: 0L)
                    delay(500)
                }
            }
        } else {
            positionTicker?.cancel()
        }
    }
}
```

- [ ] **Step 6: Wire the controller into `AppContainer`**

`AppContainer.kt`:
```kotlin
package com.yplayer

import android.app.Application
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.library.MediaStoreLibrarySource
import com.yplayer.player.PlaybackController
import com.yplayer.player.PlaybackControllerImpl

class AppContainer(private val app: Application) {
    val libraryRepository = LibraryRepository(MediaStoreLibrarySource(app))
    val playbackController: PlaybackController = PlaybackControllerImpl(app)
}
```

- [ ] **Step 7: Run tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.yplayer.player.MediaItemMapperTest" --tests "com.yplayer.ui.screens.nowplaying.NowPlayingViewModelTest"
```

Expected: PASS.

- [ ] **Step 8: Write the Now Playing ViewModel + screen**

`app/src/main/java/com/yplayer/ui/screens/nowplaying/NowPlayingViewModel.kt`:
```kotlin
package com.yplayer.ui.screens.nowplaying

import androidx.lifecycle.ViewModel
import com.yplayer.player.PlaybackController
import com.yplayer.player.PlayerState
import kotlinx.coroutines.flow.StateFlow

class NowPlayingViewModel(
    private val playbackController: PlaybackController,
) : ViewModel() {

    val state: StateFlow<PlayerState> = playbackController.state

    fun togglePlayPause() = playbackController.togglePlayPause()

    fun next() = playbackController.next()

    fun previous() = playbackController.previous()

    fun seekTo(positionMs: Long) = playbackController.seekTo(positionMs)

    fun setShuffle(on: Boolean) = playbackController.setShuffle(on)

    fun cycleRepeat() = playbackController.cycleRepeat()
}
```

`app/src/main/java/com/yplayer/ui/screens/nowplaying/NowPlayingScreen.kt`:
```kotlin
package com.yplayer.ui.screens.nowplaying

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.SkipNext
import androidx.compose.material.icons.automirrored.filled.SkipPrevious
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.Player
import com.yplayer.ui.components.AlbumArt
import com.yplayer.ui.util.appContainer

@Composable
fun NowPlayingScreen(
    viewModel: NowPlayingViewModel = viewModel(
        factory = viewModelFactory {
            initializer { NowPlayingViewModel(appContainer().playbackController) }
        }
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val song = state.currentSong

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AlbumArt(
            albumId = song?.albumId ?: 0L,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        Text(
            text = song?.title ?: "Nothing playing",
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            textAlign = TextAlign.Center,
        )
        Text(
            text = song?.artist ?: "",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            textAlign = TextAlign.Center,
        )
        Slider(
            value = state.positionMs.toFloat().coerceAtMost(state.durationMs.toFloat()),
            onValueChange = { viewModel.seekTo(it.toLong()) },
            valueRange = 0f..state.durationMs.coerceAtLeast(1L).toFloat(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        ) {
            Text(
                text = formatTime(state.positionMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(modifier = Modifier.weight(1f))
            Text(
                text = formatTime(state.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { viewModel.setShuffle(!state.isShuffle) }) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (state.isShuffle) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { viewModel.previous() }) {
                Icon(Icons.AutoMirrored.Filled.SkipPrevious, contentDescription = "Previous")
            }
            IconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier.size(80.dp),
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(72.dp),
                )
            }
            IconButton(onClick = { viewModel.next() }) {
                Icon(Icons.AutoMirrored.Filled.SkipNext, contentDescription = "Next")
            }
            IconButton(onClick = { viewModel.cycleRepeat() }) {
                Icon(
                    imageVector = if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne
                    else Icons.Filled.Repeat,
                    contentDescription = "Repeat",
                    tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box(modifier = Modifier.height(24.dp))
        Icon(
            imageVector = Icons.Filled.QueueMusic,
            contentDescription = "Queue",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
```

Note: remove the unused `width`/`height`/`Image`/`Row`-related imports if the compiler flags them (the `height(24.dp)` usage keeps `height`; delete `Image`, `width` if unused).

- [ ] **Step 9: Wire the Now Playing route in `MainScreen`**

Replace the placeholder `composable("nowPlaying") { /* replaced in Task 6 */ }` with:
```kotlin
composable("nowPlaying") { NowPlayingScreen() }
```
Import: `import com.yplayer.ui.screens.nowplaying.NowPlayingScreen`.

- [ ] **Step 10: Build + run all unit tests**

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`, all unit tests PASS.

- [ ] **Step 11: Manual verification (device)**

```bash
./gradlew :app:installDebug
```

Tap a song in the Songs tab. Expected: full-screen Now Playing opens, music plays, artwork shows, slider seeks, play/pause/next/previous work. Press home — Expected: media notification appears in the shade, and the music keeps playing. Grant notification permission if prompted.

- [ ] **Step 12: Commit**

```bash
git add -A
git commit -m "add media3 playback service and now playing screen"
```

---

## Task 7: Playlists — Room schema + repository (TDD)

**Files:**
- Create: `app/src/main/java/com/yplayer/data/db/PlaylistEntities.kt`
- Create: `app/src/main/java/com/yplayer/data/db/PlaylistDao.kt`
- Create: `app/src/main/java/com/yplayer/data/db/YPlayerDatabase.kt`
- Create: `app/src/main/java/com/yplayer/data/playlist/PlaylistRepository.kt`
- Create: `app/src/main/java/com/yplayer/data/playlist/RoomPlaylistRepository.kt`
- Test: `app/src/test/java/com/yplayer/data/playlist/RoomPlaylistRepositoryTest.kt`
- Modify: `app/src/main/java/com/yplayer/AppContainer.kt`

- [ ] **Step 1: Write the failing repository test**

`app/src/test/java/com/yplayer/data/playlist/RoomPlaylistRepositoryTest.kt`:
```kotlin
package com.yplayer.data.playlist

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yplayer.data.db.PlaylistDao
import com.yplayer.data.db.PlaylistSongDao
import com.yplayer.data.db.YPlayerDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomPlaylistRepositoryTest {

    private lateinit var db: YPlayerDatabase
    private lateinit var repo: RoomPlaylistRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, YPlayerDatabase::class.java).build()
        repo = RoomPlaylistRepository(db.playlistDao(), db.playlistSongDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun createAndGet_returnsOrderedByName() = runTest {
        repo.create("Road Trip")
        repo.create("Chill")

        val playlists = repo.getAll()
        assertEquals(listOf("Chill", "Road Trip"), playlists.map { it.name })
    }

    @Test
    fun addSongs_appendsAtEnd() = runTest {
        val id = repo.create("Mix")

        repo.addSong(id, 101)
        repo.addSong(id, 202)
        repo.addSong(id, 101)

        assertEquals(listOf(101L, 202L, 101L), repo.getPlaylistSongs(id))
    }

    @Test
    fun removeSong_deletesOnlyThatSong() = runTest {
        val id = repo.create("Mix")
        repo.addSong(id, 101)
        repo.addSong(id, 202)

        repo.removeSong(id, 101)

        assertEquals(listOf(202L), repo.getPlaylistSongs(id))
    }

    @Test
    fun reorder_updatesPositions() = runTest {
        val id = repo.create("Mix")
        repo.addSong(id, 101)
        repo.addSong(id, 202)
        repo.addSong(id, 303)

        repo.reorder(id, listOf(303L, 101L, 202L))

        assertEquals(listOf(303L, 101L, 202L), repo.getPlaylistSongs(id))
    }

    @Test
    fun isInPlaylist_checksMembership() = runTest {
        val id = repo.create("Mix")
        repo.addSong(id, 101)

        assertTrue(repo.isInPlaylist(id, 101))
        assertEquals(false, repo.isInPlaylist(id, 999))
    }

    @Test
    fun delete_removesPlaylistAndItsSongs() = runTest {
        val id = repo.create("Mix")
        repo.addSong(id, 101)

        repo.delete(id)

        assertEquals(0, repo.getAll().size)
    }

    @Test
    fun rename_updatesName() = runTest {
        val id = repo.create("Old")
        repo.rename(id, "New")

        assertEquals("New", repo.getAll().single().name)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "com.yplayer.data.playlist.RoomPlaylistRepositoryTest"
```

Expected: FAIL — entities/DAOs/database/repository don't exist.

- [ ] **Step 3: Write the entities**

`app/src/main/java/com/yplayer/data/db/PlaylistEntities.kt`:
```kotlin
package com.yplayer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "mediaId"])
data class PlaylistSongEntity(
    val playlistId: Long,
    val mediaId: Long,
    val position: Int,
)
```

- [ ] **Step 4: Write the DAOs**

`app/src/main/java/com/yplayer/data/db/PlaylistDao.kt`:
```kotlin
package com.yplayer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PlaylistDao {

    @Insert
    suspend fun insert(playlist: PlaylistEntity): Long

    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun get(id: Long): PlaylistEntity?

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Update
    suspend fun update(playlist: PlaylistEntity)
}

@Dao
interface PlaylistSongDao {

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position")
    suspend fun getSongs(playlistId: Long): List<PlaylistSongEntity>

    @Insert
    suspend fun insertAll(songs: List<PlaylistSongEntity>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND mediaId = :mediaId")
    suspend fun remove(playlistId: Long, mediaId: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun deleteAll(playlistId: Long)

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId AND mediaId = :mediaId")
    suspend fun count(playlistId: Long, mediaId: Long): Int
}
```

- [ ] **Step 5: Write the database**

`app/src/main/java/com/yplayer/data/db/YPlayerDatabase.kt`:
```kotlin
package com.yplayer.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PlaylistEntity::class, PlaylistSongEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class YPlayerDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao

    abstract fun playlistSongDao(): PlaylistSongDao

    companion object {
        fun get(context: Context): YPlayerDatabase =
            Room.databaseBuilder(context, YPlayerDatabase::class.java, "yplayer.db").build()
    }
}
```

- [ ] **Step 6: Write the repository interface + Room implementation**

`app/src/main/java/com/yplayer/data/playlist/PlaylistRepository.kt`:
```kotlin
package com.yplayer.data.playlist

data class Playlist(val id: Long, val name: String, val createdAt: Long)

interface PlaylistRepository {
    suspend fun create(name: String): Long
    suspend fun getAll(): List<Playlist>
    suspend fun delete(id: Long)
    suspend fun rename(id: Long, name: String)
    suspend fun getPlaylistSongs(id: Long): List<Long>
    suspend fun addSong(playlistId: Long, mediaId: Long)
    suspend fun removeSong(playlistId: Long, mediaId: Long)
    suspend fun reorder(playlistId: Long, orderedMediaIds: List<Long>)
    suspend fun isInPlaylist(playlistId: Long, mediaId: Long): Boolean
}
```

`app/src/main/java/com/yplayer/data/playlist/RoomPlaylistRepository.kt`:
```kotlin
package com.yplayer.data.playlist

import com.yplayer.data.db.PlaylistDao
import com.yplayer.data.db.PlaylistEntity
import com.yplayer.data.db.PlaylistSongDao
import com.yplayer.data.db.PlaylistSongEntity

class RoomPlaylistRepository(
    private val playlistDao: PlaylistDao,
    private val playlistSongDao: PlaylistSongDao,
) : PlaylistRepository {

    override suspend fun create(name: String): Long {
        val now = System.currentTimeMillis()
        return playlistDao.insert(PlaylistEntity(name = name, createdAt = now))
    }

    override suspend fun getAll(): List<Playlist> =
        playlistDao.getAll().map { Playlist(it.id, it.name, it.createdAt) }

    override suspend fun delete(id: Long) {
        playlistSongDao.deleteAll(id)
        playlistDao.delete(id)
    }

    override suspend fun rename(id: Long, name: String) {
        playlistDao.get(id)?.let {
            playlistDao.update(it.copy(name = name))
        }
    }

    override suspend fun getPlaylistSongs(id: Long): List<Long> =
        playlistSongDao.getSongs(id).map { it.mediaId }

    override suspend fun addSong(playlistId: Long, mediaId: Long) {
        val position = playlistSongDao.getSongs(playlistId).size
        playlistSongDao.insertAll(
            listOf(PlaylistSongEntity(playlistId = playlistId, mediaId = mediaId, position = position))
        )
    }

    override suspend fun removeSong(playlistId: Long, mediaId: Long) {
        playlistSongDao.remove(playlistId, mediaId)
        renumber(playlistId)
    }

    override suspend fun reorder(playlistId: Long, orderedMediaIds: List<Long>) {
        val existing = playlistSongDao.getSongs(playlistId).associateBy { it.mediaId }
        val updated = orderedMediaIds.mapIndexedNotNull { index, mediaId ->
            existing[mediaId]?.copy(position = index)
        }
        playlistSongDao.deleteAll(playlistId)
        playlistSongDao.insertAll(updated)
    }

    override suspend fun isInPlaylist(playlistId: Long, mediaId: Long): Boolean =
        playlistSongDao.count(playlistId, mediaId) > 0

    private suspend fun renumber(playlistId: Long) {
        val current = playlistSongDao.getSongs(playlistId)
        playlistSongDao.insertAll(current.mapIndexed { index, entity -> entity.copy(position = index) })
    }
}
```

- [ ] **Step 7: Wire the database into `AppContainer`**

`AppContainer.kt`:
```kotlin
package com.yplayer

import android.app.Application
import com.yplayer.data.db.YPlayerDatabase
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.library.MediaStoreLibrarySource
import com.yplayer.data.playlist.PlaylistRepository
import com.yplayer.data.playlist.RoomPlaylistRepository
import com.yplayer.player.PlaybackController
import com.yplayer.player.PlaybackControllerImpl

class AppContainer(private val app: Application) {
    private val database = YPlayerDatabase.get(app)

    val libraryRepository = LibraryRepository(MediaStoreLibrarySource(app))
    val playlistRepository: PlaylistRepository =
        RoomPlaylistRepository(database.playlistDao(), database.playlistSongDao())
    val playbackController: PlaybackController = PlaybackControllerImpl(app)
}
```

- [ ] **Step 8: Run tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.yplayer.data.playlist.RoomPlaylistRepositoryTest"
```

Expected: PASS (7 tests).

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "add room playlist schema and repository"
```

---

## Task 8: Playlists screens (list, create, delete, detail)

**Files:**
- Create: `app/src/main/java/com/yplayer/ui/screens/playlists/PlaylistsViewModel.kt`
- Create: `app/src/main/java/com/yplayer/ui/screens/playlists/PlaylistsScreen.kt`
- Create: `app/src/main/java/com/yplayer/ui/screens/playlistdetail/PlaylistDetailViewModel.kt`
- Create: `app/src/main/java/com/yplayer/ui/screens/playlistdetail/PlaylistDetailScreen.kt`
- Modify: `app/src/main/java/com/yplayer/ui/screens/PlaceholderScreens.kt` (remove Playlists placeholder)
- Modify: `app/src/main/java/com/yplayer/ui/MainScreen.kt`
- Test: `app/src/test/java/com/yplayer/ui/screens/playlists/PlaylistsViewModelTest.kt`
- Test: `app/src/test/java/com/yplayer/ui/screens/playlistdetail/PlaylistDetailViewModelTest.kt`

- [ ] **Step 1: Write the failing ViewModel tests**

`app/src/test/java/com/yplayer/data/playlist/FakePlaylistRepository.kt`:
```kotlin
package com.yplayer.data.playlist

class FakePlaylistRepository : PlaylistRepository {

    private val playlists = mutableListOf<Playlist>()
    private val songs = mutableMapOf<Long, MutableList<Long>>()
    private var nextId = 1L

    override suspend fun create(name: String): Long {
        val id = nextId++
        playlists += Playlist(id, name, 0L)
        return id
    }

    override suspend fun getAll(): List<Playlist> = playlists.sortedBy { it.name.lowercase() }

    override suspend fun delete(id: Long) {
        playlists.removeAll { it.id == id }
        songs.remove(id)
    }

    override suspend fun rename(id: Long, name: String) {
        val idx = playlists.indexOfFirst { it.id == id }
        if (idx >= 0) playlists[idx] = playlists[idx].copy(name = name)
    }

    override suspend fun getPlaylistSongs(id: Long): List<Long> = songs[id].orEmpty().toList()

    override suspend fun addSong(playlistId: Long, mediaId: Long) {
        songs.getOrPut(playlistId) { mutableListOf() }.add(mediaId)
    }

    override suspend fun removeSong(playlistId: Long, mediaId: Long) {
        songs[playlistId]?.remove(mediaId)
    }

    override suspend fun reorder(playlistId: Long, orderedMediaIds: List<Long>) {
        songs[playlistId] = orderedMediaIds.toMutableList()
    }

    override suspend fun isInPlaylist(playlistId: Long, mediaId: Long): Boolean =
        songs[playlistId]?.contains(mediaId) == true
}
```

`app/src/test/java/com/yplayer/ui/screens/playlists/PlaylistsViewModelTest.kt`:
```kotlin
package com.yplayer.ui.screens.playlists

import com.yplayer.data.playlist.FakePlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun createPlaylist_appearsInList() = runTest(dispatcher) {
        val repo = FakePlaylistRepository()
        val vm = PlaylistsViewModel(repo)
        advanceUntilIdle()

        vm.createPlaylist("Road Trip")
        advanceUntilIdle()

        assertEquals(listOf("Road Trip"), vm.playlists.value.map { it.name })
    }

    @Test
    fun deletePlaylist_removesFromList() = runTest(dispatcher) {
        val repo = FakePlaylistRepository()
        val vm = PlaylistsViewModel(repo)
        advanceUntilIdle()

        vm.createPlaylist("Road Trip")
        advanceUntilIdle()
        val id = vm.playlists.value.single().id
        vm.deletePlaylist(id)
        advanceUntilIdle()

        assertEquals(0, vm.playlists.value.size)
    }
}
```

`app/src/test/java/com/yplayer/ui/screens/playlistdetail/PlaylistDetailViewModelTest.kt`:
```kotlin
package com.yplayer.ui.screens.playlistdetail

import com.yplayer.data.library.FakeLibrarySource
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Song
import com.yplayer.data.playlist.FakePlaylistRepository
import com.yplayer.player.FakePlaybackController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val songs = listOf(
        Song(1, "A", "Artist", "Album", 10, 1000, android.net.Uri.parse("content://media/1")),
        Song(2, "B", "Artist", "Album", 10, 1000, android.net.Uri.parse("content://media/2")),
        Song(3, "C", "Artist", "Album", 10, 1000, android.net.Uri.parse("content://media/3")),
    )

    @Test
    fun songs_resolveMediaIdsInOrder() = runTest(dispatcher) {
        val playlistRepo = FakePlaylistRepository().also {
            it.create("Mix")
            it.addSong(1, 3)
            it.addSong(1, 1)
        }
        val library = LibraryRepository(FakeLibrarySource(songs))
        val vm = PlaylistDetailViewModel(playlistRepo, library, FakePlaybackController(), playlistId = 1)
        advanceUntilIdle()

        assertEquals(listOf(3L, 1L), vm.songs.value.map { it.id })
    }

    @Test
    fun removeSong_updatesList() = runTest(dispatcher) {
        val playlistRepo = FakePlaylistRepository().also {
            it.create("Mix")
            it.addSong(1, 3)
            it.addSong(1, 1)
        }
        val library = LibraryRepository(FakeLibrarySource(songs))
        val vm = PlaylistDetailViewModel(playlistRepo, library, FakePlaybackController(), playlistId = 1)
        advanceUntilIdle()

        vm.removeSong(0)
        advanceUntilIdle()

        assertEquals(listOf(3L), vm.songs.value.map { it.id })
    }

    @Test
    fun playSong_usesPlaylistContext() = runTest(dispatcher) {
        val controller = FakePlaybackController()
        val playlistRepo = FakePlaylistRepository().also {
            it.create("Mix")
            it.addSong(1, 3)
            it.addSong(1, 1)
        }
        val library = LibraryRepository(FakeLibrarySource(songs))
        val vm = PlaylistDetailViewModel(playlistRepo, library, controller, playlistId = 1)
        advanceUntilIdle()

        vm.playSong(1)

        assertEquals(listOf(3L, 1L), controller.lastQueue?.map { it.id })
        assertEquals(1, controller.lastStartIndex)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "com.yplayer.ui.screens.playlists.PlaylistsViewModelTest" --tests "com.yplayer.ui.screens.playlistdetail.PlaylistDetailViewModelTest"
```

Expected: FAIL — ViewModels don't exist.

- [ ] **Step 3: Write the ViewModels**

`app/src/main/java/com/yplayer/ui/screens/playlists/PlaylistsViewModel.kt`:
```kotlin
package com.yplayer.ui.screens.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yplayer.data.playlist.Playlist
import com.yplayer.data.playlist.PlaylistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaylistsViewModel(
    private val repository: PlaylistRepository,
) : ViewModel() {

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { _playlists.value = repository.getAll() }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.create(name)
            refresh()
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
            refresh()
        }
    }
}
```

`app/src/main/java/com/yplayer/ui/screens/playlistdetail/PlaylistDetailViewModel.kt`:
```kotlin
package com.yplayer.ui.screens.playlistdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Song
import com.yplayer.data.playlist.PlaylistRepository
import com.yplayer.player.PlaybackController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaylistDetailViewModel(
    private val playlistRepository: PlaylistRepository,
    private val libraryRepository: LibraryRepository,
    private val playbackController: PlaybackController,
    private val playlistId: Long,
) : ViewModel() {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isInPlaylist = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val isInPlaylist: StateFlow<Map<Long, Boolean>> = _isInPlaylist.asStateFlow()

    init {
        viewModelScope.launch {
            libraryRepository.refresh()
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val mediaIds = playlistRepository.getPlaylistSongs(playlistId)
            val allSongs = libraryRepository.songs.value.associateBy { it.id }
            _songs.value = mediaIds.mapNotNull { allSongs[it] }
            _isInPlaylist.value = allSongs.keys.associateWith { mediaIds.contains(it) }
        }
    }

    fun addSong(mediaId: Long) {
        viewModelScope.launch {
            playlistRepository.addSong(playlistId, mediaId)
            refresh()
        }
    }

    fun removeSong(index: Int) {
        val song = _songs.value.getOrNull(index) ?: return
        viewModelScope.launch {
            playlistRepository.removeSong(playlistId, song.id)
            refresh()
        }
    }

    fun moveSong(from: Int, to: Int) {
        val current = _songs.value
        if (from !in current.indices || to !in current.indices) return
        val reordered = current.toMutableList().apply {
            add(to, removeAt(from))
        }
        viewModelScope.launch {
            playlistRepository.reorder(playlistId, reordered.map { it.id })
            refresh()
        }
    }

    fun playSong(index: Int) {
        val list = _songs.value
        if (index in list.indices) playbackController.playQueue(list, index)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.yplayer.ui.screens.playlists.PlaylistsViewModelTest" --tests "com.yplayer.ui.screens.playlistdetail.PlaylistDetailViewModelTest"
```

Expected: PASS.

- [ ] **Step 5: Write the Playlists screen**

`app/src/main/java/com/yplayer/ui/screens/playlists/PlaylistsScreen.kt`:
```kotlin
package com.yplayer.ui.screens.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yplayer.data.playlist.Playlist
import com.yplayer.ui.util.appContainer

@Composable
fun PlaylistsScreen(
    onPlaylistClick: (Playlist) -> Unit = {},
    viewModel: PlaylistsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { PlaylistsViewModel(appContainer().playlistRepository) }
        }
    ),
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New playlist")
            }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(playlists, key = { it.id }) { playlist ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlaylistClick(playlist) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.createPlaylist(name)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onCreate(name.trim()) },
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
```

- [ ] **Step 6: Write the Playlist Detail screen**

`app/src/main/java/com/yplayer/ui/screens/playlistdetail/PlaylistDetailScreen.kt`:
```kotlin
package com.yplayer.ui.screens.playlistdetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yplayer.data.model.Song
import com.yplayer.ui.components.AlbumArt
import com.yplayer.ui.util.appContainer

@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    playlistName: String,
    onBack: () -> Unit,
    viewModel: PlaylistDetailViewModel = viewModel(
        key = "playlist-$playlistId",
        factory = viewModelFactory {
            initializer {
                val container = appContainer()
                PlaylistDetailViewModel(
                    container.playlistRepository,
                    container.libraryRepository,
                    container.playbackController,
                    playlistId,
                )
            }
        }
    ),
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val isInPlaylist by viewModel.isInPlaylist.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlistName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.padding(16.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Add songs", modifier = Modifier.padding(start = 8.dp))
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(songs, key = { it.id }) { song ->
                    PlaylistSongRow(
                        song = song,
                        onPlay = { viewModel.playSong(songs.indexOf(song)) },
                        onRemove = { viewModel.removeSong(songs.indexOf(song)) },
                        onMoveUp = { viewModel.moveSong(songs.indexOf(song), songs.indexOf(song) - 1) },
                        onMoveDown = { viewModel.moveSong(songs.indexOf(song), songs.indexOf(song) + 1) },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddSongsDialog(
            songs = viewModel.libraryAllSongs(),
            isInPlaylist = isInPlaylist,
            onAdd = { viewModel.addSong(it) },
            onClose = { showAddDialog = false },
        )
    }
}
```

Note: `viewModel.libraryAllSongs()` does not exist yet — it is added in Step 7 below.

- [ ] **Step 7: Add `libraryAllSongs()` to `PlaylistDetailViewModel` and the row/dialog composables**

Add to `PlaylistDetailViewModel.kt` (after `refresh`):
```kotlin
fun libraryAllSongs(): List<Song> = libraryRepository.songs.value
```

Add to `PlaylistDetailScreen.kt` (after the `PlaylistDetailScreen` composable):
```kotlin
@Composable
private fun PlaylistSongRow(
    song: Song,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(albumId = song.albumId, modifier = androidx.compose.ui.Modifier.size(48.dp))
        Column(modifier = androidx.compose.ui.Modifier.padding(start = 12.dp).weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(
                text = "${song.artist} · ${song.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        IconButton(onClick = onMoveUp) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up")
        }
        IconButton(onClick = onMoveDown) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down")
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = "Remove")
        }
    }
    HorizontalDivider(modifier = androidx.compose.ui.Modifier.padding(start = 16.dp))
}

@Composable
private fun AddSongsDialog(
    songs: List<Song>,
    isInPlaylist: Map<Long, Boolean>,
    onAdd: (Long) -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Add songs") },
        text = {
            LazyColumn(modifier = androidx.compose.ui.Modifier.height(400.dp)) {
                items(songs, key = { it.id }) { song ->
                    val added = isInPlaylist[song.id] == true
                    androidx.compose.foundation.layout.Row(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !added) { onAdd(song.id) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                            Text(song.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        if (added) {
                            Icon(Icons.Filled.Check, contentDescription = "Added")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Done") }
        },
    )
}
```

Add the missing imports to `PlaylistDetailScreen.kt`:
```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
```

- [ ] **Step 8: Remove the Playlists placeholder + wire routes in `MainScreen`**

Delete `PlaylistsScreen` from `PlaceholderScreens.kt` (the file now has nothing left — delete the file).

In `MainScreen.kt`, replace the placeholder `composable("playlists")`:
```kotlin
composable("playlists") {
    PlaylistsScreen(onPlaylistClick = { playlist ->
        navController.navigate("playlist/${playlist.id}/${Uri.encode(playlist.name)}")
    })
}
```
Add the detail route:
```kotlin
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
```
Import: `import com.yplayer.ui.screens.playlists.PlaylistsScreen` and `import com.yplayer.ui.screens.playlistdetail.PlaylistDetailScreen`.

- [ ] **Step 9: Build + run all unit tests**

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`, all unit tests PASS.

- [ ] **Step 10: Manual verification (device)**

```bash
./gradlew :app:installDebug
```

Expected: Playlists tab lists playlists; FAB creates one; tapping opens the detail where songs can be added, reordered with up/down, removed, and played.

- [ ] **Step 11: Commit**

```bash
git add -A
git commit -m "add playlists list and detail screens"
```

---

## Task 9: Search screen

**Files:**
- Create: `app/src/main/java/com/yplayer/ui/screens/search/SearchViewModel.kt`
- Create: `app/src/main/java/com/yplayer/ui/screens/search/SearchScreen.kt`
- Modify: `app/src/main/java/com/yplayer/ui/MainScreen.kt`
- Test: `app/src/test/java/com/yplayer/ui/screens/search/SearchViewModelTest.kt`
- Test: `app/src/androidTest/java/com/yplayer/ui/SearchUiTest.kt`

- [ ] **Step 1: Write the failing ViewModel test**

`app/src/test/java/com/yplayer/ui/screens/search/SearchViewModelTest.kt`:
```kotlin
package com.yplayer.ui.screens.search

import com.yplayer.data.library.FakeLibrarySource
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Song
import com.yplayer.player.FakePlaybackController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val songs = listOf(
        Song(1, "Blinding Lights", "The Weeknd", "After Hours", 10, 1000, android.net.Uri.parse("content://media/1")),
        Song(2, "Starboy", "The Weeknd", "Starboy", 20, 1000, android.net.Uri.parse("content://media/2")),
        Song(3, "Lose Yourself", "Eminem", "8 Mile", 30, 1000, android.net.Uri.parse("content://media/3")),
    )

    @Test
    fun query_filtersResultsLive() = runTest(dispatcher) {
        val vm = SearchViewModel(LibraryRepository(FakeLibrarySource(songs)), FakePlaybackController())
        advanceUntilIdle()

        assertEquals(3, vm.results.value.size)

        vm.onQueryChange("weeknd")
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L), vm.results.value.map { it.id })

        vm.onQueryChange("")
        advanceUntilIdle()

        assertEquals(3, vm.results.value.size)
    }

    @Test
    fun playSong_usesSearchContext() = runTest(dispatcher) {
        val controller = FakePlaybackController()
        val vm = SearchViewModel(LibraryRepository(FakeLibrarySource(songs)), controller)
        advanceUntilIdle()
        vm.onQueryChange("weeknd")
        advanceUntilIdle()

        vm.playSong(0)

        assertEquals(listOf(1L, 2L), controller.lastQueue?.map { it.id })
        assertEquals(0, controller.lastStartIndex)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.yplayer.ui.screens.search.SearchViewModelTest"
```

Expected: FAIL — `SearchViewModel` doesn't exist.

- [ ] **Step 3: Write the ViewModel**

`app/src/main/java/com/yplayer/ui/screens/search/SearchViewModel.kt`:
```kotlin
package com.yplayer.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Song
import com.yplayer.player.PlaybackController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchViewModel(
    private val repository: LibraryRepository,
    private val playbackController: PlaybackController,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val results: StateFlow<List<Song>> = combine(query, repository.songs) { q, songs ->
        repository.search(q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { repository.refresh() }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun playSong(index: Int) {
        val list = results.value
        if (index in list.indices) playbackController.playQueue(list, index)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.yplayer.ui.screens.search.SearchViewModelTest"
```

Expected: PASS.

- [ ] **Step 5: Write the Search screen**

`app/src/main/java/com/yplayer/ui/screens/search/SearchScreen.kt`:
```kotlin
package com.yplayer.ui.screens.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yplayer.ui.components.SongList
import com.yplayer.ui.util.appContainer

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onSongClick: (Int) -> Unit = {},
    viewModel: SearchViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val container = appContainer()
                SearchViewModel(container.libraryRepository, container.playbackController)
            }
        }
    ),
) {
    val results by viewModel.results.collectAsStateWithLifecycle()
    var text by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it
                            viewModel.onQueryChange(it)
                        },
                        placeholder = { Text("Search songs, artists, albums") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (text.isNotEmpty()) {
                                IconButton(onClick = {
                                    text = ""
                                    viewModel.onQueryChange("")
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SongList(songs = results, onSongClick = onSongClick)
        }
    }
}
```

- [ ] **Step 6: Wire the search route in `MainScreen`**

- Add a top bar search icon to the `Scaffold` in `MainScreen.kt`:

```kotlin
topBar = {
    TopAppBar(
        title = { Text("yPlayer") },
        actions = {
            IconButton(onClick = { navController.navigate("search") }) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
        },
    )
},
```

Add the route:
```kotlin
composable("search") {
    SearchScreen(
        onBack = { navController.popBackStack() },
        onSongClick = { navController.navigate("nowPlaying") },
    )
}
```

Add imports to `MainScreen.kt`:
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import com.yplayer.ui.screens.search.SearchScreen
```

Note: the `topBar` only shows on top-level tabs. To avoid it appearing on detail screens, conditionally render it only when the current route is one of the four tabs:
```kotlin
val isTopLevel = currentRoute in tabs.map { it.route }
...
topBar = {
    if (isTopLevel) {
        TopAppBar(title = { Text("yPlayer") }, actions = { ... })
    }
},
```
Define `currentRoute` before the `Scaffold` (move the `backStackEntry` lookup out of the `bottomBar` lambda).

- [ ] **Step 7: Write the Search UI test**

`app/src/androidTest/java/com/yplayer/ui/SearchUiTest.kt`:
```kotlin
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
```

- [ ] **Step 8: Build + run all unit tests**

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`, all unit tests PASS.

- [ ] **Step 9: Run UI tests (requires authorized device)**

```bash
./gradlew :app:connectedDebugAndroidTest
```

Expected: `NavigationTest` and `SearchUiTest` pass. Skip if no device is available.

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "add search screen"
```

---

## Task 10: Polish — empty states, refresh, and final verification

**Files:**
- Modify: `app/src/main/java/com/yplayer/ui/components/SongList.kt`
- Modify: `app/src/main/java/com/yplayer/ui/screens/albums/AlbumsScreen.kt`
- Modify: `app/src/main/java/com/yplayer/ui/screens/artists/ArtistsScreen.kt`
- Modify: `app/src/main/java/com/yplayer/ui/screens/playlists/PlaylistsScreen.kt`
- Modify: `app/src/main/java/com/yplayer/ui/screens/search/SearchScreen.kt`

- [ ] **Step 1: Add an empty-state composable**

`app/src/main/java/com/yplayer/ui/components/EmptyState.kt`:
```kotlin
package com.yplayer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(title: String, message: String = "", modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (message.isNotEmpty()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
```

- [ ] **Step 2: Wire empty states into the lists**

`SongList.kt` — when `songs` is empty, show an empty state instead of a blank list:
```kotlin
@Composable
fun SongList(songs: List<Song>, onSongClick: (Int) -> Unit, emptyMessage: String = "No songs") {
    if (songs.isEmpty()) {
        EmptyState(emptyMessage)
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) { /* unchanged */ }
    }
}
```

`AlbumsScreen.kt`:
```kotlin
if (albums.isEmpty()) {
    EmptyState("No albums", "Songs are grouped into albums automatically.")
} else {
    LazyVerticalGrid(...) { ... }
}
```

`ArtistsScreen.kt`:
```kotlin
if (artists.isEmpty()) {
    EmptyState("No artists")
} else {
    LazyColumn(...) { ... }
}
```

`SearchScreen.kt` — when the query is non-blank and results are empty:
```kotlin
if (results.isEmpty() && text.isNotBlank()) {
    EmptyState("No results for \"$text\"")
} else {
    SongList(songs = results, onSongClick = onSongClick)
}
```

- [ ] **Step 3: Add a pull-to-refresh library refresh**

Add a refresh action to `SongsScreen`. In the `Scaffold` of `MainScreen.kt`'s top bar, add a refresh button next to search that calls the current tab's refresh. Simplest robust approach: expose refresh through each list screen's ViewModel and add a `LaunchedEffect(Unit) { viewModel.refresh() }`-based re-query when the songs tab regains focus — implement by adding to `SongsViewModel`:

```kotlin
fun refresh() {
    viewModelScope.launch { repository.refresh() }
}
```

And in `SongsScreen`, wrap the list in a `PullToRefreshBox` (Material3 `pulltorefresh` — requires Compose BOM that includes it; BOM 2024.12.01 does):

```kotlin
val refreshState = rememberPullToRefreshState()
PullToRefreshBox(isRefreshing = false, state = refreshState, onRefresh = viewModel::refresh) {
    SongList(...)
}
```

If the `pulltorefresh` artifact is unavailable in the BOM, fall back to a manual refresh: re-call `viewModel.refresh()` from a `LaunchedEffect` keyed on navigation focus, or add a refresh `IconButton` in the top bar. Keep the change minimal.

- [ ] **Step 4: Full build + all tests**

```bash
./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`; all unit tests PASS; release APK minified successfully.

- [ ] **Step 5: Manual QA on device (if available)**

```bash
./gradlew :app:installDebug
```

Verify the full loop:
1. Grant permission → library loads.
2. Play from Songs / Album / Artist / Playlist / Search → Now Playing opens, background playback + notification work.
3. Shuffle, repeat, seek, next/prev all function.
4. Create playlist, add/remove/reorder songs, delete playlist.
5. Rotate the device — no crash, state retained.
6. Kill the app from recents while music plays → music keeps playing.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "polish empty states and library refresh"
```

---

## Self-Review Notes (from writing-plans skill)

**Spec coverage:** Songs/Albums/Artists (T4), Album/Artist detail (T5), Playback + Now Playing + background notification (T6), Playlists incl. reorder/add/remove (T7-T8), Search (T9), permissions (T4 gate), theme follows system + dynamic color (T2), empty states + refresh (T10), testing (unit tests throughout; optional androidTest T2/T9). All spec sections map to a task.

**Version pinning:** AGP 8.9.1 + Gradle 8.11.1 supports compileSdk 36. Kotlin 2.1.0 ships the Compose compiler plugin. If any artifact fails to resolve during Task 1, bump the failing version to its latest stable patch — the rest of the plan is version-agnostic.
