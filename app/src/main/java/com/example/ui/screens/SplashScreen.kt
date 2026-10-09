package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PsycotimexBannerLogo
import com.example.ui.components.PsycotimexCircularLogo
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onDismissSplash: () -> Unit,
    modifier: Modifier = Modifier
) {
    var loadingStatusText by remember { mutableStateOf("MEMULAKAN SISTEM ELECTRONIC TIMING...") }
    var progressVal by remember { mutableStateOf(0.15f) }

    // Entrance Animation
    val scaleAnim = remember { Animatable(0.7f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Entrance zoom and fade in
        scaleAnim.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
        alphaAnim.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(600)
        )

        // Staged initialization messages
        delay(600)
        progressVal = 0.45f
        loadingStatusText = "MEMUATKAN PANGKALAN DATA ATLIT & PB..."

        delay(700)
        progressVal = 0.80f
        loadingStatusText = "PENGESANAN TORSO & SYNC DWI-KAMERA SIAP..."

        delay(700)
        progressVal = 1.0f
        loadingStatusText = "SISTEM PSYCO TIME X PRO BERSEDIA!"

        delay(600)
        onDismissSplash()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030712),
                        Color(0xFF0F172A),
                        Color(0xFF020617)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .scale(scaleAnim.value)
                .alpha(alphaAnim.value)
        ) {
            // Main Circular Emblem Logo
            PsycotimexCircularLogo(
                sizeDp = 180,
                modifier = Modifier.testTag("splash_emblem_logo")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Wide Banner Logo with Metallic Plate & Slogans
            PsycotimexBannerLogo(
                compact = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            // High-Tech Loading Indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 380.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progressVal },
                    color = Color(0xFFDC2626), // Brand Red
                    trackColor = StadiumSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = SpeedAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = loadingStatusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Direct Continue / Skip Button
            OutlinedButton(
                onClick = onDismissSplash,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("skip_splash_button")
            ) {
                Text("Mula Sekarang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }

        // Bottom Brand Copyright
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "ELECTRONIC TIMING TRAINING & SPORTS MANAGEMENT",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
            Text(
                text = "psycotimexpro.my",
                fontSize = 10.sp,
                color = ElectricCyanGlow,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
