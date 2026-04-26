package com.aura.app.data.repository

import com.aura.app.data.model.Campaign
import com.aura.app.utils.Constants
import com.aura.app.utils.StubData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class CampaignRepository(private val db: FirebaseFirestore) {

    fun getCampaignsForBrand(brandId: String): Flow<List<Campaign>> = flow {
        if (Constants.USE_STUBS) {
            emit(StubData.campaigns.filter { it.brandId == brandId })
            return@flow
        }

        try {
            val snapshot = db.collection(Constants.COLLECTION_CAMPAIGNS)
                .whereEqualTo("brandId", brandId)
                .get()
                .await()
            val campaigns = snapshot.toObjects(Campaign::class.java)
            if (campaigns.isNotEmpty()) {
                emit(campaigns)
            } else {
                emit(StubData.campaigns)
            }
        } catch (e: Exception) {
            emit(StubData.campaigns)
        }
    }
}
