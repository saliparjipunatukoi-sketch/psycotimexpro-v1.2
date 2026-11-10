package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.*

@Composable
fun InfographicManualDialog(
    initialLang: String = "BM", // "BM" or "EN"
    onDismiss: () -> Unit
) {
    var selectedLang by remember { mutableStateOf(initialLang) }
    val isBm = selectedLang == "BM"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = TrackDarkNavy,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, ElectricCyan.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("infographic_manual_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Surface(
                    color = StadiumSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isBm) "POSTER INFOGRAFIK & MANUAL PENGGUNA" else "INFOGRAPHIC POSTER & USER MANUAL",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isBm)
                                            "Panduan Lengkap ET Training, Analisis AI, Profil Atlit & Laporan Kelab"
                                        else
                                            "Complete Guide to ET Training, AI Analysis, Athlete Profile & Club Reports",
                                        fontSize = 10.sp,
                                        color = ElectricCyan
                                    )
                                }
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Language Switcher: Bahasa Melayu vs English
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { selectedLang = "BM" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isBm) ElectricCyan else StadiumSurfaceVariant
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "🇲🇾 Poster Bahasa Melayu",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isBm) Color.Black else Color.White
                                )
                            }
                            Button(
                                onClick = { selectedLang = "EN" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isBm) LaserOrange else StadiumSurfaceVariant
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "🇬🇧 English Infographic",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (!isBm) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }

                // Scrollable Infographic Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Visual Infographic Poster Artwork
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Image(
                                painter = painterResource(
                                    id = if (isBm) R.drawable.img_infographic_manual_bm else R.drawable.img_infographic_manual_en
                                ),
                                contentDescription = if (isBm) "Poster Infografik Bahasa Melayu" else "English Infographic Poster",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            )
                            Surface(
                                color = StadiumSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isBm)
                                        "📌 INFOGRAFIK RASMI PSYCO TIME X PRO — 4 Langkah Utama Pengurusan Latihan & Sains Sukan"
                                    else
                                        "📌 PSYCO TIME X PRO OFFICIAL INFOGRAPHIC — 4 Core Pillars of Electronic Timing & Club Management",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    // Step 1: Club Profile & Athlete Profile Registration
                    InfographicStepCard(
                        stepNumber = "01",
                        accentColor = ElectricCyan,
                        icon = Icons.Default.PersonAdd,
                        title = if (isBm)
                            "Daftar Kelab, Muat Naik Logo & Profil Atlit Bergambar"
                        else
                            "Register Club Logo & Athlete Profile with Photo",
                        bulletPoints = if (isBm) listOf(
                            "Semasa pendaftaran atau di skrin Utama/Tetapan, masukkan Nama Kelab dan klik bulatan Logo Kelab untuk muat naik gambar logo kelab anda.",
                            "Buka tab 'Atlit & PB' ➔ tekan '+ Daftar Atlit' untuk menambah atlit baharu.",
                            "Klik bulatan foto untuk muat naik Gambar Profil Atlit dari galeri telefon, isi Nama, Acara, Rekod PB, dan Yuran.",
                            "Semua profil atlit & gambar akan diselaraskan (Auto-Sync) ke Portal Web secara automatik."
                        ) else listOf(
                            "During registration or on Dashboard/Settings, enter your Club Name and tap the circular Club Logo to upload your team emblem.",
                            "Open 'Athletes & PB' tab ➔ tap '+ Add Athlete' to register a new athlete.",
                            "Tap the photo avatar to upload the Athlete's Profile Picture from your phone gallery, enter Name, Event, PB, and Fee.",
                            "All athlete profiles & photos automatically sync to the Web Portal."
                        )
                    )

                    // Step 2: ET Training & Local Phone Video Storage
                    InfographicStepCard(
                        stepNumber = "02",
                        accentColor = LaserOrange,
                        icon = Icons.Default.Timer,
                        title = if (isBm)
                            "Cara Guna Timing ET (Cam 1/Cam 2) & Simpanan Video Telefon"
                        else
                            "How to Use ET Timing (Cam 1/Cam 2) & Local Phone Video Storage",
                        bulletPoints = if (isBm) listOf(
                            "Buka tab 'Timing ET' dan pilih Mod Kamera: Cam 1 (Jejak Pelari), Cam 2 (Garisan Penamat Torso), atau Mod 1-Telefon 100M.",
                            "Mulakan larian menggunakan butang 'MULA TIMING (GUN)', pengesan bunyi pistol automatik (Audio Gun), atau alat kawalan Bluetooth BLE-M3.",
                            "Apabila dada (Torso) pelari melintasi garisan laser merah, sistem merakam masa tepat (0.001s) dan menjana Photo-Finish Slit.",
                            "PENTING: Semua fail video ET Training disimpan 100% di dalam folder memori telefon anda sendiri (tiada beban ke hosting/drive luar)."
                        ) else listOf(
                            "Open 'Timing ET' tab and select Camera Mode: Cam 1 (Runner Tracker), Cam 2 (Finish Line Torso Gate), or 1-Phone 100M Mode.",
                            "Start the race via 'START TIMING (GUN)' button, automatic Audio Starter Gun detector, or BLE-M3 Bluetooth shutter remote.",
                            "When the athlete's Torso crosses the red laser gate, the system captures millisecond time and generates a Photo-Finish Slit image.",
                            "IMPORTANT: All ET Training videos are saved 100% inside your phone's local memory folder (never uploaded to external hosting)."
                        )
                    )

                    // Step 3: AI Sports Science Biomechanics & Auto Warm-Up/Training
                    InfographicStepCard(
                        stepNumber = "03",
                        accentColor = SpeedAmber,
                        icon = Icons.Default.Science,
                        title = if (isBm)
                            "Analisis AI Sains Sukan & Auto-Generate Sesi Warm-Up / Latihan"
                        else
                            "AI Sports Science Analysis & Auto-Generated Warm-Up / Training",
                        bulletPoints = if (isBm) listOf(
                            "Selepas setiap larian atau lompatan (Lompat AI), enjin AI mengira Cadence (spm), Masa Sentuhan Tanah (GCT ms), Indeks Kekuatan Reaktif (RSI), Kuasa Mekanikal (W/kg), dan Sudut Condongan Torso.",
                            "Buka tab 'Sains AI' untuk melihat cadangan senaman Warm-Up (Protokol RAMP) dan Latihan Utama yang dijana secara automatik & sentiasa terkini (Up-To-Date) oleh AI.",
                            "Tekan 'Apply Ke Semua Atlit' atau 'Kongsi Pek Latihan Rumah' untuk menghantar jadual latihan & pautan tutorial YouTube ke WhatsApp atlit."
                        ) else listOf(
                            "After every sprint or jump (Sand Jump AI), the AI engine calculates Cadence (spm), Ground Contact Time (GCT ms), Reactive Strength Index (RSI), Mechanical Power (W/kg), and Torso Lean Angle.",
                            "Open 'Sports AI' tab to view automatically generated, always up-to-date RAMP Warm-Up and Biomechanical Training sessions.",
                            "Tap 'Apply to All Athletes' or 'Share Home Training Pack' to send the workout schedule & YouTube drill tutorials via WhatsApp."
                        )
                    )

                    // Step 4: 1-Month Performance by Date/Time, Lifetime Fee Report & PDF Export
                    InfographicStepCard(
                        stepNumber = "04",
                        accentColor = SuccessGreen,
                        icon = Icons.Default.PictureAsPdf,
                        title = if (isBm)
                            "Laporan Prestasi Sebulan (Tarikh & Masa), Yuran & Eksport PDF"
                        else
                            "1-Month Performance (Date & Time), Lifetime Fee History & PDF Export",
                        bulletPoints = if (isBm) listOf(
                            "Klik mana-mana kad atlit untuk membuka 'Profil Lengkap Atlit'.",
                            "Tab 'Prestasi Sebulan': Lihat semua catatan masa & biomekanik atlit selama 30 hari lengkap mengikut Tarikh dan Masa tepat.",
                            "Tab 'Rekod Yuran': Semak semua sejarah pembayaran yuran bulanan/pendaftaran sejak hari pertama atlit didaftarkan ke dalam sistem.",
                            "Tekan butang 'Muat Turun Laporan Bulanan PDF' untuk menjana fail PDF rasmi yang sedia dicetak atau dikongsi ke ibu bapa & pihak pengurusan."
                        ) else listOf(
                            "Tap any athlete card to open the 'Complete Athlete Profile'.",
                            "Tab '1-Month Performance': View all sprint/field results and biomechanical metrics over the past 30 days sorted by exact Date & Time.",
                            "Tab 'Fee History': Track every fee payment & pending balance since the exact date the athlete was registered in the system.",
                            "Tap 'Download Monthly PDF Report' to generate an official printable PDF report for parents, schools, or club management."
                        )
                    )
                }

                // Bottom Bar
                Surface(
                    color = StadiumSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (isBm) "Faham & Tutup Manual" else "Got It & Close Manual",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfographicStepCard(
    stepNumber: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    bulletPoints: List<String>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StadiumSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = accentColor.copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stepNumber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }

            bulletPoints.forEach { point ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "• ",
                        color = accentColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                    Text(
                        text = point,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
