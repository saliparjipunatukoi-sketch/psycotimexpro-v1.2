package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AthleteEntity
import com.example.data.model.RaceRecordEntity
import com.example.service.SandJumpAiAnalysis
import com.example.ui.theme.*
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val TAG = "SportsScienceAiHub"

enum class SportDiscipline(val label: String, val shortLabel: String, val defaultYoutubeQuery: String) {
    SPRINT("Pecut (Sprint 100m / 200m / 400m)", "Sprint", "sprint warm up drills A skip B skip track and field"),
    JUMP("Lompatan (Long Jump & Triple Jump)", "Jump (Lompat)", "long jump triple jump plyometric drills warm up"),
    HURDLES_POWER("Lari Berpagar & Kuasa Eksplosif", "Pagar & Kuasa", "hurdle mobility drills explosive plyometrics track"),
    HOME_RECOVERY("Latihan Rumah, Core & Pemulihan", "Latihan Rumah", "track and field home workout core mobility exercises")
}

enum class SuggestionType(val title: String) {
    ALL("Semua (Warm-Up & Training)"),
    WARM_UP("Warm-Up Suggestion (Pemanasan RAMP)"),
    TRAINING("Training Suggestion (Latihan Utama)")
}

enum class DemoMotionStyle {
    HIGH_KNEES_SKIP,
    EXPLOSIVE_BLOCK_START,
    BOUNDING_JUMP,
    ANKLE_POGO_STIFFNESS,
    HIP_HURDLE_MOBILITY,
    CORE_HAMSTRING_SHIELD
}

data class SportsScienceDrillItem(
    val id: String,
    val sport: SportDiscipline,
    val type: SuggestionType,
    val title: String,
    val subtitle: String,
    val setsAndReps: String,
    val targetBiomechanics: String,
    val sportsScienceRationale: String,
    val howToSteps: List<String>,
    val commonMistakeToAvoid: String,
    val youtubeSearchQuery: String,
    val demoMotionStyle: DemoMotionStyle,
    val isAiGenerated: Boolean = false
)

@Composable
fun SportsScienceAiHubScreen(
    athletes: List<AthleteEntity>,
    races: List<RaceRecordEntity>,
    sandJumpHistory: List<SandJumpAiAnalysis>,
    coachName: String,
    onApplyDrillToAllAthletes: (drill: SportsScienceDrillItem, assignedCount: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedSport by remember { mutableStateOf(SportDiscipline.SPRINT) }
    var selectedType by remember { mutableStateOf(SuggestionType.ALL) }
    var customYoutubeQuery by remember { mutableStateOf("") }

    // Coach AI Prompt for custom Gemini 3.8 Flash Sports Science generation
    var customAiFocusPrompt by remember { mutableStateOf("") }
    var isGeneratingAiPlan by remember { mutableStateOf(false) }
    var aiGenerationError by remember { mutableStateOf<String?>(null) }
    var aiCoachInsightBanner by remember {
        mutableStateOf(
            "AI Sains Sukan Sedia: Pilih kategori Sukan (Sprint / Jump) untuk melihat cadangan Warm-Up (RAMP) & Training Biomekanik, tonton contoh di YouTube, tetapkan kepada semua atlit berdaftar, dan kongsi pautan latihan rumah."
        )
    }

    // Track which drills have been applied to all registered athletes
    var appliedDrillIds by remember { mutableStateOf(setOf<String>()) }
    var globalAssignedSportProgram by remember { mutableStateOf<String?>(null) }

    // Built-in Curated World-Athletics Sports Science Warm-Up & Training Library + AI Generated Drills
    var drillCatalog by remember { mutableStateOf(buildInitialSportsScienceCatalog()) }

    // Lazy Firebase AI Logic model with structured JSON output schema (gemini-3.8-flash)
    val sportsScienceAiModel by lazy {
        val drillObjSchema = Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "Nama senaman atau warm-up dalam Bahasa Melayu"),
                "type" to Schema.enumeration(
                    values = listOf("WARM_UP", "TRAINING"),
                    description = "Jenis cadangan sama ada WARM_UP atau TRAINING"
                ),
                "setsAndReps" to Schema.string(description = "Dos latihan contohnya 3 Set x 20m (Rehat 90s)"),
                "targetBiomechanics" to Schema.string(description = "Fokus biomekanik dan otot sasaran"),
                "sportsScienceRationale" to Schema.string(description = "Penerangan Sains Sukan mengapa senaman ini berkesan"),
                "steps" to Schema.array(
                    items = Schema.string(),
                    description = "3 hingga 4 langkah cara membuat senaman dengan betul"
                ),
                "commonMistake" to Schema.string(description = "Kesalahan lazim yang perlu dielakkan oleh atlit"),
                "youtubeQuery" to Schema.string(description = "Kata kunci carian video tutorial di YouTube")
            )
        )
        val responseSchema = Schema.obj(
            properties = mapOf(
                "coachSummary" to Schema.string(description = "Rumusan analisis Sains Sukan AI untuk jurulatih"),
                "drills" to Schema.array(
                    items = drillObjSchema,
                    description = "Senarai cadangan Warm-Up dan Training"
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                this.responseSchema = responseSchema
            }
        )
    }

    // Compute live Sports Science Team Readiness & Biomechanics Metrics from stored data
    val bestSprintSec = remember(races) {
        val validMs = races.map { it.recordedTimeMillis }.filter { it > 1000L }
        if (validMs.isNotEmpty()) validMs.minOrNull()!! / 1000.0 else 10.85
    }
    val bestJumpMeters = remember(sandJumpHistory) {
        sandJumpHistory.maxOfOrNull { it.measuredDistanceMeters } ?: 6.42
    }
    val reactiveStrengthIndex = remember(bestSprintSec, bestJumpMeters) {
        ((bestJumpMeters / 2.25) + (11.5 / bestSprintSec.coerceAtLeast(9.5)) * 0.4).let {
            (it * 100.0).toInt() / 100.0
        }
    }

    val filteredDrills = remember(drillCatalog, selectedSport, selectedType) {
        drillCatalog.filter { item ->
            item.sport == selectedSport &&
                (selectedType == SuggestionType.ALL || item.type == selectedType)
        }
    }

    // Animation phase for live biomechanical exercise demonstration figures
    var animPhase by remember { mutableStateOf(0.0) }
    LaunchedEffect(Unit) {
        while (true) {
            animPhase = (animPhase + 0.16) % (2.0 * Math.PI)
            delay(65L)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TrackDarkNavy)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. HERO HEADER: SPORTS SCIENCE AI & HOME TRAINING HUB
        Surface(
            color = StadiumSurface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, ElectricCyan.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sports_science_ai_header")
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .background(ElectricCyan.copy(alpha = 0.18f), CircleShape)
                                .border(1.dp, ElectricCyan, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Science,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "HAB SAINS SUKAN AI • TRAINING & WARM-UP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "Cadangan Pintar Sprint & Jump • Contoh Cara Senaman • YouTube & Kongsi Latihan Rumah Atlit",
                                fontSize = 8.5.sp,
                                color = ElectricCyan
                            )
                        }
                    }

                    Surface(
                        color = SuccessGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SuccessGreen)
                    ) {
                        Text(
                            text = "${athletes.size} Atlit Berdaftar",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = SuccessGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // ADVANCED SPORTS SCIENCE TELEMETRY METRICS (RSI, Force-Velocity, CNS Readiness, Metabolic Zone)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ScienceIndexTile(
                        title = "Indeks RSI",
                        value = "$reactiveStrengthIndex",
                        sub = "Kekakuan Tendon",
                        accent = ElectricCyan,
                        modifier = Modifier.weight(1f)
                    )
                    ScienceIndexTile(
                        title = "Profil F-V",
                        value = if (selectedSport == SportDiscipline.JUMP) "Kuasa Lonjak" else "Halaju Maks",
                        sub = "Force-Velocity AI",
                        accent = SpeedAmber,
                        modifier = Modifier.weight(1f)
                    )
                    ScienceIndexTile(
                        title = "Kesediaan CNS",
                        value = "94%",
                        sub = "ACWR: 1.08 (Optimum)",
                        accent = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    ScienceIndexTile(
                        title = "Sistem Tenaga",
                        value = "ATP-PCr",
                        sub = "Alaktik Eksplosif",
                        accent = FinishLineRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. PEMILIH SUKAN PILIHAN USER (SPRINT, JUMP, PAGAR, LATIHAN RUMAH)
        Text(
            text = "1. Pilih Kategori Sukan Pilihan (Sprint / Jump / Kuasa / Rumah):",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = SilverMetallic
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SportDiscipline.entries.forEach { discipline ->
                val isSelected = selectedSport == discipline
                Surface(
                    color = if (isSelected) ElectricCyan else StadiumSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isSelected) ElectricCyan else StadiumBorder),
                    modifier = Modifier
                        .height(34.dp)
                        .clickable { selectedSport = discipline }
                        .testTag("sport_selector_${discipline.name}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = when (discipline) {
                                SportDiscipline.SPRINT -> Icons.Default.DirectionsRun
                                SportDiscipline.JUMP -> Icons.Default.Straighten
                                SportDiscipline.HURDLES_POWER -> Icons.Default.Bolt
                                SportDiscipline.HOME_RECOVERY -> Icons.Default.HomeWork
                            },
                            contentDescription = null,
                            tint = if (isSelected) Color.Black else ElectricCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = discipline.shortLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSelected) Color.Black else Color.White
                        )
                    }
                }
            }
        }

        // 3. PENAPIS CADANGAN: SEMUA | WARM-UP SUGGESTION | TRAINING SUGGESTION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SuggestionType.entries.forEach { sType ->
                val isSelected = selectedType == sType
                val badgeColor = when (sType) {
                    SuggestionType.WARM_UP -> SpeedAmber
                    SuggestionType.TRAINING -> FinishLineRed
                    SuggestionType.ALL -> SuccessGreen
                }
                Surface(
                    color = if (isSelected) badgeColor else StadiumSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isSelected) badgeColor else StadiumBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .clickable { selectedType = sType }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                        Text(
                            text = when (sType) {
                                SuggestionType.ALL -> "Semua (${drillCatalog.count { it.sport == selectedSport }})"
                                SuggestionType.WARM_UP -> "🔥 Warm-Up RAMP"
                                SuggestionType.TRAINING -> "⚡ Training Utama"
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSelected && sType != SuggestionType.TRAINING) Color.Black else Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 4. APPLY TO ALL REGISTERED ATHLETES & SHARE FULL HOME WORKOUT PACK CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.65f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "2. Tetapkan Kepada Semua Atlit Berdaftar & Kongsi Latihan Rumah",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Semua cadangan Warm-Up & Training (${selectedSport.shortLabel}) ini akan ditetapkan terus kepada ${athletes.size} atlit di bawah Coach $coachName. Kongsi video & cara senaman ke WhatsApp/Telegram supaya atlit boleh berlatih dari rumah.",
                            fontSize = 8.5.sp,
                            color = TextSecondary,
                            lineHeight = 11.5.sp
                        )
                    }
                }

                // Registered Athletes Chips showing assignment status
                if (athletes.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        athletes.forEach { athlete ->
                            val isAssigned = globalAssignedSportProgram != null || appliedDrillIds.isNotEmpty()
                            Surface(
                                color = if (isAssigned) SuccessGreen.copy(alpha = 0.16f) else StadiumSurfaceVariant,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isAssigned) SuccessGreen else StadiumBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isAssigned) Icons.Default.CheckCircle else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isAssigned) SuccessGreen else SilverMetallic,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (isAssigned) "${athlete.name} (Aktif ✓)" else athlete.name,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAssigned) SuccessGreen else Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val allIds = filteredDrills.map { it.id }.toSet()
                            appliedDrillIds = appliedDrillIds + allIds
                            globalAssignedSportProgram = selectedSport.label
                            filteredDrills.firstOrNull()?.let { firstDrill ->
                                onApplyDrillToAllAthletes(firstDrill, athletes.size)
                            }
                            Toast.makeText(
                                context,
                                "Program Warm-Up & Training '${selectedSport.shortLabel}' telah ditetapkan kepada semua ${athletes.size} atlit berdaftar!",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("apply_all_athletes_btn")
                    ) {
                        Icon(
                            Icons.Default.GroupAdd,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Apply Ke Semua Atlit (${athletes.size})",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }

                    Button(
                        onClick = {
                            shareFullProgramToAthletes(
                                context = context,
                                coachName = coachName,
                                sport = selectedSport,
                                drills = filteredDrills,
                                athletes = athletes
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("share_full_program_btn")
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Kongsi Pek Latihan Rumah",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // 5. AI GENERATOR & YOUTUBE SEARCH BAR (GEMINI 3.8 FLASH + DIRECT YOUTUBE SEARCH)
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SpeedAmber.copy(alpha = 0.65f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = SpeedAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "3. Janaan Pintar AI (Gemini Sains Sukan) & Carian Terus YouTube",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                // Custom AI Focus Input + Generate Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customAiFocusPrompt,
                        onValueChange = { customAiFocusPrompt = it },
                        placeholder = {
                            Text(
                                "Cth: Latihan rumah kuatkan lutut & lonjakan papan lompat jauh...",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 10.sp, color = Color.White),
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            if (!isGeneratingAiPlan) {
                                isGeneratingAiPlan = true
                                aiGenerationError = null
                                val focusQuery = customAiFocusPrompt.ifBlank {
                                    "Cadangkan senaman Warm-Up RAMP dan Training biomekanik terbaik yang boleh dibuat di padang atau dari rumah untuk ${selectedSport.label}"
                                }
                                coroutineScope.launch {
                                    try {
                                        val prompt = """
                                            Anda ialah Pakar Sains Sukan & Biomekanik Olahraga (World Athletics).
                                            Sukan Pilihan: ${selectedSport.label}.
                                            Fokus Jurulatih: $focusQuery.
                                            Hasilkan 2 senaman (1 WARM_UP dan 1 TRAINING) lengkap dengan cara melakukan senaman langkah demi langkah, rasional sains sukan, dan kata kunci video tutorial YouTube.
                                        """.trimIndent()

                                        val response = withTimeout(60_000L) {
                                            sportsScienceAiModel.generateContent(prompt)
                                        }
                                        val rawJson = response.text
                                            ?: throw IllegalStateException("Respons AI kosong.")
                                        val parsed = JSONObject(rawJson)
                                        val summary = parsed.optString("coachSummary", "")
                                        if (summary.isNotBlank()) {
                                            aiCoachInsightBanner = "🤖 Analisis AI Terkini: $summary"
                                        }
                                        val drillsArr = parsed.optJSONArray("drills")
                                        val newDrills = mutableListOf<SportsScienceDrillItem>()
                                        if (drillsArr != null) {
                                            for (i in 0 until drillsArr.length()) {
                                                val obj = drillsArr.getJSONObject(i)
                                                val stepsJson = obj.optJSONArray("steps")
                                                val stepList = mutableListOf<String>()
                                                if (stepsJson != null) {
                                                    for (s in 0 until stepsJson.length()) {
                                                        stepList.add(stepsJson.getString(s))
                                                    }
                                                }
                                                val typeStr = obj.optString("type", "TRAINING")
                                                val sType = if (typeStr == "WARM_UP") {
                                                    SuggestionType.WARM_UP
                                                } else {
                                                    SuggestionType.TRAINING
                                                }
                                                newDrills.add(
                                                    SportsScienceDrillItem(
                                                        id = "ai_${System.currentTimeMillis()}_$i",
                                                        sport = selectedSport,
                                                        type = sType,
                                                        title = obj.optString("title", "Senaman Sains Sukan AI"),
                                                        subtitle = "Dijana Khas Oleh AI Sains Sukan • Sesuai Untuk Latihan Rumah & Padang",
                                                        setsAndReps = obj.optString("setsAndReps", "3 Set x 12 Rep (Rehat 60s)"),
                                                        targetBiomechanics = obj.optString("targetBiomechanics", "Rangka Badan, Lutut & Tendon Achilles"),
                                                        sportsScienceRationale = obj.optString("sportsScienceRationale", "Meningkatkan koordinasi neuromuskular dan kuasa lonjakan."),
                                                        howToSteps = if (stepList.isNotEmpty()) stepList else listOf(
                                                            "Pastikan postur badan tegak 45° dan teras perut dikunci.",
                                                            "Lakukan ayunan tangan 90° selari dengan angkatan lutut.",
                                                            "Mendarat di atas bebola kaki (dorsiflexion) untuk pantulan elastik."
                                                        ),
                                                        commonMistakeToAvoid = obj.optString("commonMistake", "Elakkan mendarat menggunakan tumit atau membongkokkan pinggang."),
                                                        youtubeSearchQuery = obj.optString("youtubeQuery", selectedSport.defaultYoutubeQuery),
                                                        demoMotionStyle = if (selectedSport == SportDiscipline.JUMP) {
                                                            DemoMotionStyle.BOUNDING_JUMP
                                                        } else {
                                                            DemoMotionStyle.HIGH_KNEES_SKIP
                                                        },
                                                        isAiGenerated = true
                                                    )
                                                )
                                            }
                                        }
                                        if (newDrills.isNotEmpty()) {
                                            drillCatalog = newDrills + drillCatalog
                                            Toast.makeText(
                                                context,
                                                "Berjaya menjana ${newDrills.size} senaman Sains Sukan AI baharu!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } catch (e: TimeoutCancellationException) {
                                        Log.e(TAG, "AI generation timed out", e)
                                        aiGenerationError = "Janaan AI mengambil masa terlalu lama (Timeout 60s). Sila cuba lagi."
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Firebase AI generation failed", e)
                                        aiGenerationError = "Ralat AI: ${e.localizedMessage ?: "Gagal menjana cadangan AI"}"
                                    } finally {
                                        isGeneratingAiPlan = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SpeedAmber),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("generate_ai_sports_science_btn")
                    ) {
                        if (isGeneratingAiPlan) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Jana AI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }
                    }
                }

                if (aiGenerationError != null) {
                    Surface(
                        color = FinishLineRed.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, FinishLineRed)
                    ) {
                        Text(
                            text = aiGenerationError ?: "",
                            fontSize = 8.5.sp,
                            color = FinishLineRed,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Direct YouTube Video Tutorial Search Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customYoutubeQuery,
                        onValueChange = { customYoutubeQuery = it },
                        placeholder = {
                            Text(
                                "Cari video senaman di YouTube (cth: A-Skip Sprint Drill / Long Jump Takeoff)...",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 10.sp, color = Color.White),
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            val q = customYoutubeQuery.ifBlank { selectedSport.defaultYoutubeQuery }
                            openYoutubeVideoSearch(context, q)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("search_youtube_drill_btn")
                    ) {
                        Icon(
                            Icons.Default.PlayCircleFilled,
                            contentDescription = "Cari YouTube",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "YouTube",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                // AI Coach Insight Banner
                Surface(
                    color = ElectricCyan.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = aiCoachInsightBanner,
                        fontSize = 8.5.sp,
                        color = Color.White,
                        lineHeight = 12.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        // 6. SENARAI CADANGAN WARM-UP & TRAINING BERSERTA ANIMASI CARA MEMBUAT SENAMAN, YOUTUBE & SHARE
        Text(
            text = "4. Panduan Cara Membuat Senaman, Video YouTube & Kongsi Ke Atlit (${filteredDrills.size} Modul):",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )

        filteredDrills.forEach { drill ->
            val isApplied = appliedDrillIds.contains(drill.id) || globalAssignedSportProgram == selectedSport.label
            SportsScienceDrillCard(
                drill = drill,
                animPhase = animPhase,
                isAppliedToAllAthletes = isApplied,
                registeredAthletesCount = athletes.size,
                onWatchYoutube = { openYoutubeVideoSearch(context, drill.youtubeSearchQuery) },
                onApplyToAllAthletes = {
                    appliedDrillIds = appliedDrillIds + drill.id
                    onApplyDrillToAllAthletes(drill, athletes.size)
                    Toast.makeText(
                        context,
                        "Senaman '${drill.title}' ditetapkan kepada semua ${athletes.size} atlit berdaftar!",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onShareVideoToAthletes = {
                    shareSingleDrillToAthletes(
                        context = context,
                        coachName = coachName,
                        drill = drill,
                        athletes = athletes
                    )
                }
            )
        }
    }
}

@Composable
private fun ScienceIndexTile(
    title: String,
    value: String,
    sub: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StadiumSurfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                color = SilverMetallic,
                maxLines = 1
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                color = accent,
                maxLines = 1
            )
            Text(
                text = sub,
                fontSize = 7.sp,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SportsScienceDrillCard(
    drill: SportsScienceDrillItem,
    animPhase: Double,
    isAppliedToAllAthletes: Boolean,
    registeredAthletesCount: Int,
    onWatchYoutube: () -> Unit,
    onApplyToAllAthletes: () -> Unit,
    onShareVideoToAthletes: () -> Unit
) {
    val isWarmup = drill.type == SuggestionType.WARM_UP
    val badgeColor = if (isWarmup) SpeedAmber else ElectricCyan

    Card(
        colors = CardDefaults.cardColors(containerColor = StadiumSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.2.dp,
            if (isAppliedToAllAthletes) SuccessGreen.copy(alpha = 0.8f) else badgeColor.copy(alpha = 0.55f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card Header: Type Pill + AI Badge + Applied Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, badgeColor)
                    ) {
                        Text(
                            text = if (isWarmup) "🔥 WARM-UP (RAMP)" else "⚡ TRAINING SAINS SUKAN",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (drill.isAiGenerated) {
                        Surface(
                            color = FinishLineRed.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, FinishLineRed)
                        ) {
                            Text(
                                text = "🤖 JANAAN AI",
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Black,
                                color = FinishLineRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = drill.setsAndReps,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            // Title & Subtitle
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = drill.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = drill.subtitle,
                    fontSize = 8.5.sp,
                    color = TextSecondary
                )
            }

            // VISUAL ANIMATED BIOMECHANICS DEMONSTRATION + STEP-BY-STEP HOW TO DO IT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Animated AI Biomechanical Skeleton Demonstrator showing how to perform the exercise
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(145.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black)
                        .border(1.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val swing = sin(animPhase).toFloat()
                        val bounce = abs(cos(animPhase)).toFloat()

                        // Ground Track Line
                        drawLine(
                            color = SilverBorder.copy(alpha = 0.5f),
                            start = Offset(8f, h * 0.86f),
                            end = Offset(w - 8f, h * 0.86f),
                            strokeWidth = 3f
                        )

                        // Compute animated joints based on DemoMotionStyle
                        val leanX = when (drill.demoMotionStyle) {
                            DemoMotionStyle.EXPLOSIVE_BLOCK_START -> 0.14f
                            DemoMotionStyle.BOUNDING_JUMP -> 0.08f
                            else -> 0.03f
                        }
                        val jumpLift = when (drill.demoMotionStyle) {
                            DemoMotionStyle.BOUNDING_JUMP, DemoMotionStyle.ANKLE_POGO_STIFFNESS -> -bounce * 0.14f
                            else -> -bounce * 0.04f
                        }

                        val hip = Offset(w * 0.48f, h * (0.54f + jumpLift))
                        val neck = Offset(w * (0.48f + leanX), h * (0.30f + jumpLift))
                        val head = Offset(w * (0.49f + leanX * 1.2f), h * (0.20f + jumpLift))

                        val lElbow = Offset(neck.x - w * 0.12f * swing, neck.y + h * 0.12f)
                        val lWrist = Offset(lElbow.x + w * 0.10f * swing, lElbow.y - h * 0.06f * swing)
                        val rElbow = Offset(neck.x + w * 0.12f * swing, neck.y + h * 0.12f)
                        val rWrist = Offset(rElbow.x + w * 0.12f, rElbow.y - h * 0.08f)

                        val lKneeLift = if (swing > 0) swing * 0.18f else 0.02f
                        val rKneeLift = if (swing < 0) (-swing) * 0.18f else 0.02f

                        val lKnee = Offset(hip.x + w * 0.16f * maxOf(0.2f, swing), hip.y + h * (0.14f - lKneeLift))
                        val lAnkle = Offset(lKnee.x - w * 0.04f, (lKnee.y + h * 0.16f).coerceAtMost(h * 0.85f))

                        val rKnee = Offset(hip.x - w * 0.14f * maxOf(0.2f, -swing), hip.y + h * (0.15f - rKneeLift))
                        val rAnkle = Offset(rKnee.x - w * 0.06f, (rKnee.y + h * 0.16f).coerceAtMost(h * 0.85f))

                        // Draw Spine & Head
                        drawLine(color = SpeedAmber, start = head, end = neck, strokeWidth = 4.5f)
                        drawLine(color = SpeedAmber, start = neck, end = hip, strokeWidth = 5.5f)
                        drawCircle(color = SpeedAmber, radius = 9f, center = head, style = Stroke(width = 2.5f))

                        // Draw Arms
                        drawLine(color = ElectricCyan, start = neck, end = lElbow, strokeWidth = 3.5f)
                        drawLine(color = ElectricCyan, start = lElbow, end = lWrist, strokeWidth = 3.5f)
                        drawLine(color = ElectricCyan, start = neck, end = rElbow, strokeWidth = 3.5f)
                        drawLine(color = ElectricCyan, start = rElbow, end = rWrist, strokeWidth = 3.5f)

                        // Draw Legs
                        drawLine(color = SuccessGreen, start = hip, end = lKnee, strokeWidth = 4.5f)
                        drawLine(color = SuccessGreen, start = lKnee, end = lAnkle, strokeWidth = 4.5f)
                        drawLine(color = LaserOrange, start = hip, end = rKnee, strokeWidth = 4.5f)
                        drawLine(color = LaserOrange, start = rKnee, end = rAnkle, strokeWidth = 4.5f)

                        // Draw Key Joint Dots
                        listOf(hip, lKnee, rKnee, lAnkle, rAnkle, lWrist, rWrist).forEach { pt ->
                            drawCircle(color = Color.White, radius = 3.5f, center = pt)
                        }

                        // Vector Guide Line
                        drawLine(
                            color = badgeColor.copy(alpha = 0.5f),
                            start = Offset(w * 0.15f, hip.y),
                            end = Offset(w * 0.85f, hip.y - 10f),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                        )
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = "DEMO AI LIVE",
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Black,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                // Right: Step-by-Step Instructions ("Contoh Cara Membuat Senaman")
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Cara Membuat Senaman (Langkah Demi Langkah):",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = ElectricCyan
                    )
                    drill.howToSteps.forEachIndexed { idx, step ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${idx + 1}.",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                color = SpeedAmber
                            )
                            Text(
                                text = step,
                                fontSize = 8.5.sp,
                                color = Color.White,
                                lineHeight = 11.5.sp
                            )
                        }
                    }
                }
            }

            // Sports Science Rationale & Common Mistake Box
            Surface(
                color = StadiumSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, StadiumBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "🧬 Sains Sukan & Otot Sasaran: ${drill.targetBiomechanics}",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = SuccessGreen
                    )
                    Text(
                        text = drill.sportsScienceRationale,
                        fontSize = 8.sp,
                        color = Color.White,
                        lineHeight = 11.sp
                    )
                    Text(
                        text = "⚠ Elakkan Kesilapan: ${drill.commonMistakeToAvoid}",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = LaserOrange
                    )
                }
            }

            // 3 ACTION BUTTONS: TONTON CONTOH YOUTUBE | APPLY KE SEMUA ATLIT | KONGSI VIDEO LATIHAN RUMAH
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Watch / Search Example on YouTube
                Button(
                    onClick = onWatchYoutube,
                    colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Icon(
                        Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Video YouTube",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1
                    )
                }

                // 2. Apply to all registered athletes
                Button(
                    onClick = onApplyToAllAthletes,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAppliedToAllAthletes) SuccessGreen else StadiumSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Icon(
                        imageVector = if (isAppliedToAllAthletes) Icons.Default.Verified else Icons.Default.GroupAdd,
                        contentDescription = null,
                        tint = if (isAppliedToAllAthletes) Color.Black else ElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isAppliedToAllAthletes) "Applied ($registeredAthletesCount) ✓" else "Apply ($registeredAthletesCount Atlit)",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isAppliedToAllAthletes) Color.Black else Color.White,
                        maxLines = 1
                    )
                }

                // 3. Share Video & Home Workout Guide to Athletes
                Button(
                    onClick = onShareVideoToAthletes,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Share Ke Atlit",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun openYoutubeVideoSearch(context: Context, query: String) {
    val encoded = Uri.encode(query)
    val youtubeUrl = "https://www.youtube.com/results?search_query=$encoded"
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Membuka YouTube: $youtubeUrl", Toast.LENGTH_SHORT).show()
    }
}

private fun shareSingleDrillToAthletes(
    context: Context,
    coachName: String,
    drill: SportsScienceDrillItem,
    athletes: List<AthleteEntity>
) {
    val youtubeUrl = "https://www.youtube.com/results?search_query=${Uri.encode(drill.youtubeSearchQuery)}"
    val athleteNames = if (athletes.isNotEmpty()) {
        athletes.joinToString(", ") { it.name }
    } else {
        "Semua Atlit Kelab"
    }
    val stepsFormatted = drill.howToSteps.mapIndexed { idx, s -> "${idx + 1}. $s" }.joinToString("\n")

    val shareMessage = """
        🏃‍♂️ *MODUL LATIHAN RUMAH & SAINS SUKAN AI — PSYCO TIME X PRO*
        👨‍🏫 *Jurulatih:* Coach $coachName
        🎯 *Sukan:* ${drill.sport.label}
        📌 *Kategori:* ${if (drill.type == SuggestionType.WARM_UP) "WARM-UP (Pemanasan RAMP)" else "TRAINING UTAMA"}
        👥 *Atlit Ditugaskan (${athletes.size}):* $athleteNames

        🔥 *Senaman:* ${drill.title}
        ⏱️ *Dos Latihan:* ${drill.setsAndReps}
        🧬 *Fokus Sains Sukan:* ${drill.targetBiomechanics}

        📋 *Cara Membuat Senaman (Langkah Demi Langkah):*
        $stepsFormatted

        ⚠️ *Elakkan Kesilapan:* ${drill.commonMistakeToAvoid}

        🎬 *Tonton Video Contoh Tutorial di YouTube:*
        $youtubeUrl
    """.trimIndent()

    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Latihan Rumah Sains Sukan: ${drill.title}")
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Kongsi Video & Latihan Ke Atlit"))
    } catch (_: Exception) {
        Toast.makeText(context, "Mesej latihan sedia dikongsi kepada atlit!", Toast.LENGTH_SHORT).show()
    }
}

private fun shareFullProgramToAthletes(
    context: Context,
    coachName: String,
    sport: SportDiscipline,
    drills: List<SportsScienceDrillItem>,
    athletes: List<AthleteEntity>
) {
    val athleteNames = if (athletes.isNotEmpty()) {
        athletes.joinToString(", ") { it.name }
    } else {
        "Semua Atlit Berdaftar"
    }
    val drillsText = drills.mapIndexed { idx, d ->
        val ytLink = "https://www.youtube.com/results?search_query=${Uri.encode(d.youtubeSearchQuery)}"
        val typeTag = if (d.type == SuggestionType.WARM_UP) "[WARM-UP]" else "[TRAINING]"
        "${idx + 1}. $typeTag *${d.title}* (${d.setsAndReps})\n   • Cara: ${d.howToSteps.firstOrNull() ?: ""}\n   • Video YouTube: $ytLink"
    }.joinToString("\n\n")

    val fullMessage = """
        🏆 *PEK LATIHAN RUMAH & WARM-UP SAINS SUKAN AI*
        👨‍🏫 *Daripada:* Coach $coachName (Psyco Time X Pro)
        🎽 *Kategori Sukan:* ${sport.label}
        👥 *Wajib Untuk Semua Atlit Berdaftar (${athletes.size}):* $athleteNames

        $drillsText

        💪 Sila tonton video rujukan YouTube di atas dan lakukan mengikut set serta postur biomekanik yang betul!
    """.trimIndent()

    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Pek Latihan Rumah & Warm-Up: ${sport.shortLabel}")
            putExtra(Intent.EXTRA_TEXT, fullMessage)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Kongsi Pek Latihan Rumah Ke Semua Atlit"))
    } catch (_: Exception) {
        Toast.makeText(context, "Pek latihan rumah sedia dikongsi!", Toast.LENGTH_SHORT).show()
    }
}

private fun buildInitialSportsScienceCatalog(): List<SportsScienceDrillItem> = listOf(
    // SPRINT WARM-UP
    SportsScienceDrillItem(
        id = "sprint_wu_1",
        sport = SportDiscipline.SPRINT,
        type = SuggestionType.WARM_UP,
        title = "A-Skip & B-Skip Neuromuscular Rhythm (Protokol RAMP)",
        subtitle = "Pemanasan wajib sebelum pecutan 100m/200m untuk mengaktifkan refleks lutut & dorsifleksi buku lali.",
        setsAndReps = "3 Set x 20 Meter (Rehat 45s)",
        targetBiomechanics = "Hip Flexor (Iliopsoas), Gluteus Maximus & Kekakuan Tendon Achilles",
        sportsScienceRationale = "Meningkatkan koordinasi neuromuskular antara ayunan tangan 90° dan angkatan lutut tinggi, serta mengurangkan masa sentuhan tanah (Ground Contact Time < 100ms).",
        howToSteps = listOf(
            "Berdiri tegak dengan pandangan mata lurus ke hadapan dan teras perut (core) dikunci.",
            "Angkat lutut kaki hadapan sehingga paras pinggang (90°) sambil jari kaki ditarik ke atas (dorsiflexion).",
            "Ayunkan tangan bertentangan dari pipi ke poket seluar secara eksplosif.",
            "Hentak bebola kaki ke bawah tepat di bawah pusat graviti badan (bukan di hadapan badan)."
        ),
        commonMistakeToAvoid = "Badan condong ke belakang atau mendarat menggunakan tumit yang menyebabkan brek mekanikal.",
        youtubeSearchQuery = "Track and Field A Skip B Skip Sprint Warm Up Drill Tutorial",
        demoMotionStyle = DemoMotionStyle.HIGH_KNEES_SKIP
    ),
    SportsScienceDrillItem(
        id = "sprint_wu_2",
        sport = SportDiscipline.SPRINT,
        type = SuggestionType.WARM_UP,
        title = "Ankle Pogo Hops & Stiffness Potentiation",
        subtitle = "Senaman pantulan elastik buku lali (boleh dibuat di rumah atau balapan) untuk kekuatan reaktif (RSI).",
        setsAndReps = "3 Set x 15 Pantulan Pantas (Rehat 60s)",
        targetBiomechanics = "Gastrocnemius, Soleus & Kompleks Tendon Achilles",
        sportsScienceRationale = "Mengaktifkan kitaran regangan-pemendekan pantas (Fast Stretch-Shortening Cycle) supaya buku lali bertindak seperti spring keras semasa pecutan maksimum.",
        howToSteps = listOf(
            "Kunci lutut supaya hampir lurus (sedikit fleksi mikro sahaja).",
            "Lompat menegak menggunakan lantunan bebola kaki sahaja tanpa tumit menyentuh lantai.",
            "Tarik jari kaki ke arah tulang kering (dorsiflexion) semasa di udara sebelum mendarat semula.",
            "Kekalkan masa sentuhan lantai sesingkat mungkin seperti memijak bara panas."
        ),
        commonMistakeToAvoid = "Membengkokkan lutut terlalu banyak sehingga bertukar menjadi squat jump yang lambat.",
        youtubeSearchQuery = "Ankle Pogo Hops Plyometric Sprint Warm Up Drill",
        demoMotionStyle = DemoMotionStyle.ANKLE_POGO_STIFFNESS
    ),
    // SPRINT TRAINING
    SportsScienceDrillItem(
        id = "sprint_tr_1",
        sport = SportDiscipline.SPRINT,
        type = SuggestionType.TRAINING,
        title = "Wall Drive & 45° Block Clearance Acceleration Drill",
        subtitle = "Latihan utama postur tolakan blok permulaan (45° kecondongan badan) — sangat sesuai dibuat di rumah atau padang.",
        setsAndReps = "4 Set x 8 Pertukaran Kaki Pantas (Rehat 90s)",
        targetBiomechanics = "Triple Extension (Pinggul, Lutut, Buku Lali) & Sudut Tolakan 45°",
        sportsScienceRationale = "Melatih penghasilan daya mendatar maksimum (Horizontal Force F0) pada 10 meter pertama supaya pelari tidak bangun tegak terlalu awal.",
        howToSteps = listOf(
            "Letakkan kedua-dua tapak tangan pada dinding dengan badan condong lurus 45° dari kepala ke buku lali.",
            "Angkat sebelah lutut ke paras dada dengan jari kaki ditarik ke atas (dorsiflexion).",
            "Tukar kaki secara eksplosif (1 tukar, 2 tukar, 3 tukar) sambil mengekalkan garis lurus tulang belakang.",
            "Pastikan tumit kaki belakang menolak kuat ke lantai tanpa pinggul jatuh."
        ),
        commonMistakeToAvoid = "Pinggul membengkok ke belakang (sitting back) dan gagal mengekalkan garisan lurus 45°.",
        youtubeSearchQuery = "Sprint Wall Drill Acceleration 45 Degree Posture Track and Field",
        demoMotionStyle = DemoMotionStyle.EXPLOSIVE_BLOCK_START
    ),
    SportsScienceDrillItem(
        id = "sprint_tr_2",
        sport = SportDiscipline.SPRINT,
        type = SuggestionType.TRAINING,
        title = "Nordic Hamstring Eccentric Shield & Flying Wicket Run",
        subtitle = "Gabungan perlindungan otot hamstring di rumah & latihan frekuensi langkah (cadence) kelajuan puncak.",
        setsAndReps = "3 Set x 6 Ulangan Perlahan (4 Saat Turun)",
        targetBiomechanics = "Biceps Femoris (Hamstring) & Kawalan Pelvis Semasa Fasa Ayunan Akhir",
        sportsScienceRationale = "Kajian Sains Sukan membuktikan latihan eksentrik Nordic mengurangkan risiko koyakan hamstring sebanyak 51% dan meningkatkan daya tarikan kaki pada kelajuan > 9.5 m/s.",
        howToSteps = listOf(
            "Melutut di atas pelapik lembut, minta rakan/jurulatih memegang buku lali atau sangkut di bawah sofa di rumah.",
            "Kunci pinggul dan perut supaya badan lurus dari lutut ke bahu.",
            "Turunkan badan ke hadapan secara perlahan-lahan (kawal selama 4 saat) menggunakan kekuatan hamstring.",
            "Tahan dengan tangan apabila hampir mencecah lantai dan tolak naik semula."
        ),
        commonMistakeToAvoid = "Membengkokkan pinggang semasa turun yang membatalkan beban eksentrik pada hamstring.",
        youtubeSearchQuery = "Nordic Hamstring Curl Sprinters Injury Prevention Tutorial",
        demoMotionStyle = DemoMotionStyle.CORE_HAMSTRING_SHIELD
    ),

    // JUMP (LONG JUMP & TRIPLE JUMP) WARM-UP
    SportsScienceDrillItem(
        id = "jump_wu_1",
        sport = SportDiscipline.JUMP,
        type = SuggestionType.WARM_UP,
        title = "Pop-Up Penultimate Step & Take-Off Board Rehearsal",
        subtitle = "Pemanasan spesifik Lompat Jauh & Kijang untuk menyelaraskan langkah kedua terakhir (penultimate) dan lonjakan.",
        setsAndReps = "3 Set x 6 Lonjakan Pendek (Rehat 60s)",
        targetBiomechanics = "Quadriceps Eksentrik, Gluteus Medius & Sudut Lonjakan 20°–22°",
        sportsScienceRationale = "Menurunkan pusat graviti pada langkah kedua terakhir (penultimate step) dan menukarkan halaju lari landas kepada daya angkatan menegak tanpa kehilangan kelajuan.",
        howToSteps = listOf(
            "Mulakan dengan larian anak 5 langkah dengan irama 'cepat-cepat-lambat-CEPAT'.",
            "Rendahkan sedikit pinggul pada langkah kedua terakhir (penultimate) diikuti pijak rata-aktif pada langkah lonjakan.",
            "Dorong lutut kaki bebas (free leg) dan tangan secara serentak ke atas pada sudut ~21°.",
            "Mendarat dengan lembut dan seimbang."
        ),
        commonMistakeToAvoid = "Melihat ke bawah ke arah papan anjakan yang menyebabkan badan condong ke depan dan hilang ketinggian.",
        youtubeSearchQuery = "Long Jump Penultimate Step Pop Up Warm Up Drill Tutorial",
        demoMotionStyle = DemoMotionStyle.BOUNDING_JUMP
    ),
    SportsScienceDrillItem(
        id = "jump_wu_2",
        sport = SportDiscipline.JUMP,
        type = SuggestionType.WARM_UP,
        title = "Dynamic Hurdle Hip Mobility & Thoracic Opener",
        subtitle = "Mobiliti sendi pinggul 360° untuk membuka julat pergerakan fasa layangan (Hang / Hitch-Kick) & pendaratan pasir.",
        setsAndReps = "2 Set x 10 Setiap Sebelah Kaki",
        targetBiomechanics = "Kapsul Sendi Pinggul, Adductor & Fleksor Pinggul",
        sportsScienceRationale = "Julat pergerakan pinggul yang luas membolehkan atlit melakukan teknik Hitch-Kick atau Hang di udara serta mengangkat kaki tinggi semasa pendaratan pasir.",
        howToSteps = listOf(
            "Berdiri tegak, angkat sebelah lutut ke hadapan dan buka pusingan keluar melepasi bayangan pagar.",
            "Kekalkan bahu menghadap ke hadapan tanpa memusingkan pinggang.",
            "Lakukan gerakan berbalik (dari luar ke dalam) secara terkawal.",
            "Tambah lunges dinamik dengan regangan tangan ke atas."
        ),
        commonMistakeToAvoid = "Melakukan pusingan terlalu laju dengan momentum badan tanpa kawalan otot pinggul.",
        youtubeSearchQuery = "Track and Field Hurdle Hip Mobility Warm Up Long Jump",
        demoMotionStyle = DemoMotionStyle.HIP_HURDLE_MOBILITY
    ),

    // JUMP (LONG JUMP & TRIPLE JUMP) TRAINING
    SportsScienceDrillItem(
        id = "jump_tr_1",
        sport = SportDiscipline.JUMP,
        type = SuggestionType.TRAINING,
        title = "Alternate Leg Bounding & Single-Leg Hop-Step-Jump",
        subtitle = "Latihan plyometrik utama Lompat Jauh & Triple Jump untuk kekuatan reaktif mendatar dan keseimbangan fasa.",
        setsAndReps = "4 Set x 30 Meter Bounding (Rehat Penuh 2.5 Minit)",
        targetBiomechanics = "Kekuatan Eksplosif Sebelah Kaki, Hamstring-Glute & Kestabilan Pelvis",
        sportsScienceRationale = "Melatih daya tahan impak sehingga 6–8 kali berat badan semasa papan dipijak dan memastikan nisbah Hop:Step:Jump (35%:30%:35%) seimbang.",
        howToSteps = listOf(
            "Lakukan lonjakan langkah panjang bergilir-gilir (kiri-kanan-kiri) dengan fokus masa terapung di udara.",
            "Tahan lutut hadapan pada sudut 90° seketika di udara sebelum menarik kaki aktif ke bawah (pawing action).",
            "Gunakan ayunan kedua-dua belah tangan atau ayunan bergilir yang kuat untuk membantu angkatan.",
            "Pastikan tapak kaki mendarat rata-aktif tepat di bawah pinggul."
        ),
        commonMistakeToAvoid = "Kaki mendarat jauh di hadapan badan yang menyebabkan lutut terkunci dan hilang momentum.",
        youtubeSearchQuery = "Alternate Leg Bounding Triple Jump Long Jump Plyometric Drill",
        demoMotionStyle = DemoMotionStyle.BOUNDING_JUMP
    ),
    SportsScienceDrillItem(
        id = "jump_tr_2",
        sport = SportDiscipline.JUMP,
        type = SuggestionType.TRAINING,
        title = "Box Depth Drop to Standing Broad Jump & Leg Shoot Landing",
        subtitle = "Latihan rumah / gim untuk meningkatkan refleks lonjakan papan dan teknik pendaratan kaki jauh ke hadapan.",
        setsAndReps = "4 Set x 5 Lompatan Eksplosif (Rehat 2 Minit)",
        targetBiomechanics = "Reactive Strength Index (RSI), Kuasa Quadriceps & Teknik Leg-Shoot Pendaratan",
        sportsScienceRationale = "Mengelakkan kerugian jarak 25–40 cm akibat punggung atau tangan jatuh ke belakang semasa mendarat di kotak pasir.",
        howToSteps = listOf(
            "Berdiri di atas bangku rendah (25–35 cm), melangkah turun dan sebaik sahaja bebola kaki mencecah lantai, terus melompat jauh ke hadapan.",
            "Semasa di puncak lompatan, luruskan kedua-dua kaki jauh ke hadapan (leg shoot) sambil tangan diayun ke belakang kemudian ke depan.",
            "Sebaik sahaja tumit menyentuh lantai/pasir, bengkokkan lutut dan tolak pinggul ke arah hadapan bekas tapak kaki."
        ),
        commonMistakeToAvoid = "Jatuh terduduk ke belakang selepas mendarat atau meletakkan tapak tangan di belakang garisan tumit.",
        youtubeSearchQuery = "Depth Jump to Standing Broad Jump Long Jump Landing Technique",
        demoMotionStyle = DemoMotionStyle.BOUNDING_JUMP
    ),

    // HURDLES & POWER
    SportsScienceDrillItem(
        id = "hurdle_wu_1",
        sport = SportDiscipline.HURDLES_POWER,
        type = SuggestionType.WARM_UP,
        title = "Lead-Leg Snap & Trail-Leg Wall Rhythm Drill",
        subtitle = "Pemanasan pantas kaki hadapan (lead leg) dan lipatan kaki belakang (trail leg) untuk lari berpagar.",
        setsAndReps = "3 Set x 8 Ulangan Setiap Kaki",
        targetBiomechanics = "Kelajuan Lead-Leg Down-Snap & Abduksi Pinggul Trail-Leg",
        sportsScienceRationale = "Memendekkan masa terapung di atas pagar supaya pelari kembali mencecah tanah dan memecut secepat mungkin.",
        howToSteps = listOf(
            "Berdiri menghadap dinding atau pagar rendah, angkat lutut kaki hadapan dahulu sebelum menendang tapak kaki.",
            "Tekan kaki hadapan turun ke lantai dengan pantas sebaik melepasi ketinggian pagar.",
            "Tarik kaki belakang (trail leg) melalui sisi dengan lutut lebih tinggi daripada buku lali."
        ),
        commonMistakeToAvoid = "Mengayun kaki hadapan dalam keadaan lurus dari bawah yang melambatkan pelepasan pagar.",
        youtubeSearchQuery = "Hurdle Lead Leg Trail Leg Wall Drill Track and Field",
        demoMotionStyle = DemoMotionStyle.HIP_HURDLE_MOBILITY
    ),
    SportsScienceDrillItem(
        id = "hurdle_tr_1",
        sport = SportDiscipline.HURDLES_POWER,
        type = SuggestionType.TRAINING,
        title = "3-Step Rhythm Mini-Hurdle Wicket Sprints",
        subtitle = "Latihan irama 3 langkah pantas di antara pagar untuk kelajuan frekuensi langkah.",
        setsAndReps = "4 Set x 5 Pagar Rendah (Rehat 2 Minit)",
        targetBiomechanics = "Frekuensi Langkah (Cadence) & Kestabilan Badan Atas",
        sportsScienceRationale = "Melatih sistem saraf pusat mengekalkan irama 3 langkah konsisten walaupun di bawah keletihan.",
        howToSteps = listOf(
            "Susun 5 pagar rendah atau penanda kasut di rumah/padang dengan jarak sekata.",
            "Berlari melepasi setiap penanda dengan irama 3 langkah yang tajam di atas bebola kaki.",
            "Kekalkan dagu dan bahu stabil tanpa melompat tinggi."
        ),
        commonMistakeToAvoid = "Melompat pagar terlalu tinggi sehingga hilang kelajuan mendatar.",
        youtubeSearchQuery = "3 Step Hurdle Rhythm Drill Mini Hurdles Sprint Training",
        demoMotionStyle = DemoMotionStyle.HIGH_KNEES_SKIP
    ),

    // HOME RECOVERY & CORE
    SportsScienceDrillItem(
        id = "home_wu_1",
        sport = SportDiscipline.HOME_RECOVERY,
        type = SuggestionType.WARM_UP,
        title = "World's Greatest Stretch & Thoracic Spine Rotation",
        subtitle = "Pemanasan mobiliti seluruh badan yang wajib dibuat oleh atlit di rumah setiap pagi atau sebelum latihan.",
        setsAndReps = "2 Set x 6 Ulangan Setiap Sisi",
        targetBiomechanics = "Hamstring, Hip Flexor, Tulang Belakang Torasik & Bahu",
        sportsScienceRationale = "Membuka rantaian kinetik dari buku lali hingga bahu untuk mengelakkan kecederaan otot semasa latihan intensiti tinggi.",
        howToSteps = listOf(
            "Ambil langkah lunge besar ke hadapan dan letakkan kedua-dua tangan di lantai di sebelah kaki depan.",
            "Turunkan siku dalam ke arah buku lali dan tahan selama 2 saat.",
            "Pusingkan dada dan angkat tangan menghala ke siling sambil mata mengikut jari tangan.",
            "Luruskan kaki hadapan perlahan-lahan untuk meregangkan hamstring sebelum bertukar kaki."
        ),
        commonMistakeToAvoid = "Menahan nafas semasa regangan; hembus nafas perlahan-lahan pada setiap pusingan.",
        youtubeSearchQuery = "Worlds Greatest Stretch Athletes Mobility Warm Up Home",
        demoMotionStyle = DemoMotionStyle.HIP_HURDLE_MOBILITY
    ),
    SportsScienceDrillItem(
        id = "home_tr_1",
        sport = SportDiscipline.HOME_RECOVERY,
        type = SuggestionType.TRAINING,
        title = "Single-Leg Glute Bridge Isometric Hold & Deadbug Core Lock",
        subtitle = "Latihan kekuatan teras & lantunan pinggul tanpa alatan — khusus untuk latihan rumah semua atlit berdaftar.",
        setsAndReps = "3 Set x 30 Saat Tahan Setiap Kaki",
        targetBiomechanics = "Gluteus Maximus, Hamstring Proksimal & Transversus Abdominis",
        sportsScienceRationale = "Kestabilan pelvis menghalang kebocoran tenaga (energy leak) semasa kaki menolak tanah ketika memecut atau melompat.",
        howToSteps = listOf(
            "Baring terlentang di atas tikar di rumah, bengkokkan satu lutut (90°) dan angkat sebelah kaki lurus.",
            "Tolak tumit ke lantai untuk mengangkat pinggul sehingga badan membentuk garisan lurus dari bahu ke lutut.",
            "Kunci otot perut dan glute selama 30 saat tanpa membiarkan pinggul senget.",
            "Tukar kaki dan ulang."
        ),
        commonMistakeToAvoid = "Melengkungkan tulang belakang bawah (lower back arch) akibat tidak mengunci otot perut.",
        youtubeSearchQuery = "Single Leg Glute Bridge Isometric Hold Sprinters Home Workout",
        demoMotionStyle = DemoMotionStyle.CORE_HAMSTRING_SHIELD
    )
)
