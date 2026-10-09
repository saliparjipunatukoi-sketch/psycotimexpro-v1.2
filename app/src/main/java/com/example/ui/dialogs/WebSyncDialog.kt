package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.SyncResult
import com.example.ui.theme.*

@Composable
fun WebSyncDialog(
    currentEndpoint: String,
    athletesCount: Int,
    racesCount: Int,
    lastSyncTimestamp: String,
    isSyncing: Boolean,
    syncResult: SyncResult?,
    onDismiss: () -> Unit,
    onTriggerSync: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudSync, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Sync ke Portal Web",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "psycotimexpro.my/training-management",
                        color = ElectricCyan,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = StadiumSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "SASARAN PELAYAN WEB:",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentEndpoint,
                            fontSize = 11.sp,
                            color = ElectricCyan,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Status Terakhir: $lastSyncTimestamp",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = StadiumSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Rekod Atlit", fontSize = 11.sp, color = TextSecondary)
                            Text("$athletesCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Surface(
                        color = StadiumSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Keputusan Race", fontSize = 11.sp, color = TextSecondary)
                            Text("$racesCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LaserOrange)
                        }
                    }
                }

                syncResult?.let { res ->
                    Surface(
                        color = if (res.success) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (res.success) "✓ SYNC SELESAI" else "⚠ AMARAN SYNC",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (res.success) SuccessGreen else FinishLineRed
                            )
                            Text(
                                text = res.message,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onTriggerSync,
                enabled = !isSyncing,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                modifier = Modifier.testTag("start_web_sync_button")
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Menghantar Data...", color = Color.Black, fontSize = 12.sp)
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync Sekarang", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = TextSecondary)
            }
        },
        containerColor = StadiumSurface
    )
}
