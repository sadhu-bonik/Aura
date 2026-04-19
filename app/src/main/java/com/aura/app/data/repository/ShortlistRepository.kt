package com.aura.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ShortlistRepository {

    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("shortlists")

    suspend fun isShortlisted(brandId: String, creatorId: String): Boolean {
        val snap = collection
            .whereEqualTo("brandId", brandId)
            .whereEqualTo("creatorId", creatorId)
            .limit(1)
            .get()
            .await()
        return !snap.isEmpty
    }

    suspend fun toggleShortlist(brandId: String, creatorId: String): Boolean {
        val snap = collection
            .whereEqualTo("brandId", brandId)
            .whereEqualTo("creatorId", creatorId)
            .limit(1)
            .get()
            .await()

        if (snap.isEmpty) {
            val docRef = collection.document()
            val data = hashMapOf(
                "shortlistId" to docRef.id,
                "brandId" to brandId,
                "creatorId" to creatorId,
                "campaignId" to "",
                "note" to "",
                "savedAt" to Timestamp.now(),
            )
            docRef.set(data).await()
            return true
        } else {
            snap.documents.first().reference.delete().await()
            return false
        }
    }
}
