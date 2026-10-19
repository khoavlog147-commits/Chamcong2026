package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AdminNotification
import com.example.ui.theme.*
import com.example.viewmodel.TimeSnapViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    viewModel: TimeSnapViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val notifications by viewModel.adminNotifications.collectAsStateWithLifecycle()
    val readIds by viewModel.readNotificationIds.collectAsStateWithLifecycle()
    val isLoading by viewModel.isRefreshingNotifications.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val session by viewModel.currentUserSession.collectAsStateWithLifecycle()
    val userConfig by viewModel.userConfig.collectAsStateWithLifecycle()
    val isAdmin = userConfig?.isAdmin == true || session?.email?.lowercase() == "khoatubexxx@gmail.com"

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "UNREAD", "ADMIN", "REMINDER"
    var selectedNotifForDetail by remember { mutableStateOf<AdminNotification?>(null) }
    var notificationToDeleteByAdmin by remember { mutableStateOf<AdminNotification?>(null) }

    // Refresh notifications when screen loads
    LaunchedEffect(Unit) {
        viewModel.fetchAdminNotifications()
    }

    val filteredNotifications = remember(notifications, readIds, searchQuery, selectedFilter) {
        notifications.filter { notif ->
            val matchesSearch = searchQuery.isBlank() ||
                    notif.title.contains(searchQuery, ignoreCase = true) ||
                    notif.message.contains(searchQuery, ignoreCase = true) ||
                    notif.sentBy.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "UNREAD" -> notif.id !in readIds
                "ADMIN" -> notif.type == "GENERAL" || notif.type.isBlank()
                "REMINDER" -> notif.type == "SHIFT_REMINDER" || notif.type == "SHIFT_CHANGE" || notif.type == "AUTO_TIME_APPROVED"
                else -> true
            }

            matchesSearch && matchesFilter
        }.sortedByDescending { it.createdAt }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Trung Tâm Thông Báo",
                                fontWeight = FontWeight.Bold,
                                color = White,
                                fontSize = 18.sp
                            )
                            if (unreadCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = AccentOrange,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "$unreadCount",
                                        color = White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Lịch sử tin nhắn & thông báo từ Ban quản trị",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back_notif_center")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = White
                        )
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        IconButton(
                            onClick = {
                                viewModel.markAllNotificationsAsRead()
                                Toast.makeText(context, "Đã đánh dấu tất cả là đã đọc", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("btn_mark_all_read")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Đọc tất cả",
                                tint = AccentGreen
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkContainer.copy(alpha = 0.95f)
                ),
                modifier = Modifier.border(0.dp, CardBorder)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.fetchAdminNotifications() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_notif"),
                placeholder = { Text("Tìm kiếm nội dung thông báo...", color = TextSecondary, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Tìm",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Xóa", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkContainer,
                    unfocusedContainerColor = DarkContainer,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = White,
                    unfocusedTextColor = White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipItem(
                    label = "Tất cả (${notifications.size})",
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" }
                )
                FilterChipItem(
                    label = "Chưa đọc ($unreadCount)",
                    selected = selectedFilter == "UNREAD",
                    onClick = { selectedFilter = "UNREAD" },
                    badgeColor = if (unreadCount > 0) AccentOrange else null
                )
                FilterChipItem(
                    label = "Admin",
                    selected = selectedFilter == "ADMIN",
                    onClick = { selectedFilter = "ADMIN" }
                )
                FilterChipItem(
                    label = "Nhắc ca",
                    selected = selectedFilter == "REMINDER",
                    onClick = { selectedFilter = "REMINDER" }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Notification List
            if (filteredNotifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryBlue.copy(alpha = 0.15f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MarkEmailRead,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Không tìm thấy thông báo phù hợp" else "Không có thông báo nào",
                            fontWeight = FontWeight.Bold,
                            color = White,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Hãy thử thay đổi từ khóa tìm kiếm" else "Các tin nhắn và nhắc nhở từ Ban quản trị sẽ xuất hiện ở đây.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredNotifications, key = { it.id }) { notif ->
                        val isRead = notif.id in readIds
                        NotificationCardItem(
                            notif = notif,
                            isRead = isRead,
                            isAdmin = isAdmin,
                            onClick = {
                                viewModel.markNotificationAsRead(notif.id)
                                selectedNotifForDetail = notif
                            },
                            onDelete = {
                                viewModel.deleteNotificationLocally(notif.id)
                                Toast.makeText(context, "Đã xóa khỏi lịch sử", Toast.LENGTH_SHORT).show()
                            },
                            onDeleteAsAdmin = {
                                notificationToDeleteByAdmin = notif
                            }
                        )
                    }
                }
            }
        }
    }
    }

    // Admin Confirmation Dialog for deleting notification (including pinned notifications)
    if (notificationToDeleteByAdmin != null) {
        val target = notificationToDeleteByAdmin!!
        AlertDialog(
            onDismissRequest = { notificationToDeleteByAdmin = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = DangerRed)
                    Text("Xác nhận xóa thông báo", color = White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Bạn đang thao tác với quyền Admin:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Surface(
                        color = DarkBackground,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = target.title.ifBlank { "Thông báo" },
                                color = White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (target.isPinned) {
                                Text(
                                    text = "📌 Thông báo đã ghim",
                                    color = AccentOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = target.message,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = "Thông báo này sẽ được xóa vĩnh viễn khỏi toàn bộ hệ thống và thiết bị của mọi nhân viên.",
                        color = AccentOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idToDelete = target.id
                        notificationToDeleteByAdmin = null
                        if (selectedNotifForDetail?.id == idToDelete) {
                            selectedNotifForDetail = null
                        }
                        viewModel.deleteNotificationAsAdmin(idToDelete) { success ->
                            if (success) {
                                Toast.makeText(context, "Đã xóa thông báo khỏi hệ thống", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Lỗi xóa thông báo từ máy chủ", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Xác nhận xóa", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { notificationToDeleteByAdmin = null }) {
                    Text("Hủy", color = TextSecondary)
                }
            },
            containerColor = DarkContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Notification Detail Dialog
    if (selectedNotifForDetail != null) {
        val notif = selectedNotifForDetail!!
        val isRead = notif.id in readIds
        val sdf = remember { SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault()) }
        val timeString = remember(notif.createdAt) { sdf.format(Date(notif.createdAt)) }

        AlertDialog(
            onDismissRequest = { selectedNotifForDetail = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = getNotifColor(notif.type).copy(alpha = 0.2f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = getNotifIcon(notif.type),
                                contentDescription = null,
                                tint = getNotifColor(notif.type),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = notif.title.ifBlank { "Thông Báo Admin" },
                            fontWeight = FontWeight.Bold,
                            color = White,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Từ: ${notif.sentBy} • $timeString",
                            color = TextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = CardBorder)

                    Surface(
                        color = DarkBackground.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Text(
                            text = notif.message,
                            color = White,
                            fontSize = 14.5.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gửi tới: ${if (notif.targetUid == "ALL") "Tất cả nhân viên" else notif.targetName}",
                            color = NeonBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Thông báo", notif.message)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Đã sao chép nội dung", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Sao chép", modifier = Modifier.size(14.dp), tint = TextSecondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sao chép", fontSize = 12.sp, color = TextSecondary)
                        }
                    }

                    if (notif.isPinned) {
                        Surface(
                            color = AccentOrange.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.PushPin, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(16.dp))
                                Text(
                                    text = if (isAdmin) "Thông báo đã ghim (Admin có thể xóa)" else "Thông báo quan trọng đã được ghim (Không thể xóa)",
                                    color = AccentOrange,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedNotifForDetail = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Đóng", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (isAdmin) {
                    TextButton(
                        onClick = {
                            notificationToDeleteByAdmin = notif
                        }
                    ) {
                        Text("🗑️ Xóa thông báo (Admin)", color = DangerRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (!notif.isPinned) {
                    TextButton(
                        onClick = {
                            viewModel.deleteNotificationLocally(notif.id)
                            selectedNotifForDetail = null
                            Toast.makeText(context, "Đã xóa khỏi danh sách", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Xóa tin nhắn", color = DangerRed, fontSize = 13.sp)
                    }
                }
            },
            containerColor = DarkContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("dialog_notif_detail")
        )
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    badgeColor: Color? = null
) {
    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) PrimaryBlue else DarkContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) PrimaryBlue else CardBorder),
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) White else TextSecondary
            )
            if (badgeColor != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
            }
        }
    }
}

@Composable
private fun NotificationCardItem(
    notif: AdminNotification,
    isRead: Boolean,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onDeleteAsAdmin: () -> Unit
) {
    val context = LocalContext.current
    val notifColor = getNotifColor(notif.type)
    val notifIcon = getNotifIcon(notif.type)

    val formattedTime = remember(notif.createdAt) {
        val diff = System.currentTimeMillis() - notif.createdAt
        when {
            diff < 60 * 1000L -> "Vừa xong"
            diff < 60 * 60 * 1000L -> "${diff / (60 * 1000L)} phút trước"
            diff < 24 * 60 * 60 * 1000L -> "${diff / (60 * 60 * 1000L)} giờ trước"
            else -> SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault()).format(Date(notif.createdAt))
        }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (!isRead) DarkContainer else DarkContainer.copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (!isRead) PrimaryBlue.copy(alpha = 0.6f) else CardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notif_item_${notif.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left icon container
            Box(contentAlignment = Alignment.TopEnd) {
                Surface(
                    shape = CircleShape,
                    color = notifColor.copy(alpha = if (!isRead) 0.22f else 0.12f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = notifIcon,
                            contentDescription = null,
                            tint = if (!isRead) notifColor else notifColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                if (!isRead) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(AccentOrange)
                            .border(1.5.dp, DarkContainer, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main text info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notif.title.ifBlank { "Thông Báo Ban Quản Trị" },
                        fontWeight = if (!isRead) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (!isRead) White else TextPrimary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formattedTime,
                        color = if (!isRead) NeonBlue else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (!isRead) FontWeight.Medium else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notif.message,
                    color = if (!isRead) White.copy(alpha = 0.9f) else TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (notif.isPinned) {
                        Surface(
                            color = AccentOrange.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "📌 ĐÃ GHIM",
                                color = AccentOrange,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Surface(
                        color = notifColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = getNotifTypeName(notif.type),
                            color = notifColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "• Từ: ${notif.sentBy}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Quick delete option or pinned icon
            if (isAdmin) {
                IconButton(
                    onClick = onDeleteAsAdmin,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Xóa thông báo (Admin)",
                        tint = DangerRed.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (notif.isPinned) {
                IconButton(
                    onClick = {
                        Toast.makeText(context, "📌 Thông báo đã ghim bởi Admin, không thể xóa", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Đã ghim",
                        tint = AccentOrange,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Xóa khỏi máy",
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun getNotifColor(type: String): Color {
    return when (type) {
        "SHIFT_REMINDER" -> AccentOrange
        "SHIFT_CHANGE" -> NeonBlue
        "AUTO_TIME_APPROVED" -> AccentGreen
        else -> PrimaryBlue
    }
}

private fun getNotifIcon(type: String): ImageVector {
    return when (type) {
        "SHIFT_REMINDER" -> Icons.Default.Schedule
        "SHIFT_CHANGE" -> Icons.Default.SwapHoriz
        "AUTO_TIME_APPROVED" -> Icons.Default.CheckCircle
        else -> Icons.Default.Notifications
    }
}

private fun getNotifTypeName(type: String): String {
    return when (type) {
        "SHIFT_REMINDER" -> "Nhắc Ca"
        "SHIFT_CHANGE" -> "Đổi Ca"
        "AUTO_TIME_APPROVED" -> "Duyệt Công"
        else -> "Thông Báo Admin"
    }
}
