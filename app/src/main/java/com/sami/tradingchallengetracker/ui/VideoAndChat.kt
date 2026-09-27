package com.sami.tradingchallengetracker.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.google.firebase.firestore.Query
import com.sami.tradingchallengetracker.R
import com.sami.tradingchallengetracker.util.CloudAsyncImage
import com.sami.tradingchallengetracker.util.FirebaseCloudHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class AndroidAuthUser(
    val id: Int,
    val username: String,
    val displayName: String,
    val password: String
)

val AUTHORIZED_ANDROID_USERS = listOf(
    AndroidAuthUser(1, "Sami", "Sami", "12345678"),
    AndroidAuthUser(2, "Hani Alqadasi", "Hani Alqadasi", "Hani33334"),
    AndroidAuthUser(3, "Eslam Alqadasi", "Eslam Alqadasi", "55555555"),
    AndroidAuthUser(4, "Omar Ali", "Omar Ali", "Omar1234"),
    AndroidAuthUser(5, "Ahmed Saleh", "Ahmed Saleh", "Ahmed123"),
    AndroidAuthUser(6, "Mohammed Ali", "Mohammed Ali", "Mo2025"),
    AndroidAuthUser(7, "Khaled Nasser", "Khaled Nasser", "Khaled22"),
    AndroidAuthUser(8, "Yasser Ahmed", "Yasser Ahmed", "Yasser11"),
    AndroidAuthUser(9, "Ali Hassan", "Ali Hassan", "Ali2025"),
    AndroidAuthUser(10, "Abdullah Sami", "Abdullah Sami", "Abd12345"),
    AndroidAuthUser(11, "Faisal Omar", "Faisal Omar", "Faisal88"),
    AndroidAuthUser(12, "Mahmoud Adel", "Mahmoud Adel", "Mahmoud7"),
    AndroidAuthUser(13, "Tareq Salem", "Tareq Salem", "Tareq123"),
    AndroidAuthUser(14, "Zaid Ahmed", "Zaid Ahmed", "Zaid2025"),
    AndroidAuthUser(15, "Noor Ali", "Noor Ali", "Noor1234"),
    AndroidAuthUser(16, "Amjad Sami", "Amjad Sami", "Amjad555"),
    AndroidAuthUser(17, "Saleh Omar", "Saleh Omar", "Saleh2025"),
    AndroidAuthUser(18, "Nasser Ali", "Nasser Ali", "Nasser99"),
    AndroidAuthUser(19, "Tariq90", "Tariq90", "Tariq4141"),
    AndroidAuthUser(20, "Yazan Sami", "Yazan Sami", "Yazan2025")
)

/**
 * Looping Background Video using Media3 ExoPlayer:
 * - Uses the exact attached R.raw.background_video
 * - Autoplay on screen open
 * - Infinite loop (REPEAT_MODE_ALL)
 * - Mute (volume 0f)
 * - No controls (useController = false)
 * - Fullscreen Center Crop (RESIZE_MODE_ZOOM)
 * - 60-70% Transparent dark overlay
 * - Releases ExoPlayer resources on dispose
 */
@OptIn(UnstableApi::class)
@Composable
fun LoopingBackgroundVideo(
    modifier: Modifier = Modifier,
    rawResId: Int = R.raw.background_video,
    overlayAlpha: Float = 0.65f
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse("android.resource://${context.packageName}/$rawResId")
            val mediaItem = MediaItem.fromUri(uri)
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            }
        )

        // 60-70% Black Transparent Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = overlayAlpha))
        )
    }
}

/**
 * Android Offline Login Screen
 */
@Composable
fun AndroidLoginScreen(
    onLoginSuccess: (AndroidAuthUser) -> Unit
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Video Background with 70% dark overlay
        LoopingBackgroundVideo(rawResId = R.raw.background_video, overlayAlpha = 0.70f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xEA0D131F)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sami",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD700)
                    )
                    Text(
                        text = "متتبع رحلة التداول الشخصية • تسجيل الدخول",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    errorMessage?.let { msg ->
                        Surface(
                            color = Color(0x33EF4444),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = msg,
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            errorMessage = null
                        },
                        label = { Text("اسم المستخدم (Username)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFFFD700)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("كلمة المرور (Password)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFFD700)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val trimmedUser = username.trim()
                            val trimmedPass = password.trim()
                            val matched = AUTHORIZED_ANDROID_USERS.find {
                                it.username.equals(trimmedUser, ignoreCase = true) && it.password == trimmedPass
                            }
                            if (matched != null) {
                                val prefs = context.getSharedPreferences("sami_auth_prefs", Context.MODE_PRIVATE)
                                prefs.edit()
                                    .putInt("user_id", matched.id)
                                    .putString("username", matched.username)
                                    .putString("display_name", matched.displayName)
                                    .apply()

                                onLoginSuccess(matched)
                            } else {
                                errorMessage = "اسم المستخدم أو كلمة المرور غير صحيحة."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "تسجيل الدخول",
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

data class AndroidChatMessage(
    val id: String,
    val senderId: Int,
    val senderName: String,
    val text: String?,
    val imageUrl: String?,
    val storagePath: String?,
    val timestamp: Long
)

/**
 * Cloud Group Chat Screen: 💬 دفعة الرحلة إلى 3000$
 * - Real-time sync via Firebase Firestore (`chat_messages`)
 * - Cloud image storage via Firebase Storage (`chat_images`)
 * - Supports text, Gallery images, and Camera capture
 * - Auto-deletes messages and images older than 48 hours
 */
@Composable
fun AndroidGroupChatScreen(
    currentUser: AndroidAuthUser
) {
    val context = LocalContext.current
    var isOnline by remember { mutableStateOf(checkInternet(context)) }
    var inputText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var previewFullImageUrl by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var alertBanner by remember { mutableStateOf<String?>(null) }

    val messages = remember { mutableStateListOf<AndroidChatMessage>() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val fortyEightHoursMs = 48L * 3600L * 1000L

    // Monitor network connectivity changes
    DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline = true
            }
            override fun onLost(network: Network) {
                isOnline = checkInternet(context)
            }
        }
        try {
            cm?.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {
        }
        onDispose {
            try {
                cm?.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
    }

    // Subscribe to Firebase Firestore `chat_messages` in real-time
    DisposableEffect(Unit) {
        val db = FirebaseCloudHelper.getFirestore(context)
        val registration = db.collection("chat_messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    alertBanner = "تعذر الاتصال بخدمة الدردشة السحابية."
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val now = System.currentTimeMillis()
                val validList = mutableListOf<AndroidChatMessage>()
                val expiredList = mutableListOf<AndroidChatMessage>()

                for (doc in snapshot.documents) {
                    val msg = AndroidChatMessage(
                        id = doc.getString("id") ?: doc.id,
                        senderId = (doc.getLong("senderId") ?: 0L).toInt(),
                        senderName = doc.getString("senderName") ?: "متداول",
                        text = doc.getString("text"),
                        imageUrl = doc.getString("imageUrl"),
                        storagePath = doc.getString("storagePath"),
                        timestamp = doc.getLong("timestamp") ?: now
                    )
                    if (now - msg.timestamp < fortyEightHoursMs) {
                        validList.add(msg)
                    } else {
                        expiredList.add(msg)
                    }
                }

                // Auto-purge expired messages (> 48 hours) from Firestore and Storage
                if (expiredList.isNotEmpty()) {
                    coroutineScope.launch(Dispatchers.IO) {
                        for (exp in expiredList) {
                            try {
                                db.collection("chat_messages").document(exp.id).delete().await()
                                FirebaseCloudHelper.deleteCloudImage(context, exp.imageUrl, exp.storagePath)
                            } catch (_: Exception) {
                            }
                        }
                    }
                }

                validList.sortBy { it.timestamp }
                messages.clear()
                messages.addAll(validList)
            }

        onDispose {
            registration.remove()
        }
    }

    // Auto-scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            alertBanner = null
        }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && pendingCameraUri != null) {
            selectedImageUri = pendingCameraUri
            alertBanner = null
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val outUri = FirebaseCloudHelper.createCameraOutputUri(context)
            pendingCameraUri = outUri
            cameraLauncher.launch(outUri)
        } else {
            alertBanner = "يرجى السماح بصلاحية الكاميرا لالتقاط الصور."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A10))
    ) {
        // Chat Header
        Surface(
            color = Color(0xFF0D131F),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFD700)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💬", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "دفعة الرحلة إلى 3000$",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "20 عضواً • حذف تلقائي بعد 48 ساعة",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentUser.displayName,
                            color = Color(0xFFFFD700),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Offline Banner
        if (!isOnline) {
            Surface(
                color = Color(0x33EF4444),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("لا يوجد اتصال بالإنترنت.", color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // In-App Alert Banner
        alertBanner?.let { bannerMsg ->
            Surface(
                color = Color(0x33EF4444),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = bannerMsg,
                        color = Color(0xFFFCA5A5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { alertBanner = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Messages list
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💬", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("لا توجد رسائل بعد", color = Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("كن أول من يبدأ المحادثة في مجموعة دفعة الرحلة إلى 3000$!", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMe = msg.senderId == currentUser.id
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isMe) Alignment.Start else Alignment.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isMe) Color(0x33FFD700) else Color(0xFF131D30),
                            border = if (isMe) androidx.compose.foundation.BorderStroke(1.dp, Color(0x88FFD700)) else null,
                            modifier = Modifier.widthIn(max = 290.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isMe) "${msg.senderName} (أنت)" else msg.senderName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isMe) Color(0xFFFFD700) else Color(0xFF38BDF8)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                    Text(
                                        text = timeFormat.format(Date(msg.timestamp)),
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }

                                if (!msg.imageUrl.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 120.dp, max = 220.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { previewFullImageUrl = msg.imageUrl }
                                    ) {
                                        CloudAsyncImage(
                                            imageUrl = msg.imageUrl,
                                            contentDescription = "مرفق الدردشة",
                                            modifier = Modifier.fillMaxWidth(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                if (!msg.text.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = msg.text, color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Image Preview Banner before sending
        if (selectedImageUri != null) {
            Surface(
                color = Color(0xFF121C2E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "صورة جاهزة للإرسال",
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("صورة جاهزة للإرسال", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { selectedImageUri = null }) {
                        Icon(Icons.Default.Close, contentDescription = "إلغاء الصورة", tint = Color.LightGray)
                    }
                }
            }
        }

        // Input Bar with Gallery, Camera, Text Field, and Send
        Surface(
            color = Color(0xFF0D131F),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Picker Button
                IconButton(
                    enabled = isOnline && !isSending,
                    onClick = { galleryLauncher.launch("image/*") }
                ) {
                    Icon(Icons.Default.Image, contentDescription = "إرسال صورة من المعرض", tint = Color(0xFFFFD700))
                }

                // Camera Capture Button
                IconButton(
                    enabled = isOnline && !isSending,
                    onClick = {
                        val hasCamPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasCamPerm) {
                            val outUri = FirebaseCloudHelper.createCameraOutputUri(context)
                            pendingCameraUri = outUri
                            cameraLauncher.launch(outUri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "التقاط صورة بالكاميرا", tint = Color(0xFFFFD700))
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            if (isOnline) "اكتب رسالتك للمجموعة..." else "لا يوجد اتصال بالإنترنت.",
                            fontSize = 12.sp
                        )
                    },
                    enabled = isOnline && !isSending,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFD700),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    enabled = isOnline && !isSending && (inputText.isNotBlank() || selectedImageUri != null),
                    onClick = {
                        if (!isOnline) {
                            alertBanner = "لا يوجد اتصال بالإنترنت."
                            return@IconButton
                        }
                        val textToSend = inputText.trim()
                        val imageUriToSend = selectedImageUri
                        if (textToSend.isEmpty() && imageUriToSend == null) return@IconButton

                        isSending = true
                        alertBanner = null
                        coroutineScope.launch {
                            try {
                                val now = System.currentTimeMillis()
                                val msgId = "msg_${now}_${currentUser.id}_${UUID.randomUUID().toString().take(6)}"
                                var cloudImageUrl: String? = null
                                var storagePath: String? = null

                                if (imageUriToSend != null) {
                                    val uploaded = FirebaseCloudHelper.uploadImageToCloud(
                                        context,
                                        imageUriToSend,
                                        "chat_images",
                                        currentUser.id
                                    )
                                    cloudImageUrl = uploaded.first
                                    storagePath = uploaded.second
                                }

                                val payload = mutableMapOf<String, Any>(
                                    "id" to msgId,
                                    "senderId" to currentUser.id,
                                    "senderName" to currentUser.displayName,
                                    "timestamp" to now
                                )
                                if (textToSend.isNotEmpty()) payload["text"] = textToSend
                                if (cloudImageUrl != null) payload["imageUrl"] = cloudImageUrl
                                if (storagePath != null) payload["storagePath"] = storagePath

                                FirebaseCloudHelper.getFirestore(context)
                                    .collection("chat_messages")
                                    .document(msgId)
                                    .set(payload)
                                    .await()

                                inputText = ""
                                selectedImageUri = null
                            } catch (e: Exception) {
                                alertBanner = "تعذر إرسال الرسالة، يرجى التحقق من اتصال الإنترنت."
                            } finally {
                                isSending = false
                            }
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFFFD700))
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF0F172A)
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "إرسال", tint = Color(0xFF0F172A))
                    }
                }
            }
        }
    }

    // Fullscreen Image Preview Modal
    if (previewFullImageUrl != null) {
        Dialog(
            onDismissRequest = { previewFullImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { previewFullImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                CloudAsyncImage(
                    imageUrl = previewFullImageUrl,
                    contentDescription = "عرض الصورة",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { previewFullImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                }
            }
        }
    }
}

fun checkInternet(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val activeNet = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(activeNet) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
