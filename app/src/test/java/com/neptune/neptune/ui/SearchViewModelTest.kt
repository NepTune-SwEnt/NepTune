package com.neptune.neptune.ui.search

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.neptune.neptune.model.fakes.FakeProfileRepository
import com.neptune.neptune.model.fakes.FakeSampleRepository
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
  fun initialSamplesAreEmpty() {
    val vm =
        SearchViewModel(
            repo = fakeSampleRepo,
            profileRepo = fakeProfileRepo,
            context = appContext,
            useMockData = true)
    assertTrue(vm.samples.value.isEmpty())
  }

  @Test
  fun searchWithEmptyQueryLoadsAllSamples() {
    val vm =
        SearchViewModel(
            repo = fakeSampleRepo,
            profileRepo = fakeProfileRepo,
            context = appContext,
            useMockData = true)

    vm.search("") // should call loadData() then early return

    val ids = vm.samples.value.map { it.id }
    assertEquals(5, ids.size)
    assertTrue(ids.containsAll(listOf(1, 2, 3, 4, 5)))
  }

  @Test
  fun searchMatchesByNameDescriptionAndTagsNature() {
    val vm =
        SearchViewModel(
            repo = fakeSampleRepo,
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
    assertEquals(listOf(1, 3, 4, 5), ids)
  }

  @Test
  fun searchMatchesByTagIgnoringHashAndCase() {
    val vm =
        SearchViewModel(
            repo = fakeSampleRepo,
            profileRepo = fakeProfileRepo,
            context = appContext,
            useMockData = true)

    vm.search("#NATURE") // hash and upper-case should be normalized away

    val ids = vm.samples.value.map { it.id }.sorted()
    assertEquals(listOf(1, 3, 4, 5), ids)
  }

  @Test
  fun searchMatchesByDescriptionSea() {
    val vm =
        SearchViewModel(
            repo = fakeSampleRepo,
            profileRepo = fakeProfileRepo,
            context = appContext,
            useMockData = true)

    vm.search("sea")

    // 2 has tag #sea (tag match)
    // 3 has description "sea" (description match)
    val ids = vm.samples.value.map { it.id }.sorted()
    assertEquals(listOf(2, 3), ids)
  }

  @Test
  fun consecutiveSearchesResetBaseDataEachTime() {
    val vm =
        SearchViewModel(
            repo = fakeSampleRepo,
            profileRepo = fakeProfileRepo,
            context = appContext,
            useMockData = true)

    vm.search("nature")
    val afterNature = vm.samples.value.map { it.id }.sorted()
    assertEquals(listOf(1, 3, 4, 5), afterNature)

    // Should NOT filter within previous subset; loadData() runs again
    vm.search("sea")
    val afterSea = vm.samples.value.map { it.id }.sorted()
    assertEquals(listOf(2, 3), afterSea)
  }

  @Test
  fun clearingQueryAfterFilterRestoresAllResults() {
    val vm =
        SearchViewModel(
            repo = fakeSampleRepo,
            profileRepo = fakeProfileRepo,
            context = appContext,
            useMockData = true)

    vm.search("sea")
    assertEquals(listOf(2, 3), vm.samples.value.map { it.id }.sorted())

    vm.search("") // clear => loadData then early return
    val ids = vm.samples.value.map { it.id }.sorted()
    assertEquals(listOf(1, 2, 3, 4, 5), ids)
  }

  @Test
  fun searchWithNoMatchesReturnsEmptyList() {
    val vm =
        SearchViewModel(
            repo = fakeSampleRepo,
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
            repo = fakeSampleRepo,
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
            repo = fakeSampleRepo,
            profileRepo = fakeProfileRepo,
            context = appContext,
            useMockData = true)
    assertEquals("abc", vm.normalize("  abc  "))
  }
}
