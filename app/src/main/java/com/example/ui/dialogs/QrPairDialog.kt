package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.QrCodeVisualizer
import com.example.ui.theme.*

@Composable
fun QrPairDialog(
    sessionPayload: String,
    onDismiss: () -> Unit,
    onSwitchToCam2Mode: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Link Phone Backup Sebagai Cam 2",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Finisher Timing Gate & AI Torso Capture",
                    color = LaserOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Imbas QR ini menggunakan Telefon Ke-2 (Backup) untuk membuka Cam 2 Finisher Torso Gate melalui pautan web www.psycotimexpro.my/training-management/cam2 atau sambungan terus APK.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                // Render dynamic QR Code matrix
                QrCodeVisualizer(
                    payload = sessionPayload,
                    sizeDp = 180
                )

                Surface(
                    color = StadiumSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "URL CAM 2: www.psycotimexpro.my/training-management",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "KOD SESI: ${sessionPayload.takeLast(16)}",
                            fontSize = 10.sp,
                            color = SpeedAmber
                        )
                        Text(
                            text = "Status: Bersedia Menerima Sambungan Torso Sensor",
                            fontSize = 11.sp,
                            color = SuccessGreen
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSwitchToCam2Mode()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = LaserOrange),
                modifier = Modifier.testTag("activate_cam2_mode_button")
            ) {
                Text("Gunakan Telefon Ini Sebagai Cam 2", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
