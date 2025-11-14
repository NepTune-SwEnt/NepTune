package com.neptune.neptune.ui.search

import org.junit.Assert.*
import org.junit.Test

/*
    Tests for SearchViewModel's search() functionality.
    Written with assistance from ChatGPT.

*/

class SearchViewModelTest {

  @Test
  fun `initial samples are empty`() {
    val vm = SearchViewModel()
    assertTrue(vm.samples.value.isEmpty())
  }

  @Test
  fun `search with empty query loads all 5 samples`() {
    val vm = SearchViewModel()

    vm.search("") // should call loadData() then early return

    val ids = vm.samples.value.map { it.id }
    assertEquals(5, ids.size)
    assertTrue(ids.containsAll(listOf("1", "2", "3", "4", "5")))
  }

  @Test
  fun `search matches by name description and tags (nature)`() {
    val vm = SearchViewModel()

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
  fun `search matches by tag ignoring hash and case`() {
    val vm = SearchViewModel()

    vm.search("#NATURE") // hash and upper-case should be normalized away

    val ids = vm.samples.value.map { it.id }.sorted()
    assertEquals(listOf("1", "3", "4", "5"), ids)
  }

  @Test
  fun `search matches by description (sea)`() {
    val vm = SearchViewModel()

    vm.search("sea")

    // 2 has tag #sea (tag match)
    // 3 has description "sea" (description match)
    val ids = vm.samples.value.map { it.id }.sorted()
    assertEquals(listOf("2", "3"), ids)
  }

  @Test
  fun `consecutive searches reset base data each time`() {
    val vm = SearchViewModel()

    vm.search("nature")
    val afterNature = vm.samples.value.map { it.id }.sorted()
    assertEquals(listOf("1", "3", "4", "5"), afterNature)

    // Should NOT filter within previous subset; loadData() runs again
    vm.search("sea")
    val afterSea = vm.samples.value.map { it.id }.sorted()
    assertEquals(listOf("2", "3"), afterSea)
  }

  @Test
  fun `clearing query after a filter restores all results`() {
    val vm = SearchViewModel()

    vm.search("sea")
    assertEquals(listOf("2", "3"), vm.samples.value.map { it.id }.sorted())

    vm.search("") // clear => loadData then early return
    val ids = vm.samples.value.map { it.id }.sorted()
    assertEquals(listOf("1", "2", "3", "4", "5"), ids)
  }

  @Test
  fun `search with no matches returns empty list`() {
    val vm = SearchViewModel()

    vm.search("nope-no-match-123")

    assertTrue(vm.samples.value.isEmpty())
  }

  // ---- normalize() direct tests ----

  @Test
  fun `normalize removes spaces punctuation and hashes and lowercases`() {
    val vm = SearchViewModel()
    assertEquals("nature", vm.normalize(" N a-T_U R E!! "))
    assertEquals("nature", vm.normalize("#Nature"))
    assertEquals("sea", vm.normalize(" s.e-a "))
    assertEquals("takeiteasy", vm.normalize("#takeItEasy"))
  }

  @Test
  fun `normalize trims`() {
    val vm = SearchViewModel()
    assertEquals("abc", vm.normalize("  abc  "))
  }
}
