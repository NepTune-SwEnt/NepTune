package com.neptune.neptune.util

import com.google.firebase.database.FirebaseDatabase

object RealtimeDatabaseProvider {

  /** Always returns the FirebaseDatabase instance for the Europe-west1 DB. */
  fun getDatabase(): FirebaseDatabase =
      FirebaseDatabase.getInstance(
          "https://neptune-e2728-default-rtdb.europe-west1.firebasedatabase.app/")
}
