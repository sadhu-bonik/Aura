package com.aura.app

import com.aura.app.data.model.PortfolioItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DebugFirestore {
    suspend fun listAllVideos() {
        val db = FirebaseFirestore.getInstance()
        val snap = db.collection("portfolioItems").get().await()
        println("TOTAL VIDEO DOCUMENTS: ${snap.size()}")
        snap.documents.forEach { doc ->
            val item = doc.toObject(PortfolioItem::class.java)
            println("ID: ${doc.id}, Creator: ${item?.creatorId}, Public: ${item?.public}, Date: ${item?.createdAt}")
        }
    }
}
