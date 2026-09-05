package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.JyotishTheme
import com.example.ui.viewmodel.AstrologyViewModel
import com.example.data.engine.MockAstrologyEngine
import com.example.domain.FakeProfileRepository
import com.example.domain.location.LocationResolver
import com.example.domain.location.LocationRepository
import com.example.domain.models.BirthLocation
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

class FakeLocationResolver : LocationResolver {
    override suspend fun resolveLocation(query: String): Result<List<BirthLocation>> = Result.success(emptyList())
}

class FakeLocationRepository : LocationRepository {
    override suspend fun saveVerifiedLocation(location: BirthLocation) {}
    override suspend fun getVerifiedLocation(): BirthLocation? = null
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun home_screen_screenshot() {
    val viewModel = AstrologyViewModel(MockAstrologyEngine(), FakeLocationResolver(), FakeLocationRepository(), FakeProfileRepository())

    composeTestRule.setContent {
      JyotishTheme {
        HomeScreen(viewModel = viewModel)
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/home_screen.png")
  }
}
