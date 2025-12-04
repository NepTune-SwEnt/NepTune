package com.neptune.neptune.model.recommendation

import com.google.api.QuotaLimit
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.log10
import kotlin.math.sqrt

object RecommendationEngine{


    /**
     * Calculates a popularity score for user based on the number of downloads and likes,
     * recency and tags of a sample
     */
    fun scoreSample(sample: Sample, user: RecoUserProfile, now: Long): Double {
        val tagSim = tagSimilarity(user.tagsWeight, sample.tags)
        val pop = popularityScore(sample.downloads, sample.likes)
        val recency = recencyScore(sample.creationTime, now)

        // Hyperparameters
        val alpha = 4.0  // tag similarity importance
        val beta  = 1.0  // popularity importance
        val gamma = 0.5  // recency importance

        return alpha * tagSim + beta * pop + gamma * recency
    }

    /**
     * Calculates the similarity of a sample's tags with a user's top tags
     * between 0 and 1, where 1 means a perfect match.
     */
    fun tagSimilarity(
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
    fun popularityScore(downloads: Int, likes: Int): Double {
        val raw = downloads + likes
        return log10(1.0 + raw.toDouble())
    }

    /**
     * Recency score: samples get higher score if recent
     */
    fun recencyScore(
        creationTime: Long,
        now: Long
    ): Double {
        if (creationTime <= 0L) return 0.0
        val ageMillis = (now - creationTime).coerceAtLeast(1L)
        val ageDays = ageMillis / (1000.0 * 60.0 * 60.0 * 24.0)
        return 1.0 / (1.0 + ageDays.toDouble())
    }
    /**
     * rank samples for user based on their tags and likes, recency, and downloads
     */
    fun rankSamplesForUser(
        user: RecoUserProfile,
        candidates: List<Sample>,
        limit: Int = 50
        ): List<Sample> {
            if (candidates.isEmpty()) return emptyList()

            val now = System.currentTimeMillis()

            return candidates
                .distinctBy { it.id }
                .map { sample -> sample to scoreSample(sample, user, now) }
                .sortedByDescending { it.second }
                .take(limit)
                .map { it.first }
    }
}