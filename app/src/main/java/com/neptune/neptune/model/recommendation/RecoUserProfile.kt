package com.neptune.neptune.model.recommendation

data class RecoUserProfile(val uid: String, val tagsWeight: Map<String, Double>) {}
