package com.neptune.neptune.model.recommendation

import com.neptune.neptune.model.sample.Sample
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class RecommendationEngineTest {

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private fun userWithTags(uid: String, vararg pairs: Pair<String, Double>): RecoUserProfile =
      RecoUserProfile(uid = uid, tagsWeight = mapOf(*pairs))

  private fun sample(
      id: String,
      name: String = id,
      tags: List<String> = emptyList(),
      likes: Int = 0,
      downloads: Int = 0,
      creationTime: Long = 0L,
      durationSeconds: Int = 0,
      comments: Int = 0
  ): Sample =
      Sample(
          id = id,
          name = name,
          description = "",
          durationMilliSecond = durationSeconds,
          tags = tags,
          likes = likes,
          usersLike = emptyList(),
          downloads = downloads,
          comments = comments,
          creationTime = creationTime)

  // -------------------------------------------------------------------------
  // tagSimilarity
  // -------------------------------------------------------------------------

  @Test
  fun tagSimilarityPerfectOverlapReturnsOne() {
    val userTags = mapOf("miku" to 1.0, "lofi" to 1.0)
    val sampleTags = listOf("miku", "lofi")

    val sim = RecommendationEngine.tagSimilarity(userTags, sampleTags)

    assertEquals(1.0, sim, 1e-9)
  }

  @Test
  fun tagSimilarityNoOverlapReturnsZero() {
    val userTags = mapOf("miku" to 1.0, "lofi" to 0.5)
    val sampleTags = listOf("rock", "metal")

    val sim = RecommendationEngine.tagSimilarity(userTags, sampleTags)

    assertEquals(0.0, sim, 1e-9)
  }

  @Test
  fun tagSimilarityPartialOverlapBetweenZeroAndOne() {
    val userTags = mapOf("miku" to 2.0, "lofi" to 1.0)
    val sampleTags = listOf("miku", "piano")

    val sim = RecommendationEngine.tagSimilarity(userTags, sampleTags)

    assertTrue(sim > 0.0)
    assertTrue(sim < 1.0)
  }

  @Test
  fun tagSimilarityEmptyUserOrSampleReturnsZero() {
    assertEquals(0.0, RecommendationEngine.tagSimilarity(emptyMap(), listOf("a", "b")), 1e-9)

    assertEquals(0.0, RecommendationEngine.tagSimilarity(mapOf("tag" to 1.0), emptyList()), 1e-9)
  }

  // -------------------------------------------------------------------------
  // popularityScore
  // -------------------------------------------------------------------------

  @Test
  fun popularityScoreZeroEngagementIsZero() {
    val score = RecommendationEngine.popularityScore(downloads = 0, likes = 0)
    assertEquals(0.0, score, 1e-9)
  }

  @Test
  fun popularityScoreIncreasesWithEngagement() {
    val low = RecommendationEngine.popularityScore(10, 0)
    val mid = RecommendationEngine.popularityScore(10, 90)
    val high = RecommendationEngine.popularityScore(1000, 500)

    assertTrue(low < mid)
    assertTrue(mid < high)
  }

  @Test
  fun popularityScoreUsesLogarithmicScaling() {
    val s1 = RecommendationEngine.popularityScore(10, 0)
    val s2 = RecommendationEngine.popularityScore(1000, 0)

    val rawRatio = 1000.0 / 10.0
    val scoreRatio = s2 / s1

    assertTrue(scoreRatio < 5.0)
  }

  // -------------------------------------------------------------------------
  // recencyScore
  // -------------------------------------------------------------------------

  @Test
  fun recencyScoreNonPositiveCreationTimeReturnsZero() {
    val now = System.currentTimeMillis()

    assertEquals(0.0, RecommendationEngine.recencyScore(0L, now), 1e-9)
    assertEquals(0.0, RecommendationEngine.recencyScore(-1L, now), 1e-9)
  }

  @Test
  fun recencyScoreRecentSamplesHaveHigherScore() {
    val now = System.currentTimeMillis()
    val oneDayAgo = now - TimeUnit.DAYS.toMillis(1)
    val tenDaysAgo = now - TimeUnit.DAYS.toMillis(10)

    val recent = RecommendationEngine.recencyScore(oneDayAgo, now)
    val older = RecommendationEngine.recencyScore(tenDaysAgo, now)

    assertTrue(recent > older)
  }

  @Test
  fun recencyScoreVeryRecentDoesNotReturnInfinity() {
    val now = System.currentTimeMillis()
    val score = RecommendationEngine.recencyScore(now, now)

    assertTrue(score.isFinite())
    assertTrue(score >= 0.0)
  }

  // -------------------------------------------------------------------------
  // scoreSample
  // -------------------------------------------------------------------------

  @Test
  fun scoreSamplePrefersTagMatchingSamples() {
    val now = System.currentTimeMillis()

    val user = userWithTags(uid = "user_tag_matching", "miku" to 3.0, "lofi" to 1.0)

    val match =
        sample(
            id = "match",
            name = "Matching sample — tags overlap",
            tags = listOf("miku", "lofi"),
            likes = 10,
            downloads = 10,
            creationTime = now - TimeUnit.DAYS.toMillis(1))

    val noMatch =
        sample(
            id = "nomatch",
            name = "No overlap sample",
            tags = listOf("rock"),
            likes = 10,
            downloads = 10,
            creationTime = match.creationTime)

    val scoreMatch = RecommendationEngine.scoreSample(match, user, now)
    val scoreNoMatch = RecommendationEngine.scoreSample(noMatch, user, now)

    assertTrue(scoreMatch > scoreNoMatch)
  }

  @Test
  fun scoreSampleRecencyBreaksTieOnPopularity() {
    val now = System.currentTimeMillis()

    val user = userWithTags(uid = "user_recency", "tag" to 1.0)

    val recent =
        sample(
            id = "recent",
            name = "Recent sample",
            tags = listOf("tag"),
            likes = 10,
            downloads = 10,
            creationTime = now - TimeUnit.DAYS.toMillis(1))

    val older =
        sample(
            id = "older",
            name = "Older sample",
            tags = listOf("tag"),
            likes = 10,
            downloads = 10,
            creationTime = now - TimeUnit.DAYS.toMillis(30))

    val sRecent = RecommendationEngine.scoreSample(recent, user, now)
    val sOlder = RecommendationEngine.scoreSample(older, user, now)

    assertTrue(sRecent > sOlder)
  }

  // -------------------------------------------------------------------------
  // rankSamplesForUser
  // -------------------------------------------------------------------------

  @Test
  fun rankSamplesForUserRespectsLimit() {
    val now = System.currentTimeMillis()

    val user = userWithTags(uid = "user_limit_test", "tag" to 1.0)

    val candidates =
        (1..100).map { idx ->
          sample(
              id = "s$idx",
              name = "Candidate sample #$idx",
              tags = if (idx % 2 == 0) listOf("tag") else listOf("other"),
              likes = idx,
              downloads = idx * 2,
              creationTime = now - idx * TimeUnit.DAYS.toMillis(1))
        }

    val result =
        RecommendationEngine.rankSamplesForUser(user = user, candidates = candidates, limit = 10)

    assertEquals(10, result.size)
  }

  @Test
  fun rankSamplesForUserFloatsTagRelevantSamplesToTop() {
    val now = System.currentTimeMillis()

    val user = userWithTags(uid = "user_relevance_test", "miku" to 5.0, "lofi" to 3.0)

    val relevant =
        sample(
            id = "relevant",
            name = "Perfect tag match sample",
            tags = listOf("miku", "lofi"),
            likes = 5,
            downloads = 5,
            creationTime = now - TimeUnit.DAYS.toMillis(2))

    val irrelevant =
        sample(
            id = "irrelevant",
            name = "High popularity but wrong tags",
            tags = listOf("fx", "noise"),
            likes = 1000,
            downloads = 5000,
            creationTime = relevant.creationTime)

    val ranked =
        RecommendationEngine.rankSamplesForUser(
            user = user, candidates = listOf(irrelevant, relevant), limit = 10)

    assertEquals(relevant.id, ranked.first().id)
  }

  @Test
  fun rankSamplesForUserEmptyCandidatesReturnsEmptyList() {
    val user = userWithTags(uid = "user_empty", "tag" to 1.0)

    val ranked =
        RecommendationEngine.rankSamplesForUser(user = user, candidates = emptyList(), limit = 10)

    assertTrue(ranked.isEmpty())
  }
}
