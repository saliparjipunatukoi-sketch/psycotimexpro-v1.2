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
import com.example.data.model.AthleteEntity
import com.example.data.model.FeeRecordEntity
import com.example.ui.theme.*

@Composable
fun FeeManagementScreen(
    fees: List<FeeRecordEntity>,
    athletes: List<AthleteEntity>,
    onAddFee: (FeeRecordEntity) -> Unit,
    onUpdateFee: (FeeRecordEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var filterStatus by remember { mutableStateOf("All") } // "All", "Paid", "Pending"
    var showAddFeeDialog by remember { mutableStateOf(false) }

    val filteredFees = fees.filter {
        when (filterStatus) {
            "Paid" -> it.isPaid
            "Pending" -> !it.isPaid
            else -> true
        }
    }

    val totalPaid = fees.filter { it.isPaid }.sumOf { it.amount }
    val totalPending = fees.filter { !it.isPaid }.sumOf { it.amount }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddFeeDialog = true },
                containerColor = ElectricCyan,
                contentColor = Color.Black,
                modifier = Modifier.testTag("fab_add_fee")
            ) {
                Icon(Icons.Default.AddCard, contentDescription = "Rekod Yuran")
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
                        text = "Pengurusan Yuran Latihan & Pendaftaran",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Jejak status pembayaran yuran bulanan, pendaftaran dan janaan resit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Summary Financial Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = StadiumSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Jumlah Kutipan Lunas", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("RM %.2f".format(totalPaid), fontSize = 20.sp, fontWeight = FontWeight.Black, color = SuccessGreen)
                        }
                    }

                    Surface(
                        color = StadiumSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SpeedAmber.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Jumlah Tertunggak", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("RM %.2f".format(totalPending), fontSize = 20.sp, fontWeight = FontWeight.Black, color = SpeedAmber)
                        }
                    }
                }
            }

            // Filters
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = filterStatus == "All",
                        onClick = { filterStatus = "All" },
                        label = { Text("Semua (${fees.size})") }
                    )
                    FilterChip(
                        selected = filterStatus == "Paid",
                        onClick = { filterStatus = "Paid" },
                        label = { Text("Lunas (${fees.count { it.isPaid }})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SuccessGreen,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = filterStatus == "Pending",
                        onClick = { filterStatus = "Pending" },
                        label = { Text("Tertunggak (${fees.count { !it.isPaid }})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SpeedAmber,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            // Fee Items
            if (filteredFees.isEmpty()) {
                item {
                    Surface(
                        color = StadiumSurface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("Tiada rekod yuran dalam senarai.", color = TextSecondary)
                        }
                    }
                }
            } else {
                items(filteredFees) { fee ->
                    FeeCard(
                        fee = fee,
                        onTogglePaid = {
                            onUpdateFee(fee.copy(isPaid = !fee.isPaid))
                        }
                    )
                }
            }
        }
    }

    if (showAddFeeDialog) {
        AddFeeDialog(
            athletes = athletes,
            onDismiss = { showAddFeeDialog = false },
            onSave = { fee ->
                onAddFee(fee)
                showAddFeeDialog = false
            }
        )
    }
}

@Composable
private fun FeeCard(
    fee: FeeRecordEntity,
    onTogglePaid: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StadiumSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
        modifier = Modifier.fillMaxWidth().testTag("fee_card_${fee.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (fee.isPaid) SuccessGreen.copy(alpha = 0.2f) else SpeedAmber.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (fee.isPaid) Icons.Default.ReceiptLong else Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = if (fee.isPaid) SuccessGreen else SpeedAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fee.athleteName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${fee.feeType} • Resit: ${fee.receiptNo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "Tarikh: ${fee.paymentDate}",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "RM %.2f".format(fee.amount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = if (fee.isPaid) SuccessGreen else SpeedAmber
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onTogglePaid,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (fee.isPaid) "Tanda Belum" else "Tanda Lunas",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFeeDialog(
    athletes: List<AthleteEntity>,
    onDismiss: () -> Unit,
    onSave: (FeeRecordEntity) -> Unit
) {
    var selectedAthleteName by remember { mutableStateOf(athletes.firstOrNull()?.name ?: "Atlit") }
    var selectedAthleteId by remember { mutableStateOf(athletes.firstOrNull()?.id ?: 1L) }
    var feeType by remember { mutableStateOf("Yuran Bulanan Latihan") }
    var amount by remember { mutableStateOf("80.0") }
    var isPaid by remember { mutableStateOf(true) }
    var athleteExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rekod Yuran Baharu", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
                    expanded = athleteExpanded,
                    onExpandedChange = { athleteExpanded = !athleteExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAthleteName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pilih Atlit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = athleteExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = athleteExpanded,
                        onDismissRequest = { athleteExpanded = false }
                    ) {
                        athletes.forEach { a ->
                            DropdownMenuItem(
                                text = { Text(a.name) },
                                onClick = {
                                    selectedAthleteName = a.name
                                    selectedAthleteId = a.id
                                    athleteExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = feeType,
                    onValueChange = { feeType = it },
                    label = { Text("Jenis Yuran") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Jumlah (RM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPaid, onCheckedChange = { isPaid = it })
                    Text("Telah Dibayar (Lunas)", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val feeVal = amount.toDoubleOrNull() ?: 80.0
                    onSave(
                        FeeRecordEntity(
                            athleteId = selectedAthleteId,
                            athleteName = selectedAthleteName,
                            feeType = feeType,
                            amount = feeVal,
                            isPaid = isPaid,
                            receiptNo = "PTX-REC-${System.currentTimeMillis() % 10000}",
                            paymentDate = if (isPaid) "Hari Ini" else "Tertunggak"
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
            ) {
                Text("Simpan Rekod Yuran", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
        containerColor = StadiumSurface
    )
}
