package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AthleteEntity
import com.example.data.model.ClubProfile
import com.example.data.model.RaceRecordEntity
import com.example.ui.components.PsycotimexBannerLogo
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    clubProfile: ClubProfile,
    athletes: List<AthleteEntity>,
    races: List<RaceRecordEntity>,
    currentUserEmail: String,
    isSuperAdmin: Boolean,
    onNavigateToTiming: () -> Unit,
    onNavigateToAthletes: () -> Unit,
    onNavigateToReplays: () -> Unit,
    onOpenAddAthlete: () -> Unit,
    onOpenKadTeknikal: () -> Unit,
    onOpenWebSync: () -> Unit,
    onSelectAthlete: (AthleteEntity) -> Unit,
    onUpdateClubLogo: (logoUri: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Club logo picker launcher from local storage (Item 12)
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onUpdateClubLogo(uri.toString())
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TrackDarkNavy)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP OFFICIAL PSYCO TIME X PRO BRAND BANNER
        item {
            PsycotimexBannerLogo(
                compact = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("brand_banner_logo")
            )
        }

        // TOP CLUB BANNER WITH LOGO UPLOAD (Item 12)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
                modifier = Modifier.fillMaxWidth().testTag("club_header_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Club Logo (Clickable to upload from local storage)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(StadiumSurfaceVariant)
                            .border(2.dp, ElectricCyan, CircleShape)
                            .clickable {
                                logoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("club_logo_picker_button")
                    ) {
                        if (clubProfile.logoUri != null) {
                            AsyncImage(
                                model = clubProfile.logoUri,
                                contentDescription = "Logo Kelab",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_psycotimex_emblem),
                                contentDescription = "Logo Rasmi Psyco Time X Pro",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = clubProfile.clubName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (isSuperAdmin) LaserOrange.copy(alpha = 0.2f) else ElectricCyan.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isSuperAdmin) "SUPERADMIN" else "JURULATIH UTAMA",
                                    color = if (isSuperAdmin) LaserOrange else ElectricCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentUserEmail,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // QUICK STATS STRIP
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Atlit Berdaftar",
                    value = "${athletes.size}",
                    subText = "${athletes.count { it.category == "Track" }} Balapan / ${athletes.count { it.category == "Field" }} Padang",
                    icon = Icons.Default.DirectionsRun,
                    accentColor = ElectricCyan,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Jumlah Race ET",
                    value = "${races.size}",
                    subText = "Cam 1 & Cam 2",
                    icon = Icons.Default.Timer,
                    accentColor = LaserOrange,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // QUICK ACTION BUTTONS
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Tindakan Pantas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionPill(
                            title = "Mula Timing ET",
                            icon = Icons.Default.PlayArrow,
                            color = ElectricCyan,
                            onClick = onNavigateToTiming,
                            modifier = Modifier.weight(1f)
                        )
                        ActionPill(
                            title = "Daftar Atlit",
                            icon = Icons.Default.PersonAdd,
                            color = LaserOrange,
                            onClick = onOpenAddAthlete,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionPill(
                            title = "Kad Teknikal",
                            icon = Icons.Default.Description,
                            color = SpeedAmber,
                            onClick = onOpenKadTeknikal,
                            modifier = Modifier.weight(1f)
                        )
                        ActionPill(
                            title = "Sync Portal Web",
                            icon = Icons.Default.CloudSync,
                            color = SuccessGreen,
                            onClick = onOpenWebSync,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // SQUAD LIST HEADER WITH "LIHAT SEMUA" (Item 12)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Senarai Pelatih & Profil Atlit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Termasuk gambar, acara dan Personal Best (PB)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                TextButton(onClick = onNavigateToAthletes) {
                    Text("Urus Atlit", color = ElectricCyan, fontWeight = FontWeight.Bold)
                }
            }
        }

        // SQUAD LIST ITEMS WITH ATHLETE PHOTO, NAME, EVENT, AND PB (Item 12)
        if (athletes.isEmpty()) {
            item {
                Surface(
                    color = StadiumSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Tiada atlit berdaftar lagi. Tekan 'Daftar Atlit' untuk bermula.", color = TextSecondary)
                    }
                }
            }
        } else {
            items(athletes) { athlete ->
                AthleteSquadCard(
                    athlete = athlete,
                    onClick = { onSelectAthlete(athlete) }
                )
            }
        }
    }
}

@Composable
private fun AthleteSquadCard(
    athlete: AthleteEntity,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StadiumSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("athlete_item_${athlete.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Athlete Profile Photo from Local Storage (Item 10 & 12)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(StadiumSurfaceVariant)
                    .border(1.5.dp, if (athlete.category == "Track") ElectricCyan else LaserOrange, CircleShape)
            ) {
                if (athlete.photoUri != null) {
                    AsyncImage(
                        model = athlete.photoUri,
                        contentDescription = athlete.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = if (athlete.category == "Track") Icons.Default.DirectionsRun else Icons.Default.AccessibilityNew,
                        contentDescription = null,
                        tint = if (athlete.category == "Track") ElectricCyan else LaserOrange,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name and Category Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = athlete.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = athlete.specificEvent,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(" • ", color = TextMuted)
                    Surface(
                        color = if (athlete.feePaidStatus) SuccessGreen.copy(alpha = 0.15f) else SpeedAmber.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (athlete.feePaidStatus) "Yuran Lunas" else "Tertunggak",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (athlete.feePaidStatus) SuccessGreen else SpeedAmber,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Personal Best (PB) Badge (Item 12 & 9)
            Surface(
                color = if (athlete.category == "Track") ElectricCyan.copy(alpha = 0.15f) else LaserOrange.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (athlete.category == "Track") ElectricCyan.copy(alpha = 0.4f) else LaserOrange.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "PB TERBAIK",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = if (athlete.category == "Track") ElectricCyan else LaserOrange
                    )
                    Text(
                        text = "${athlete.pbValue} ${athlete.pbUnit.take(4)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StadiumSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(subText, fontSize = 10.sp, color = TextMuted)
        }
    }
}

@Composable
private fun ActionPill(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StadiumSurfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
        }
    }
}
