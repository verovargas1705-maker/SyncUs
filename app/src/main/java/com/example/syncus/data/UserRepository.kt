package com.example.syncus.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun createOrMergeUserProfile(profile: UserProfile) {
        val data = hashMapOf(
            "uid" to profile.uid,
            "email" to profile.email,
            "displayName" to profile.displayName,
            "photoUrl" to profile.photoUrl,
            "phone" to profile.phone,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("users")
            .document(profile.uid)
            .set(data, SetOptions.merge())
            .await()
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        val snap = db.collection("users").document(uid).get().await()
        return if (snap.exists()) snap.toObject(UserProfile::class.java) else null
    }

    suspend fun updateProfile(uid: String, displayName: String, phone: String?) {
        val data = hashMapOf<String, Any?>(
            "displayName" to displayName,
            "phone" to phone
        )

        db.collection("users")
            .document(uid)
            .set(data, SetOptions.merge())
            .await()
    }
}