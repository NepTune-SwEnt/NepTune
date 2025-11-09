package com.neptune.neptune.data.storage

import com.google.firebase.storage.FirebaseStorage

class StorageService(private val storage: FirebaseStorage) {
  private val storageRef = storage.reference
}
