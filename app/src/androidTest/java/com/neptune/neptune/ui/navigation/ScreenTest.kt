// Kotlin
import com.neptune.neptune.ui.navigation.Screen
import org.junit.Assert.*
import org.junit.Test

class ScreenTest {

  @Test
  fun testMainScreenProperties() {
    val screen = Screen.Main
    assertEquals("main", screen.route)
    assertTrue(screen.showBottomBar)
  }

  @Test
  fun testEditScreenProperties() {
    val screen = Screen.Edit
    assertEquals("edit_screen/{zipFilePath}", screen.route)
    assertTrue(screen.showBottomBar)
  }

  @Test
  fun testProfileScreenProperties() {
    val screen = Screen.Profile
    assertEquals("profile", screen.route)
    assertFalse(screen.showBottomBar)
  }

  @Test
  fun testScreenEquality() {
    assertEquals(Screen.Main, Screen.Main)
    assertEquals(Screen.Edit, Screen.Edit)
    assertEquals(Screen.Profile, Screen.Profile)
  }
}
