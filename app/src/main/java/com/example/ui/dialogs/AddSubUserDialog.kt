package com.example.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubUserDialog(
    currentSubUserCount: Int,
    onDismiss: () -> Unit,
    onAddSubUser: (username: String, fullName: String, role: String, pin: String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Urusetia") }
    var pin by remember { mutableStateOf("1234") }

    val roles = listOf("Urusetia", "Pegawai Teknikal", "Penolong Jurulatih")
    var roleDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Daftar Sub-User Urusetia / Teknikal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Slot Digunakan: $currentSubUserCount / 10 Maksimum",
                    color = ElectricCyan,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username Sub-User") },
                    placeholder = { Text("cth: urusetia_padang1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("subuser_username_input")
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nama Penuh Pegawai") },
                    placeholder = { Text("cth: Cikgu Ahmad Razak") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = roleDropdownExpanded,
                    onExpandedChange = { roleDropdownExpanded = !roleDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = role,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Peranan Pegawai") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = roleDropdownExpanded,
                        onDismissRequest = { roleDropdownExpanded = false }
                    ) {
                        roles.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = {
                                    role = r
                                    roleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("Kod PIN Akses (4 Digit)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isNotBlank() && fullName.isNotBlank()) {
                        onAddSubUser(username.trim(), fullName.trim(), role, pin.trim())
                    }
                },
                enabled = username.isNotBlank() && currentSubUserCount < 10,
                colors = ButtonDefaults.buttonColors(containerColor = LaserOrange),
                modifier = Modifier.testTag("confirm_add_subuser_button")
            ) {
                Text("Cipta Sub-User", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSecondary)
            }
        },
        containerColor = StadiumSurface
    )
}
