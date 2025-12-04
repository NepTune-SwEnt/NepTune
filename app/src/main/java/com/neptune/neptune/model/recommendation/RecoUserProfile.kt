package com.neptune.neptune.model.recommendation

data class RecoUserProfile (
    val uid: String,
    val tagsWeight: Map<String, Double>
)  {
    fun topTags(limit: Int = 10): List<String> =
        tagsWeight.entries.sortedByDescending { it.value }.take(limit).map { it.key }
}