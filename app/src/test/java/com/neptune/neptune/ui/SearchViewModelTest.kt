package com.neptune.neptune.ui

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.Timestamp
import com.neptune.neptune.model.fakes.FakeProfileRepository
import com.neptune.neptune.model.fakes.FakeSampleRepository
import com.neptune.neptune.ui.search.SearchViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/*
    Tests for SearchViewModel's search() functionality.
    Written with assistance from ChatGPT.
*/

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SearchViewModelTest {
  private val fakeSampleRepo = FakeSampleRepository()
  private val fakeProfileRepo = FakeProfileRepository()
  private val appContext: Context = ApplicationProvider.getApplicationContext<Application>()

  @Test
  fun searchWithEmptyQueryLoadsAllSamples() {
    val vm =
        SearchViewModel(
            sampleRepo = fakeSampleRepo,
            profileRepo = fakeProfileRepo,
            context = appContext,
            useMockData = true)

    vm.search("") // should call loadData() then early return

    val ids = vm.samples.value.map { it.id }
    assertEquals(5, ids.size)
    assertTrue(ids.containsAll(listOf("1", "2", "3", "4", "5")))
  }

  @Test
  fun searchMatchesByNameDescriptionAndTagsNature() {
    val vm =
        SearchViewModel(
            sampleRepo = fakeSampleRepo,
            profileRepo = fakeProfileRepo,
            context = appContext,
            useMockData = true)

    vm.search("nature")

    // With current implementation:
    // 1 has tag #nature
    // 3 has tag #nature
    // 4 name == "nature"
    // 5 has tag #nature
    val ids = vm.samples.value.map { it.id }.sorted()
    assertEquals(listOf("1", "3", "4", "5"), ids)
  }

  @Test
  fun searchMatchesByTagIgnoringHashAndCase() {
    val vm =
        SearchViewModel(
            sampleRepo = fakeSampleRepo,
            profileRepo = fakeProfileRepo,
            context = appContext,
            useMockData = true)

    vm.search("#NATURE") // hash and upper-case should be normalized away

    val ids = vm.samples.value.map { it.id }.sorted()
    assertEquals(listOf("1", "3", "4", "5"), ids)
  }

  @Test
  fun searchMatchesByDescriptionSea() {
    val vm =
        SearchViewModel(
            sampleRepo = fakeSampleRepo,
            profileRepo = fakeProfileRepo,
            context = appContext,
            useMockData = true)

    vm.search("sea")

    // 2 has tag #sea (tag match)
    // 3 has description "sea" (description match)
    val ids = vm.samples.value.map { it.id }.sorted()
    assertEquals(listOf("2", "3"), ids)
  }

  @Test
  fun consecutiveSearchesResetBaseDataEachTime() {
    val vm =
        SearchViewModel(
            sampleRepo = fakeSampleRepo,
            profileRepo = fakeProfileRepo,
            context = appContext,
            useMockData = true)

    vm.search("nature")
    val afterNature = vm.samples.value.map { it.id }.sorted()
    assertEquals(listOf("1", "3", "4", "5"), afterNature)

    // Should NOT filter within previous subset; loadData() runs again
    vm.search("sea")
    val afterSea = vm.samples.value.map { it.id }.sorted()
    assertEquals(listOf("2", "3"), afterSea)
  }

  @Test
  fun clearingQueryAfterFilterRestoresAllResults() {
    val vm =
        SearchViewModel(
            sampleRepo = fakeSampleRepo,
            profileRepo = fakeProfileRepo,
            context = appContext,
            useMockData = true)

    vm.search("sea")
    assertEquals(listOf("2", "3"), vm.samples.value.map { it.id }.sorted())

    vm.search("") // clear => loadData then early return
    val ids = vm.samples.value.map { it.id }.sorted()
    assertEquals(listOf("1", "2", "3", "4", "5"), ids)
  }

  @Test
  fun searchWithNoMatchesReturnsEmptyList() {
    val vm =
        SearchViewModel(
            sampleRepo = fakeSampleRepo,
            profileRepo = fakeProfileRepo,
            context = appContext,
            useMockData = true)

    vm.search("nope-no-match-123")

    assertTrue(vm.samples.value.isEmpty())
  }

  // ---- normalize() direct tests ----

  @Test
  fun normalizeRemovesNoiseAndLowercases() {
    val vm =
        SearchViewModel(
            sampleRepo = fakeSampleRepo,
            profileRepo = fakeProfileRepo,
            context = appContext,
            useMockData = true)
    assertEquals("nature", vm.normalize(" N a-T_U R E!! "))
    assertEquals("nature", vm.normalize("#Nature"))
    assertEquals("sea", vm.normalize(" s.e-a "))
    assertEquals("takeiteasy", vm.normalize("#takeItEasy"))
  }

  @Test
  fun normalizeTrimsWhitespace() {
    val vm =
        SearchViewModel(
            sampleRepo = fakeSampleRepo,
            profileRepo = fakeProfileRepo,
            context = appContext,
            useMockData = true)
    assertEquals("abc", vm.normalize("  abc  "))
  }

  @Test
  fun observeCommentsLoadsCommentsIntoViewModel() {
    val vm =
        SearchViewModel(
            sampleRepo = fakeSampleRepo,
            profileRepo = fakeProfileRepo,
            context = appContext,
            useMockData = true)

    val sampleId = "21"

    val now = Timestamp.now()
    val yesterday = Timestamp(now.seconds - 86400, 0)

    // Insert fake comments in repo
    fakeSampleRepo.addComment(sampleId, "u1", "Alice", "Nice!", now)
    fakeSampleRepo.addComment(sampleId, "u2", "Bob", "Wow!", yesterday)

    vm.observeCommentsForSamplePublic(sampleId)

    val comments = vm.comments.value
    val usernames = vm.usernames.value
    assertEquals(2, comments.size)

    assertEquals("Alice", comments[0].authorName)
    assertEquals("Nice!", comments[0].text)

    assertEquals("Bob", comments[1].authorName)
    assertEquals("Wow!", comments[1].text)

    assertEquals("anonymous", usernames["u1"])
    assertEquals("anonymous", usernames["u2"])
  }

  @Test
  fun loadUsernameUpdatesUsername() = runTest {
    val vm =
        SearchViewModel(
            sampleRepo = fakeSampleRepo,
            profileRepo = fakeProfileRepo,
            context = appContext,
            useMockData = true)

    val userId = "u1"

    vm.loadUsernamePublic(userId)

    // The fake repo returns the ID as username
    assertEquals("anonymous", vm.usernames.value[userId])
  }

  @Test
  fun addCommentAddsCommentAndUpdatesUsernames() = runTest {
    val vm =
        SearchViewModel(
            sampleRepo = fakeSampleRepo,
            profileRepo = fakeProfileRepo,
            context = appContext,
            useMockData = true)

    val sampleId = "21"

    // Add a new comment
    vm.onAddCommentPublic(sampleId, "Hello world")

    val comments = vm.comments.value
    val usernames = vm.usernames.value

    // There should be one comment
    assertEquals(1, comments.size)
    assertEquals("testUid", comments[0].authorId)
    assertEquals("johndoe", comments[0].authorName)
    assertEquals("johndoe", usernames["testUid"])
  }
}
