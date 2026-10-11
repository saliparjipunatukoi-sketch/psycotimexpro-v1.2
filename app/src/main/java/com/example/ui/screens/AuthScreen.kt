package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PsycotimexCircularLogo
import com.example.ui.theme.*

/**
 * Skrin Log Masuk & Pendaftaran Pengguna / Jurulatih Baru (Trial & Superadmin)
 * Memastikan APK tidak terus auto-login ke akaun sesiapa.
 * Pengguna boleh log masuk atau daftar akaun kelab baru mereka sendiri.
 */
@Composable
fun AuthScreen(
    onLoginSuccess: (email: String, name: String, club: String, isSuperAdmin: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isRegisterMode by remember { mutableStateOf(false) }

    // Form States
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var clubNameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Google Gmail Login & Registration Popup States
    var showGooglePicker by remember { mutableStateOf(false) }
    var googleSelectedEmail by remember { mutableStateOf("") }
    var googleSelectedName by remember { mutableStateOf("") }
    var showDetailCompletionPopup by remember { mutableStateOf(false) }
    var popupNameInput by remember { mutableStateOf("") }
    var popupClubInput by remember { mutableStateOf("") }
    var popupPhoneInput by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TrackDarkNavy),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Emblem Logo
            PsycotimexCircularLogo(
                sizeDp = 100,
                modifier = Modifier.testTag("auth_emblem")
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "PSYCO TIME X PRO",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                text = if (isRegisterMode) "Pendaftaran Jurulatih Baru (Percubaan 7 Hari)" else "Log Masuk Pengguna & Jurulatih",
                fontSize = 12.sp,
                color = SilverMetallic,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Card Container
            Card(
                colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isRegisterMode) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Nama Penuh Jurulatih") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = FinishLineRed) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_reg_name")
                        )

                        OutlinedTextField(
                            value = clubNameInput,
                            onValueChange = { clubNameInput = it },
                            label = { Text("Nama Kelab / Sekolah") },
                            leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = SilverMetallic) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_reg_club")
                        )

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("No. Telefon WhatsApp") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = SilverMetallic) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_reg_phone")
                        )
                    }

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Emel Pengguna") },
                        placeholder = { Text("contoh@gmail.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = FinishLineRed) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_auth_email")
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Kata Laluan") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SilverMetallic) },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = SilverMetallic
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_auth_password")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Submit Action Button
                    Button(
                        onClick = {
                            val cleanEmail = emailInput.trim().lowercase()
                            val cleanPass = passwordInput.trim()

                            if (cleanEmail.isBlank()) {
                                Toast.makeText(context, "Sila masukkan emel anda.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (cleanPass.isBlank()) {
                                Toast.makeText(context, "Sila masukkan kata laluan.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (isRegisterMode) {
                                // Register as new user with 7-day trial
                                val finalName = if (nameInput.isNotBlank()) nameInput.trim() else "Jurulatih ${cleanEmail.take(6)}"
                                val finalClub = if (clubNameInput.isNotBlank()) clubNameInput.trim() else "Kelab Sukan"
                                Toast.makeText(context, "Pendaftaran berjaya! Selamat mencuba 7 Days Trial.", Toast.LENGTH_LONG).show()
                                onLoginSuccess(cleanEmail, finalName, finalClub, false)
                            } else {
                                // Check if Superadmin
                                val isSuper = (cleanEmail == "saliparjipun.atukoi@gmail.com")
                                val finalName = if (isSuper) "Coach Salipar Jipun (Superadmin)" else "Jurulatih (${cleanEmail.substringBefore("@")})"
                                val finalClub = if (isSuper) "Psyco Track & Field Elite" else "Kelab Olahraga"
                                
                                Toast.makeText(context, "Log masuk berjaya sebagai $finalName", Toast.LENGTH_SHORT).show()
                                onLoginSuccess(cleanEmail, finalName, finalClub, isSuper)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_auth_submit")
                    ) {
                        Icon(
                            imageVector = if (isRegisterMode) Icons.Default.AppRegistration else Icons.Default.Login,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRegisterMode) "Daftar Akaun Percubaan" else "Log Masuk ke Sistem",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Divider with OR
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = StadiumBorder
                        )
                        Text(
                            text = "ATAU",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = StadiumBorder
                        )
                    }

                    // Google Gmail Login / Register Button
                    OutlinedButton(
                        onClick = {
                            showGooglePicker = true
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SilverBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_google_auth")
                    ) {
                        // Google 'G' stylized representation
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = Color(0xFF4285F4),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "G",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isRegisterMode) "Daftar dengan Google Gmail" else "Log Masuk dengan Google Gmail",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F1F1F)
                        )
                    }

                    // Mode Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRegisterMode) "Sudah ada akaun? " else "Pengguna baru? ",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = if (isRegisterMode) "Log Masuk Di Sini" else "Daftar Akaun (7 Days Free)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.clickable {
                                isRegisterMode = !isRegisterMode
                            }
                        )
                    }
                }
            }

            // Quick Demo Shortcut for Testing
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        emailInput = "coach.demo@gmail.com"
                        passwordInput = "123456"
                        isRegisterMode = false
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SilverBorder.copy(alpha = 0.5f))
                ) {
                    Text("Isi Demo Coach", fontSize = 11.sp, color = SilverMetallic)
                }

                OutlinedButton(
                    onClick = {
                        emailInput = "saliparjipun.atukoi@gmail.com"
                        passwordInput = "admin123"
                        isRegisterMode = false
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FinishLineRed.copy(alpha = 0.6f))
                ) {
                    Text("Isi Superadmin", fontSize = 11.sp, color = FinishLineRed)
                }
            }
        }

        // GOOGLE ACCOUNT SELECTION MODAL POPUP
        if (showGooglePicker) {
            AlertDialog(
                onDismissRequest = { showGooglePicker = false },
                containerColor = StadiumSurface,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = Color(0xFF4285F4),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "G",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Pilih Akaun Google Gmail",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "Untuk meneruskan ke Psyco Time X Pro",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pre-listed device Google accounts
                        listOf(
                            Triple("Salipar Jipun", "saliparjipun.atukoi@gmail.com", "Superadmin"),
                            Triple("Coach Demo Sukan", "coach.demo@gmail.com", "Pengguna Demo"),
                            Triple("Jurulatih Olahraga", "jurulatih.malaysia@gmail.com", "Akaun Gmail Peribadi")
                        ).forEach { (gName, gEmail, gDesc) ->
                            Surface(
                                color = StadiumSurfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (gEmail == "saliparjipun.atukoi@gmail.com") FinishLineRed.copy(alpha = 0.6f) else StadiumBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showGooglePicker = false
                                        googleSelectedEmail = gEmail
                                        googleSelectedName = gName
                                        popupNameInput = gName
                                        popupClubInput = if (gEmail == "saliparjipun.atukoi@gmail.com") "Psyco Track & Field Elite" else "Kelab Olahraga Tempatan"
                                        popupPhoneInput = if (gEmail == "saliparjipun.atukoi@gmail.com") "+6019-8765432" else "+6012-3456789"
                                        
                                        // Open detail completion popup for Google account
                                        showDetailCompletionPopup = true
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        color = if (gEmail == "saliparjipun.atukoi@gmail.com") FinishLineRed else LaserOrange,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = gName.take(1),
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = gName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = gEmail,
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = gDesc,
                                            fontSize = 10.sp,
                                            color = if (gEmail == "saliparjipun.atukoi@gmail.com") FinishLineRed else SilverMetallic
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = SilverMetallic,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Option to enter another custom Gmail
                        OutlinedButton(
                            onClick = {
                                showGooglePicker = false
                                googleSelectedEmail = ""
                                googleSelectedName = ""
                                popupNameInput = ""
                                popupClubInput = ""
                                popupPhoneInput = ""
                                showDetailCompletionPopup = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SilverBorder.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = SilverMetallic, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gunakan Akaun Gmail Lain", fontSize = 11.sp, color = SilverMetallic)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showGooglePicker = false }) {
                        Text("Batal", color = SilverMetallic)
                    }
                }
            )
        }

        // DETAIL COMPLETION POPUP MODAL (POPUP MASUKKAN DETAIL SELEBIHNYA SAMA SEPERTI YANG SEDIA ADA)
        if (showDetailCompletionPopup) {
            AlertDialog(
                onDismissRequest = { showDetailCompletionPopup = false },
                containerColor = StadiumSurface,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AssignmentInd,
                            contentDescription = null,
                            tint = FinishLineRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "Lengkapkan Butiran Jurulatih",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "Pendaftaran & Pengesahan Akaun Google Gmail",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = StadiumSurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Akaun Google Disahkan:",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = if (googleSelectedEmail.isNotBlank()) googleSelectedEmail else "(Akaun Gmail Baru)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (googleSelectedEmail.isBlank()) {
                            OutlinedTextField(
                                value = googleSelectedEmail,
                                onValueChange = { googleSelectedEmail = it },
                                label = { Text("Emel Google Gmail") },
                                placeholder = { Text("nama@gmail.com") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = FinishLineRed) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("popup_input_gmail")
                            )
                        }

                        OutlinedTextField(
                            value = popupNameInput,
                            onValueChange = { popupNameInput = it },
                            label = { Text("Nama Penuh Jurulatih / Pegawai") },
                            placeholder = { Text("Contoh: Mohd Ali bin Hassan") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = FinishLineRed) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("popup_input_name")
                        )

                        OutlinedTextField(
                            value = popupClubInput,
                            onValueChange = { popupClubInput = it },
                            label = { Text("Nama Kelab / Pasukan / Sekolah") },
                            placeholder = { Text("Contoh: SMK Seri Kota Track Team") },
                            leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = SilverMetallic) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("popup_input_club")
                        )

                        OutlinedTextField(
                            value = popupPhoneInput,
                            onValueChange = { popupPhoneInput = it },
                            label = { Text("No. Telefon WhatsApp") },
                            placeholder = { Text("Contoh: 012-3456789") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = SilverMetallic) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("popup_input_phone")
                        )

                        Surface(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Pendaftaran Google secara automatik mengaktifkan Percubaan Percuma 7 Hari (Trial). Jika emel Superadmin dipilih, hak akses Superadmin penuh akan diaktifkan serta-merta.",
                                fontSize = 10.sp,
                                color = TextMuted,
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val cleanEmail = googleSelectedEmail.trim().lowercase()
                            if (cleanEmail.isBlank()) {
                                Toast.makeText(context, "Sila masukkan atau pilih emel Google Gmail anda.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val isSuper = (cleanEmail == "saliparjipun.atukoi@gmail.com")
                            val finalName = if (popupNameInput.isNotBlank()) {
                                popupNameInput.trim()
                            } else if (isSuper) {
                                "Coach Salipar Jipun (Superadmin)"
                            } else {
                                "Jurulatih (${cleanEmail.substringBefore("@")})"
                            }

                            val finalClub = if (popupClubInput.isNotBlank()) {
                                popupClubInput.trim()
                            } else if (isSuper) {
                                "Psyco Track & Field Elite"
                            } else {
                                "Kelab Olahraga"
                            }

                            showDetailCompletionPopup = false
                            Toast.makeText(context, "Akaun Google Berjaya Disambungkan: $finalName!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess(cleanEmail, finalName, finalClub, isSuper)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("popup_btn_confirm")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sahkan & Masuk Sistem", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDetailCompletionPopup = false }) {
                        Text("Batal", color = SilverMetallic)
                    }
                }
            )
        }
    }
}
