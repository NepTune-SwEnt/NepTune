package com.neptune.neptune.model

data class Profile(
    val uid: String,
    val username: String = "",
    val name: String? = null,
    val bio: String? = null,
    val subscriptions: Long = 0,
    val subscribers: Long = 0,
    val avatarUrl: String = ""
)

class UsernameTakenException(username: String) : Exception("Username taken: $username")