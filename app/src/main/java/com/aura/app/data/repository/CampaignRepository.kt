package com.aura.app.data.repository

import com.aura.app.data.model.Campaign
import com.aura.app.utils.Constants
import com.aura.app.utils.StubData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CampaignRepository(private val db: FirebaseFirestore) {

    fun getCampaignsForBrand(brandId: String): Flow<List<Campaign>> = callbackFlow {
        if (Constants.USE_STUBS) {
            trySend(StubData.campaigns.filter { it.brandId == brandId })
            awaitClose {}
            return@callbackFlow
        }

        val registration = db.collection(Constants.COLLECTION_CAMPAIGNS)
            .whereEqualTo("brandId", brandId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot.toObjects(Campaign::class.java))
            }

        awaitClose { registration.remove() }
    }

    suspend fun getCampaign(campaignId: String): Campaign? {
        if (Constants.USE_STUBS) {
            return StubData.campaigns.firstOrNull { it.campaignId == campaignId }
        }
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

    suspend fun deleteCampaign(campaignId: String): Result<Unit> {
        return try {
            db.collection(Constants.COLLECTION_CAMPAIGNS)
                .document(campaignId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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
