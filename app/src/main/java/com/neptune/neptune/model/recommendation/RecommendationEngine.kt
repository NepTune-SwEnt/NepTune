package com.neptune.neptune.model.recommendation

import com.google.api.QuotaLimit
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.log10
import kotlin.math.sqrt

class RecommendationEngine(
    private val sampleRepo: SampleRepository
) {
    /**
     *
     */
    suspend fun getRecommendedSamplesForUser(
        user: RecoUserProfile,
        limit: Int = 50
    ): List<Sample> = withContext(Dispatchers.Default) {
        val candidates = mutableListOf<Sample>()

        //1) latest samples
        val latest = sampleRepo.getLatestSamples(limit)
        //2) trending
        val trending = sampleRepo.getTrendingSamples(limit)
        //3) tag-matching samples based on user's top tags
        val topTags = user.topTags(limit)
        val tagMatches = sampleRepo.getSamplesByTags(tags = topTags, perTagLimit = 40)
        candidates.addAll(latest)
        candidates.addAll(trending)
        candidates.addAll(tagMatches)
        val distinctCandidates: List<Sample> = candidates.distinctBy { it.id }
        val now = System.currentTimeMillis()

        //score each candidate
        val scored: List<Pair<Sample, Double>> = distinctCandidates.map { sample ->
            val score = scoreSample(sample, user, now)
            sample to score
        }
        scored.sortedByDescending {(_, score) -> score}
            .take(limit)
            .map { (sample, _) -> sample }
    }

    /**
     * Calculates a popularity score for user based on the number of downloads and likes,
     * recency and tags of a sample
     */
    private fun scoreSample(sample: Sample, user: RecoUserProfile, now: Long): Double {
        val tagSim = tagSimilarity(user.tagsWeight, sample.tags)
        val pop = popularityScore(sample.downloads, sample.likes)
        val recency = recencyScore(sample.creationTime, now)

        // Hyperparameters
        val alpha = 2.0  // tag similarity importance
        val beta  = 1.0  // popularity importance
        val gamma = 0.5  // recency importance

        return alpha * tagSim + beta * pop + gamma * recency
    }

    /**
     * Calculates the similarity of a sample's tags with a user's top tags
     * between 0 and 1, where 1 means a perfect match.
     */
    private fun tagSimilarity(
        userTagProfile: Map<String, Double>,
        sampleTags: List<String>
    ): Double {
        if (userTagProfile.isEmpty() || sampleTags.isEmpty()) return 0.0

        var dot = 0.0
        var userNormSq = 0.0

        // user vector norm
        for ((_, stat) in userTagProfile) {
            val w = stat
            userNormSq += w * w
        }

        val sampleTagSet = sampleTags.toSet()
        val sampleNormSq = sampleTagSet.size.toDouble()

        // dot product over overlapping tags
        for (tag in sampleTagSet) {
            val w = userTagProfile[tag]?: 0.0
            dot += w
        }

        val denom = sqrt(userNormSq) * sqrt(sampleNormSq)
        return if (denom == 0.0) 0.0 else dot / denom
    }

    /**
     * Calculates a score based on popularity that makes a huge difference in numbers still fair using
     * log
     */
    private fun popularityScore(downloads: Int, likes: Int): Double {
        val raw = downloads + likes
        return log10(1.0 + raw.toDouble())
    }

    /**
     * Recency score: samples get higher score if recent
     */
    private fun recencyScore(
        creationTime: Long,
        now: Long
    ): Double {
        if (creationTime <= 0L) return 0.0
        val ageMillis = (now - creationTime).coerceAtLeast(1L)
        val ageDays = ageMillis / (1000 * 60 * 60 * 24)
        return 1.0 / ageDays.toDouble()
    }
}