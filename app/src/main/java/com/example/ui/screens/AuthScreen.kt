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
    }
}
