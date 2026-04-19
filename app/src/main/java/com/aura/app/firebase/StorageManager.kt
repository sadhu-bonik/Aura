package com.aura.app.firebase

import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StorageManager(
    private val context: Context,
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
) {
    /**
     * Uploads a content:// URI to chatAttachments/{dealId}/{uuid}.{ext}
     * and returns the public download URL.
     */
    suspend fun uploadChatAttachment(
        dealId: String,
        uri: Uri,
        mimeType: String,
    ): Result<String> = runCatching {
        val ext = mimeType.substringAfterLast("/", missingDelimiterValue = "bin")
        val path = "chatAttachments/$dealId/${UUID.randomUUID()}.$ext"
        val ref = storage.reference.child(path)

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: error("Cannot open input stream for $uri")
        inputStream.use { ref.putStream(it).await() }

        ref.downloadUrl.await().toString()
    }
}
