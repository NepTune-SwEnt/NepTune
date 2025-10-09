// Kotlin
import com.neptune.neptune.ui.navigation.Screen
import org.junit.Assert.*
import org.junit.Test

class ScreenTest {

  @Test
  fun testMainScreenProperties() {
    val screen = Screen.Main
    assertEquals("main", screen.route)
    assertEquals("Neptune", screen.name)
    assertTrue(screen.showBottomBar)
    assertFalse(screen.showBackButton)
  }

  @Test
  fun testEditScreenProperties() {
    val screen = Screen.Edit
    assertEquals("edit", screen.route)
    assertEquals("Edit", screen.name)
    assertTrue(screen.showBottomBar)
    assertFalse(screen.showBackButton)
  }

  @Test
  fun testProfileScreenProperties() {
    val screen = Screen.Profile
    assertEquals("profile", screen.route)
    assertEquals("My Profile", screen.name)
    assertFalse(screen.showBottomBar)
    assertTrue(screen.showBackButton)
  }

  @Test
  fun testScreenEquality() {
    assertEquals(Screen.Main, Screen.Main)
    assertEquals(Screen.Edit, Screen.Edit)
    assertEquals(Screen.Profile, Screen.Profile)
  }
}
