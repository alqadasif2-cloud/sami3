package com.sami.tradingchallengetracker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object FirebaseCloudHelper {
    private const val PROJECT_ID = "majestic-interface-ksmzh"
    private const val APP_ID = "1:462035009117:web:ef127abfd25d8ff2ec17b4"
    private const val API_KEY = "AIzaSyAR7YwgIFze6BcmUm7WTCSM30crDrMSTKk"
    private const val STORAGE_BUCKET = "majestic-interface-ksmzh.firebasestorage.app"
    private const val FIRESTORE_DB_ID = "ai-studio-c1d1b606-9578-42a6-b7b2-17faaec47f72"

    private val mediaByteCache = ConcurrentHashMap<String, ByteArray>()

    @Synchronized
    fun ensureInitialized(context: Context): FirebaseApp {
        val existing = FirebaseApp.getApps(context).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
        if (existing != null) return existing

        val options = FirebaseOptions.Builder()
            .setProjectId(PROJECT_ID)
            .setApplicationId(APP_ID)
            .setApiKey(API_KEY)
            .setStorageBucket(STORAGE_BUCKET)
            .build()

        return FirebaseApp.initializeApp(context.applicationContext, options)
    }

    fun getFirestore(context: Context): FirebaseFirestore {
        val app = ensureInitialized(context)
        return try {
            FirebaseFirestore.getInstance(app, FIRESTORE_DB_ID)
        } catch (_: Exception) {
            FirebaseFirestore.getInstance(app)
        }
    }

    fun getStorage(context: Context): FirebaseStorage {
        val app = ensureInitialized(context)
        return FirebaseStorage.getInstance(app)
    }

    fun createCameraOutputUri(context: Context): Uri {
        val cameraDir = File(context.cacheDir, "camera_captures").apply { mkdirs() }
        val file = File(cameraDir, "capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun compressUriToJpegBytes(context: Context, uri: Uri, maxDim: Int = 1280, quality: Int = 82): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("تعذر فتح ملف الصورة")
        val original = inputStream.use { BitmapFactory.decodeStream(it) }
            ?: throw IllegalArgumentException("تعذر قراءة الصورة")

        var width = original.width
        var height = original.height
        if (width > height) {
            if (width > maxDim) {
                height = (height * maxDim) / width
                width = maxDim
            }
        } else {
            if (height > maxDim) {
                width = (width * maxDim) / height
                height = maxDim
            }
        }

        val scaled = if (width != original.width || height != original.height) {
            Bitmap.createScaledBitmap(original, width, height, true)
        } else {
            original
        }

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return out.toByteArray()
    }

    suspend fun uploadImageToCloud(
        context: Context,
        imageUri: Uri,
        folder: String,
        uploaderId: Int
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val jpegBytes = compressUriToJpegBytes(context, imageUri, 1280, 82)
        val now = System.currentTimeMillis()
        val mediaId = "${folder}_${uploaderId}_${now}_${UUID.randomUUID().toString().take(6)}"
        val storagePath = "$folder/$mediaId.jpg"

        mediaByteCache[mediaId] = jpegBytes

        try {
            withTimeout(3500L) {
                val storageRef = getStorage(context).reference.child(storagePath)
                storageRef.putBytes(jpegBytes).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                Pair(downloadUrl, storagePath)
            }
        } catch (_: Exception) {
            val base64Str = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
            val dataUrl = "data:image/jpeg;base64,$base64Str"

            val db = getFirestore(context)
            db.collection("media_files").document(mediaId).set(
                mapOf(
                    "id" to mediaId,
                    "dataUrl" to dataUrl,
                    "folder" to folder,
                    "uploaderId" to uploaderId,
                    "createdAt" to now
                )
            ).await()

            val encodedPath = URLEncoder.encode(storagePath, "UTF-8")
            val encodedMediaId = URLEncoder.encode(mediaId, "UTF-8")
            val cloudUrl = "https://firebasestorage.googleapis.com/v0/b/$STORAGE_BUCKET/o/$encodedPath?alt=media&mediaId=$encodedMediaId"
            Pair(cloudUrl, storagePath)
        }
    }

    fun extractMediaId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (url.startsWith("cloud-media://")) {
            return url.removePrefix("cloud-media://").trim()
        }
        val regex = Regex("[?&]mediaId=([^&]+)")
        val match = regex.find(url)
        return match?.groupValues?.getOrNull(1)
    }

    fun extractStoragePath(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val regex = Regex("/o/([^?]+)")
        val match = regex.find(url) ?: return null
        return try {
            java.net.URLDecoder.decode(match.groupValues[1], "UTF-8")
        } catch (_: Exception) {
            null
        }
    }

    suspend fun resolveImageModel(context: Context, url: String?): Any? = withContext(Dispatchers.IO) {
        if (url.isNullOrBlank()) return@withContext null
        val mediaId = extractMediaId(url) ?: return@withContext url

        mediaByteCache[mediaId]?.let { return@withContext it }

        try {
            val snap = getFirestore(context).collection("media_files").document(mediaId).get().await()
            if (!snap.exists()) return@withContext null
            val dataUrl = snap.getString("dataUrl")
            if (!dataUrl.isNullOrBlank() && dataUrl.contains(",")) {
                val base64Part = dataUrl.substringAfter(",")
                val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                mediaByteCache[mediaId] = bytes
                return@withContext bytes
            }
        } catch (_: Exception) {
        }
        return@withContext url
    }

    suspend fun deleteCloudImage(context: Context, imageUrl: String?, storagePath: String? = null) = withContext(Dispatchers.IO) {
        val resolvedPath = storagePath ?: extractStoragePath(imageUrl)
        if (!resolvedPath.isNullOrBlank()) {
            try {
                getStorage(context).reference.child(resolvedPath).delete().await()
            } catch (_: Exception) {
            }
        }
        val mediaId = extractMediaId(imageUrl)
        if (!mediaId.isNullOrBlank()) {
            mediaByteCache.remove(mediaId)
            try {
                getFirestore(context).collection("media_files").document(mediaId).delete().await()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun deleteMultipleCloudImages(context: Context, imageUrls: List<String?>) = withContext(Dispatchers.IO) {
        imageUrls.filter { !it.isNullOrBlank() }.forEach { url ->
            deleteCloudImage(context, url, null)
        }
    }

    suspend fun syncUserChallengeToFirestore(
        context: Context,
        userId: Int,
        challenge: com.sami.tradingchallengetracker.data.ChallengeEntity,
        trades: List<com.sami.tradingchallengetracker.data.TradeEntity>
    ) = withContext(Dispatchers.IO) {
        try {
            val completedCount = trades.count { it.type == com.sami.tradingchallengetracker.data.TradeType.WIN }
            val failedCount = trades.count { it.type == com.sami.tradingchallengetracker.data.TradeType.LOSS }
            val remainingCount = (150 - completedCount).coerceAtLeast(0)

            val challengeMap = mapOf(
                "id" to challenge.id,
                "userName" to challenge.userName,
                "initialCapitalCents" to challenge.initialCapitalCents,
                "currentBalanceCents" to challenge.currentBalanceCents,
                "targetBalanceCents" to challenge.targetBalanceCents,
                "tradeCount" to challenge.tradeCount,
                "challengeStarted" to challenge.challengeStarted,
                "createdAt" to challenge.createdAt,
                "updatedAt" to challenge.updatedAt
            )
            val tradesList = trades.map { t ->
                mapOf(
                    "id" to t.id,
                    "challengeId" to t.challengeId,
                    "tradeNumber" to t.tradeNumber,
                    "resultCents" to t.resultCents,
                    "oldBalanceCents" to t.oldBalanceCents,
                    "newBalanceCents" to t.newBalanceCents,
                    "type" to t.type.name,
                    "attachmentPath" to t.attachmentPath,
                    "timestamp" to t.timestamp
                )
            }
            val totalMilestones = 150 + failedCount
            val milestonesList = (1..totalMilestones).map { num ->
                val trade = trades.find { it.tradeNumber == num }
                mapOf(
                    "id" to "${challenge.id}_milestone_$num",
                    "challengeId" to challenge.id,
                    "milestoneNumber" to num,
                    "profitCents" to trade?.resultCents,
                    "balanceCents" to trade?.newBalanceCents,
                    "status" to (trade?.type?.name ?: "PENDING"),
                    "tradeId" to trade?.id,
                    "timestamp" to trade?.timestamp,
                    "attachmentPath" to trade?.attachmentPath
                )
            }
            getFirestore(context).collection("user_challenges").document(userId.toString()).set(
                mapOf(
                    "userId" to userId,
                    "username" to challenge.userName,
                    "challenge" to challengeMap,
                    "completedCount" to completedCount,
                    "remainingCount" to remainingCount,
                    "failedCount" to failedCount,
                    "trades" to tradesList,
                    "milestones" to milestonesList,
                    "settings" to mapOf(
                        "userName" to challenge.userName,
                        "initialCapitalCents" to challenge.initialCapitalCents,
                        "targetBalanceCents" to challenge.targetBalanceCents
                    ),
                    "updatedAt" to challenge.updatedAt
                )
            ).await()
        } catch (_: Exception) {
        }
    }

    suspend fun fetchUserChallengeFromFirestore(
        context: Context,
        userId: Int
    ): Pair<com.sami.tradingchallengetracker.data.ChallengeEntity, List<com.sami.tradingchallengetracker.data.TradeEntity>>? = withContext(Dispatchers.IO) {
        try {
            val snap = getFirestore(context).collection("user_challenges").document(userId.toString()).get().await()
            if (!snap.exists()) return@withContext null
            val data = snap.data ?: return@withContext null
            val chMap = data["challenge"] as? Map<*, *> ?: return@withContext null

            val chId = chMap["id"] as? String ?: return@withContext null
            val challenge = com.sami.tradingchallengetracker.data.ChallengeEntity(
                id = chId,
                userId = userId,
                userName = (chMap["userName"] as? String) ?: "",
                initialCapitalCents = (chMap["initialCapitalCents"] as? Number)?.toLong() ?: 10000L,
                currentBalanceCents = (chMap["currentBalanceCents"] as? Number)?.toLong() ?: 10000L,
                targetBalanceCents = (chMap["targetBalanceCents"] as? Number)?.toLong() ?: 300000L,
                tradeCount = (chMap["tradeCount"] as? Number)?.toInt() ?: 0,
                challengeStarted = (chMap["challengeStarted"] as? Boolean) ?: false,
                createdAt = (chMap["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (chMap["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )

            val rawTrades = data["trades"] as? List<*> ?: emptyList<Any>()
            val trades = rawTrades.mapNotNull { item ->
                val m = item as? Map<*, *> ?: return@mapNotNull null
                val tId = m["id"] as? String ?: return@mapNotNull null
                val typeStr = m["type"] as? String ?: "WIN"
                com.sami.tradingchallengetracker.data.TradeEntity(
                    id = tId,
                    challengeId = chId,
                    tradeNumber = (m["tradeNumber"] as? Number)?.toInt() ?: 1,
                    resultCents = (m["resultCents"] as? Number)?.toLong() ?: 0L,
                    oldBalanceCents = (m["oldBalanceCents"] as? Number)?.toLong() ?: 0L,
                    newBalanceCents = (m["newBalanceCents"] as? Number)?.toLong() ?: 0L,
                    type = if (typeStr == "LOSS") com.sami.tradingchallengetracker.data.TradeType.LOSS else com.sami.tradingchallengetracker.data.TradeType.WIN,
                    attachmentPath = m["attachmentPath"] as? String,
                    timestamp = (m["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            }
            Pair(challenge, trades)
        } catch (_: Exception) {
            null
        }
    }
}

@Composable
fun CloudAsyncImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var resolvedModel by remember(imageUrl) { mutableStateOf<Any?>(null) }
    var isLoading by remember(imageUrl) { mutableStateOf(!imageUrl.isNullOrBlank()) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrBlank()) {
            resolvedModel = null
            isLoading = false
        } else {
            isLoading = true
            resolvedModel = FirebaseCloudHelper.resolveImageModel(context, imageUrl)
            isLoading = false
        }
    }

    if (isLoading || resolvedModel == null) {
        Box(
            modifier = modifier.background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color(0xFFFFD700)
            )
        }
    } else {
        AsyncImage(
            model = resolvedModel,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}
