package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.ui.components.PsycotimexCircularLogo
import com.example.ui.theme.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "AuthScreen"

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/**
 * Skrin Log Masuk & Pengesahan Google Sign-In (Jetpack Credential Manager + Firebase Auth)
 * Menyokong penyelarasan awan masa nyata (Firebase Firestore) untuk Atlit & Rekod Race.
 */
@Composable
fun AuthScreen(
    onLoginSuccess: (email: String, name: String, club: String, isSuperAdmin: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isSigningIn by remember { mutableStateOf(false) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }

    // Optional club profile customization fields synchronized upon Google Sign-In
    var clubNameInput by remember { mutableStateOf("Psyco Track & Field Elite") }
    var phoneInput by remember { mutableStateOf("+60 12-345 6789") }

    suspend fun handleGoogleCredential(credential: Credential) {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
            val firebaseUser = authResult.user
            val email = (firebaseUser?.email ?: googleIdTokenCredential.id).trim().lowercase()
            val isSuper = email.equals("saliparjipun.atukoi@gmail.com", ignoreCase = true)
            val displayName = firebaseUser?.displayName
                ?: googleIdTokenCredential.displayName
                ?: if (isSuper) "Coach Salipar Jipun (Superadmin)" else "Jurulatih (${email.substringBefore("@")})"
            val resolvedClub = clubNameInput.trim().ifBlank {
                if (isSuper) "Psyco Track & Field Elite" else "Kelab Olahraga"
            }

            Toast.makeText(
                context,
                "Log Masuk Google & Cloud Sync Aktif: $displayName",
                Toast.LENGTH_SHORT
            ).show()
            onLoginSuccess(email, displayName, resolvedClub, isSuper)
        } else {
            authErrorMessage = "Jenis kelayakan tidak disokong."
        }
    }

    // Attempt silent auto sign-in on launch for returning Google users
    LaunchedEffect(Unit) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            val email = (currentUser.email ?: "").trim().lowercase()
            val isSuper = email.equals("saliparjipun.atukoi@gmail.com", ignoreCase = true)
            val name = currentUser.displayName
                ?: if (isSuper) "Coach Salipar Jipun (Superadmin)" else "Jurulatih (${email.substringBefore("@").ifBlank { "Pro" }})"
            onLoginSuccess(email, name, clubNameInput.ifBlank { "Psyco Track & Field Elite" }, isSuper)
            return@LaunchedEffect
        }

        val activityContext = context.findActivity() ?: return@LaunchedEffect
        val webClientId = context.getString(R.string.default_web_client_id)
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )
            handleGoogleCredential(result.credential)
        } catch (_: NoCredentialException) {
            // Silent check found no authorized account; wait for explicit Google Sign-In button click
        } catch (e: GetCredentialException) {
            Log.d(TAG, "Silent sign-in skipped: ${e.message}")
        } catch (e: Exception) {
            Log.d(TAG, "Silent sign-in error: ${e.message}")
        }
    }

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
                text = "Sistem Timing Elektronik & Storan Awan Firebase Firestore",
                fontSize = 12.sp,
                color = SilverMetallic,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        color = StadiumSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Cloud Sync",
                                tint = SuccessGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Firebase Cloud & Google Sign-In",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Log masuk menggunakan akaun Google anda untuk menyelaraskan rekod atlit, masa larian & analisis AI ke awan.",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = clubNameInput,
                        onValueChange = { clubNameInput = it },
                        label = { Text("Nama Kelab / Pasukan / Sekolah") },
                        leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = FinishLineRed) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_reg_club")
                    )

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("No. Telefon Jurulatih (Pilihan)") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = SilverMetallic) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_reg_phone")
                    )

                    if (authErrorMessage != null) {
                        Surface(
                            color = FinishLineRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FinishLineRed.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = "Amaran Log Masuk",
                                    tint = FinishLineRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = authErrorMessage ?: "",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    // Primary Interactive Google Sign-In Button via Jetpack Credential Manager
                    Button(
                        onClick = {
                            val activityContext = context.findActivity()
                            if (activityContext == null) {
                                authErrorMessage = "Konteks aktiviti tidak ditemui untuk dialog Google Sign-In."
                                return@Button
                            }
                            authErrorMessage = null
                            isSigningIn = true
                            coroutineScope.launch {
                                try {
                                    val webClientId = context.getString(R.string.default_web_client_id)
                                    val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
                                        serverClientId = webClientId
                                    ).build()
                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(signInWithGoogleOption)
                                        .build()
                                    val result = credentialManager.getCredential(
                                        request = request,
                                        context = activityContext
                                    )
                                    handleGoogleCredential(result.credential)
                                } catch (_: GetCredentialCancellationException) {
                                    // User dismissed the picker; no error banner needed
                                } catch (e: NoCredentialException) {
                                    Log.w(TAG, "No Google account on device", e)
                                    authErrorMessage =
                                        "Tiada akaun Google ditemui pada peranti ini. Sila tambah akaun Google dalam Tetapan Android (Settings > Passwords & accounts) dan cuba lagi."
                                } catch (e: GetCredentialException) {
                                    Log.e(TAG, "Credential Manager error", e)
                                    authErrorMessage = "Log masuk Google gagal: ${e.localizedMessage ?: e.message}"
                                } catch (e: Exception) {
                                    Log.e(TAG, "Firebase Google auth error", e)
                                    authErrorMessage = "Ralat pengesahan Firebase: ${e.localizedMessage ?: e.message}"
                                } finally {
                                    isSigningIn = false
                                }
                            }
                        },
                        enabled = !isSigningIn,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_google_auth")
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                color = Color(0xFF4285F4),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Menyambung ke Google & Firebase...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F1F1F)
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF4285F4),
                                modifier = Modifier.size(26.dp)
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
                                text = "Log Masuk dengan Google",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F1F1F)
                            )
                        }
                    }
                }
            }
        }
    }
}
