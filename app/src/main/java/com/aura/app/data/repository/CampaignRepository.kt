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
            emit(snapshot.toObjects(Campaign::class.java))
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun getCampaign(campaignId: String): Campaign? {
        return try {
            db.collection(Constants.COLLECTION_CAMPAIGNS)
                .document(campaignId)
                .get()
                .await()
                .toObject(Campaign::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveCampaign(campaign: Campaign): Result<Unit> {
        return try {
            val ref = if (campaign.campaignId.isEmpty()) {
                db.collection(Constants.COLLECTION_CAMPAIGNS).document()
            } else {
                db.collection(Constants.COLLECTION_CAMPAIGNS).document(campaign.campaignId)
            }
            
            val finalCampaign = if (campaign.campaignId.isEmpty()) {
                campaign.copy(campaignId = ref.id, createdAt = com.google.firebase.Timestamp.now())
            } else {
                campaign
            }
            
            ref.set(finalCampaign).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
