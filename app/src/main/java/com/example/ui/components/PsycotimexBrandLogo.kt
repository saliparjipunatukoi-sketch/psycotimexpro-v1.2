package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

/**
 * High-fidelity representation of the official Psyco Time X Pro Wide Banner Logo
 * ("YOUR BEST LOCAL CHOICE FOR SPORT TIMING EQUIPMENT" - "PRECISE • SHARP • ACCURATE")
 */
@Composable
fun PsycotimexBannerLogo(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Surface(
        color = Color(0xFF1E2430),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Brush.linearGradient(
                colors = listOf(Color(0xFF6B7280), Color(0xFF1F2937), Color(0xFF4B5563))
            )
        ),
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2B3342),
                            Color(0xFF1A202C),
                            Color(0xFF111722)
                        )
                    )
                )
                .padding(horizontal = if (compact) 10.dp else 16.dp, vertical = if (compact) 6.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Branding Row: [PSYCO TIME] [ GIANT RED X + LIGHTNING EMBLEM ] [ PRO ]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Stack of "PSYCO" over "TIME" in Digital 7-Segment typography
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "PSYCO",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = if (compact) 14.sp else 22.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "TIME",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = if (compact) 14.sp else 22.sp,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.width(if (compact) 6.dp else 10.dp))

                // Iconic Crimson Red Brushed Metal "X"
                Text(
                    text = "X",
                    color = Color(0xFFDC2626),
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    fontSize = if (compact) 32.sp else 48.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                Spacer(modifier = Modifier.width(if (compact) 4.dp else 6.dp))

                // Silver Circle with Red Lightning Bolt & White Bold "PRO"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Lightning bolt icon container
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(if (compact) 26.dp else 36.dp)
                            .border(1.5.dp, Color(0xFFCBD5E1), CircleShape)
                            .background(Color(0xFF0F172A), CircleShape)
                    ) {
                        Canvas(modifier = Modifier.size(if (compact) 16.dp else 22.dp)) {
                            val w = size.width
                            val h = size.height
                            val boltPath = Path().apply {
                                moveTo(w * 0.65f, 0f)
                                lineTo(w * 0.15f, h * 0.55f)
                                lineTo(w * 0.50f, h * 0.55f)
                                lineTo(w * 0.35f, h)
                                lineTo(w * 0.95f, h * 0.40f)
                                lineTo(w * 0.58f, h * 0.40f)
                                close()
                            }
                            drawPath(boltPath, color = Color(0xFFDC2626))
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "PRO",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = if (compact) 20.sp else 30.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            if (!compact) {
                Spacer(modifier = Modifier.height(6.dp))
                // Slogans row in gold lettering
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "YOUR BEST LOCAL CHOICE FOR SPORT TIMING EQUIPMENT",
                        color = Color(0xFFEAB308),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "PRECISE • SHARP • ACCURATE",
                        color = Color(0xFFFBBF24),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Animated Circular Emblem Logo of Psyco Time X Pro
 */
@Composable
fun PsycotimexCircularLogo(
    modifier: Modifier = Modifier,
    sizeDp: Int = 160
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size((sizeDp + 20).dp)
    ) {
        // Outer Glowing Red/Cyan ring
        Canvas(modifier = Modifier.size((sizeDp + 16).dp)) {
            drawCircle(
                color = Color(0xFFDC2626).copy(alpha = glowAlpha * 0.3f),
                radius = size.width / 2f
            )
        }

        // Circular Emblem Image from Vector Drawable
        Image(
            painter = painterResource(id = R.drawable.ic_psycotimex_emblem),
            contentDescription = "Psyco Time X Pro Logo",
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .shadow(12.dp, CircleShape)
        )
    }
}
