package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubUserEntity
import com.example.ui.theme.*

@Composable
fun SubUserScreen(
    subUsers: List<SubUserEntity>,
    currentUserEmail: String,
    isSuperAdmin: Boolean,
    onOpenAddSubUser: () -> Unit,
    onDeleteSubUser: (SubUserEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        floatingActionButton = {
            if (subUsers.size < 10 || isSuperAdmin) {
                FloatingActionButton(
                    onClick = onOpenAddSubUser,
                    containerColor = LaserOrange,
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("fab_add_subuser")
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = "Tambah Sub-User")
                }
            }
        },
        containerColor = TrackDarkNavy,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Column {
                    Text(
                        text = "Pengurusan Sub-User & Pegawai",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Cipta akaun sementara untuk urusetia dan pegawai teknikal (Maksimum 10 username).",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Quota Card (Item 1: maximum 10 username)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kuota Sub-User Urusetia / Teknikal",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Text(
                                text = "${subUsers.size} / 10 Digunakan",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = if (subUsers.size >= 10) FinishLineRed else ElectricCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (subUsers.size / 10f).coerceIn(0f, 1f) },
                            color = if (subUsers.size >= 10) FinishLineRed else ElectricCyan,
                            trackColor = StadiumSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Setiap sub-user mempunyai akses terhad untuk menyusun peserta, mengendalikan Cam 2, atau merekodkan keputusan kejohanan tanpa mendedahkan data kelab lain.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            // Sub-user list
            if (subUsers.isEmpty()) {
                item {
                    Surface(
                        color = StadiumSurface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("Tiada sub-user aktif. Tekan butang '+' untuk menambah pegawai.", color = TextSecondary)
                        }
                    }
                }
            } else {
                items(subUsers) { subUser ->
                    SubUserCard(
                        subUser = subUser,
                        onDelete = { onDeleteSubUser(subUser) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubUserCard(
    subUser: SubUserEntity,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StadiumSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
        modifier = Modifier.fillMaxWidth().testTag("subuser_card_${subUser.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = when (subUser.role) {
                    "Urusetia" -> ElectricCyan.copy(alpha = 0.2f)
                    "Pegawai Teknikal" -> LaserOrange.copy(alpha = 0.2f)
                    else -> SpeedAmber.copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (subUser.role == "Pegawai Teknikal") Icons.Default.CameraEnhance else Icons.Default.Badge,
                        contentDescription = null,
                        tint = if (subUser.role == "Pegawai Teknikal") LaserOrange else ElectricCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subUser.fullName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = StadiumSurfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "@${subUser.username}",
                            fontSize = 10.sp,
                            color = ElectricCyanGlow,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${subUser.role} • PIN: ${subUser.pinCode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "Akaun Induk: ${subUser.parentCoachEmail}",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Padam Sub-user", tint = FinishLineRed)
            }
        }
    }
}
