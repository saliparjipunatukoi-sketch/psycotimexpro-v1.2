package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.Athlete
import com.example.data.model.AthleteEntity
import com.example.ui.theme.*

/**
 * Jetpack Compose screen that lists all registered athletes from the Room database,
 * featuring a real-time search bar to filter by name, category filters, quick statistics,
 * and comprehensive athlete performance summaries.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthleteListScreen(
    athletes: List<AthleteEntity>,
    onOpenAddAthlete: () -> Unit,
    onSelectAthlete: (AthleteEntity) -> Unit,
    onDeleteAthlete: ((AthleteEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") } // "All", "Track", "Field"
    var athleteToDelete by remember { mutableStateOf<AthleteEntity?>(null) }

    // Filter athletes by name (matching search query) and category
    val filteredAthletes = remember(athletes, searchQuery, selectedCategoryFilter) {
        athletes.filter { athlete ->
            val matchesName = athlete.name.contains(searchQuery.trim(), ignoreCase = true) ||
                    athlete.specificEvent.contains(searchQuery.trim(), ignoreCase = true)
            val matchesCategory = when (selectedCategoryFilter) {
                "Track" -> athlete.category.equals("Track", ignoreCase = true)
                "Field" -> athlete.category.equals("Field", ignoreCase = true)
                else -> true
            }
            matchesName && matchesCategory
        }
    }

    val trackCount = remember(athletes) { athletes.count { it.category.equals("Track", ignoreCase = true) } }
    val fieldCount = remember(athletes) { athletes.count { it.category.equals("Field", ignoreCase = true) } }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAddAthlete,
                containerColor = ElectricCyan,
                contentColor = Color.Black,
                modifier = Modifier.testTag("fab_add_athlete")
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Daftar Atlit Baharu"
                )
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
            // Header Section
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Senarai Atlit Berdaftar",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "Pangkalan data Room: ${athletes.size} atlit didaftarkan",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        FilledTonalButton(
                            onClick = onOpenAddAthlete,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = StadiumSurfaceVariant,
                                contentColor = ElectricCyan
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_register_athlete_header")
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Daftar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Search Bar Component to filter by name
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Cari nama atlit (cth: Muhd, Danish, Sarah)...",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Ikon Carian",
                            tint = ElectricCyan
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = searchQuery.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.testTag("clear_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Kosongkan Carian",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = StadiumSurface,
                        unfocusedContainerColor = StadiumSurface,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = StadiumBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("athlete_search_bar")
                )
            }

            // Summary Stats Cards (Total, Track, Field)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = "Jumlah Atlit",
                        value = "${athletes.size}",
                        accentColor = ElectricCyan,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Balapan (Track)",
                        value = "$trackCount",
                        accentColor = ElectricCyanGlow,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Padang (Field)",
                        value = "$fieldCount",
                        accentColor = LaserOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Category Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedCategoryFilter == "All",
                        onClick = { selectedCategoryFilter = "All" },
                        label = { Text("Semua (${athletes.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StadiumSurfaceVariant,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("category_chip_all")
                    )

                    FilterChip(
                        selected = selectedCategoryFilter == "Track",
                        onClick = { selectedCategoryFilter = "Track" },
                        label = { Text("Balapan ($trackCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.testTag("category_chip_track")
                    )

                    FilterChip(
                        selected = selectedCategoryFilter == "Field",
                        onClick = { selectedCategoryFilter = "Field" },
                        label = { Text("Padang ($fieldCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LaserOrange,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.testTag("category_chip_field")
                    )
                }
            }

            // Results count banner when searching
            if (searchQuery.isNotBlank()) {
                item {
                    Text(
                        text = "Menunjukkan ${filteredAthletes.size} daripada ${athletes.size} atlit untuk \"$searchQuery\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Athletes List or Empty State
            if (filteredAthletes.isEmpty()) {
                item {
                    Surface(
                        color = StadiumSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, StadiumBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .testTag("empty_athletes_state")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (searchQuery.isNotBlank()) Icons.Default.SearchOff else Icons.Default.GroupAdd,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotBlank())
                                    "Tiada atlit dengan nama \"$searchQuery\""
                                else
                                    "Tiada Atlit Berdaftar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (searchQuery.isNotBlank())
                                    "Sila semak ejaan nama atau tukar kategori tapisan di atas."
                                else
                                    "Pangkalan data Room masih kosong. Daftarkan atlit pertama anda sekarang.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            if (searchQuery.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { searchQuery = "" },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
                                    border = BorderStroke(1.dp, ElectricCyan)
                                ) {
                                    Text("Kosongkan Carian")
                                }
                            } else {
                                Button(
                                    onClick = onOpenAddAthlete,
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black),
                                    modifier = Modifier.testTag("btn_empty_register_athlete")
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Daftar Atlit Baharu", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = filteredAthletes,
                    key = { it.id }
                ) { athlete ->
                    AthleteCardItem(
                        athlete = athlete,
                        onClick = { onSelectAthlete(athlete) },
                        onDeleteClick = if (onDeleteAthlete != null) {
                            { athleteToDelete = athlete }
                        } else null
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    athleteToDelete?.let { targetAthlete ->
        AlertDialog(
            onDismissRequest = { athleteToDelete = null },
            title = {
                Text(
                    text = "Padam Rekod Atlit?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Adakah anda pasti mahu memadam profil '${targetAthlete.name}' dari database Room? Tindakan ini tidak boleh dikembalikan.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAthlete?.invoke(targetAthlete)
                        athleteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed)
                ) {
                    Text("Padam", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { athleteToDelete = null }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            containerColor = StadiumSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun StatMetricCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StadiumSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, StadiumBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
        }
    }
}

@Composable
private fun AthleteCardItem(
    athlete: AthleteEntity,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    val isTrack = athlete.category.equals("Track", ignoreCase = true)
    val accentColor = if (isTrack) ElectricCyan else LaserOrange
    val calculatedAge = remember(athlete.dateOfBirth) {
        try {
            val parts = athlete.dateOfBirth.split("-")
            val birthYear = parts.firstOrNull()?.toIntOrNull() ?: 2008
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            (currentYear - birthYear).coerceIn(6, 60)
        } catch (_: Exception) {
            16
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = StadiumSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, StadiumBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("athlete_item_card_${athlete.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Photo from Local Storage (Photo Picker) or Category Avatar
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(StadiumSurfaceVariant)
                        .border(2.dp, accentColor, CircleShape)
                ) {
                    if (!athlete.photoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = athlete.photoUri,
                            contentDescription = "Foto ${athlete.name}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = if (isTrack) Icons.Default.DirectionsRun else Icons.Default.AccessibilityNew,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Name and Metadata
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = athlete.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${athlete.specificEvent} • Umur $calculatedAge thn • ${athlete.gender}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = athlete.clubName,
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // PB Score Badge
                Surface(
                    color = accentColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isTrack) "PB (SAAT)" else "PB (${athlete.pbUnit})",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor
                        )
                        Text(
                            text = "${athlete.pbValue} ${if (isTrack) "s" else ""}".trim(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = StadiumBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Footer Row: Fee status & Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fee Status Pill
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (athlete.feePaidStatus) Icons.Default.CheckCircle else Icons.Default.Pending,
                        contentDescription = null,
                        tint = if (athlete.feePaidStatus) SuccessGreen else SpeedAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (athlete.feePaidStatus)
                            "Yuran RM%.0f Lunas".format(athlete.registrationFee)
                        else
                            "Yuran RM%.0f Belum Lunas".format(athlete.registrationFee),
                        fontSize = 11.sp,
                        color = if (athlete.feePaidStatus) SuccessGreen else SpeedAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDeleteClick != null) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_delete_athlete_${athlete.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Padam Atlit",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = "Profil ➔",
                        fontSize = 11.sp,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onClick)
                    )
                }
            }
        }
    }
}
