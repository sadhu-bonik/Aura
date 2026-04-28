package com.aura.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ShortlistRepository {

    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    suspend fun isShortlisted(userId: String, creatorId: String): Boolean {
        val snapshot = usersCollection.document(userId).get().await()
        val list = snapshot.get("shortlistedCreators") as? List<*> ?: emptyList<Any>()
        return list.contains(creatorId)
    }

    suspend fun toggleShortlist(userId: String, creatorId: String): Boolean {
        val currentlyShortlisted = isShortlisted(userId, creatorId)
        val userRef = usersCollection.document(userId)

        if (currentlyShortlisted) {
            userRef.update("shortlistedCreators", FieldValue.arrayRemove(creatorId)).await()
            return false
        } else {
            userRef.update("shortlistedCreators", FieldValue.arrayUnion(creatorId)).await()
            return true
        }
    }

    suspend fun getShortlistedCreatorIds(userId: String): List<String> {
        val snapshot = usersCollection.document(userId).get().await()
        return snapshot.get("shortlistedCreators") as? List<String> ?: emptyList()
    }
}
