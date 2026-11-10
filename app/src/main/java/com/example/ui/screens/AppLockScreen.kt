package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.*
import com.example.ui.components.PsycotimexCircularLogo
import com.example.ui.theme.*

@Composable
fun AppLockScreen(
    appStatus: RemoteAppStatus,
    currentUserEmail: String,
    userCountry: String = "Malaysia",
    currentLanguage: AppLanguage = AppLanguage.MALAY,
    onLanguageChange: (AppLanguage) -> Unit = {},
    onRefreshCheck: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val langHook = rememberLanguageHook(currentLanguage, onLanguageChange)
    var selectedPricingCountry by remember(userCountry) {
        mutableStateOf(if (SubscriptionPricing.isMalaysia(userCountry)) "Malaysia" else "International")
    }
    val isMyCurrency = selectedPricingCountry == "Malaysia"

    val isKillSwitch = appStatus.killSwitchActive
    val isBlocked = appStatus.userStatus == "blocked"
    val isExpired = appStatus.userStatus == "expired"

    val headerTitle = when {
        isKillSwitch -> langHook.t("system_locked")
        isBlocked -> "AKSES DISEKAT / ACCESS BLOCKED"
        isExpired -> langHook.t("trial_expired")
        else -> langHook.t("pending_approval")
    }

    val statusBadgeColor = when {
        isKillSwitch || isBlocked -> FinishLineRed
        isExpired -> LaserOrange
        else -> SpeedAmber
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030712),
                        Color(0xFF1E1014),
                        Color(0xFF050811)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Language Toggle Bar
            LanguageToggleBar(
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange,
                compact = false,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            PsycotimexCircularLogo(
                sizeDp = 86,
                modifier = Modifier.testTag("app_lock_logo")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Status Badge
            Surface(
                color = statusBadgeColor.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, statusBadgeColor.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isKillSwitch || isBlocked) Icons.Default.Lock else Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = statusBadgeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = headerTitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = statusBadgeColor,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "PSYCO TIME X PRO",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                text = langHook.t("pricing_header"),
                fontSize = 11.sp,
                color = SilverMetallic
            )

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = StadiumSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "GOOGLE ID / EMAIL:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            Text(
                                text = currentUserEmail.ifBlank { "Unverified" },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = appStatus.message.ifBlank {
                                    langHook.t("contact_admin_desc")
                                },
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // CURRENCY SWITCHER (MALAYSIA RM vs INTERNATIONAL USD $)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = if (isMyCurrency) ElectricCyan else StadiumSurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPricingCountry = "Malaysia" }
                        ) {
                            Text(
                                text = "🇲🇾 Malaysia (RM)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isMyCurrency) Color.Black else Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        Surface(
                            color = if (!isMyCurrency) LaserOrange else StadiumSurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPricingCountry = "International" }
                        ) {
                            Text(
                                text = "🌐 International (USD $)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (!isMyCurrency) Color.Black else Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    // 4 DYNAMIC PRICING PACKAGES (RM30->$7, RM40->$10, RM60->$15, RM80->$20)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SubscriptionPricing.plans.forEach { plan ->
                            val priceLabel = plan.formattedPrice(selectedPricingCountry)
                            Surface(
                                color = StadiumSurfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isMyCurrency) ElectricCyan.copy(alpha = 0.35f) else LaserOrange.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val msg = "Salam Admin Psyco Time X Pro (+6019-5326399), saya ingin mengaktifkan pakej $priceLabel (${plan.athletesLimit} Atlit + ${plan.subCoachLimit} Sub Coach) untuk akaun: $currentUserEmail"
                                        val uri = Uri.parse("https://wa.me/${SubscriptionPricing.ADMIN_WHATSAPP_NUMBER}?text=${Uri.encode(msg)}")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${plan.athletesLimit} Atlit + ${plan.subCoachLimit} Sub Coach",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "1 Bulan / 30 Days Active License",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Text(
                                        text = priceLabel,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isMyCurrency) ElectricCyan else LaserOrange
                                    )
                                }
                            }
                        }
                    }

                    // 1. Pay Online via Website (ToyyibPay FPX Malaysia)
                    Button(
                        onClick = {
                            val uri = Uri.parse("https://www.psycotimexpro.my/training-management/index.php#langganan")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LaserOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = langHook.t("pay_online_my"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }

                    // 2. Contact Admin Directly on WhatsApp (+6019-5326399) for International / Subscription / Error Help
                    Button(
                        onClick = {
                            val msg = "Hello Admin Psyco Time X Pro (+6019-5326399), I need assistance with my subscription / account activation.\nEmail: $currentUserEmail\nCountry: $userCountry"
                            val uri = Uri.parse("https://wa.me/${SubscriptionPricing.ADMIN_WHATSAPP_NUMBER}?text=${Uri.encode(msg)}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("lock_contact_admin_btn")
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = langHook.t("contact_admin_whatsapp"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    // 3. Refresh Status Check Button
                    OutlinedButton(
                        onClick = onRefreshCheck,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Semak Semula Status / Refresh", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // 4. Logout / Switch Account
                    TextButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Log Keluar & Tukar Akaun", fontSize = 11.sp, color = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Psyco Time X Pro Sdn. Bhd. • www.psycotimexpro.my\nWhatsApp Admin Support: ${SubscriptionPricing.ADMIN_WHATSAPP_DISPLAY}",
                fontSize = 10.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
