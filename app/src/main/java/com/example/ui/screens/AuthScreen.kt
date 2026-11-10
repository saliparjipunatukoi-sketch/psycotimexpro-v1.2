package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
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
import com.example.service.*
import com.example.ui.components.PsycotimexCircularLogo
import com.example.ui.theme.*
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

val COACH_CERTIFICATIONS = listOf(
    "World Athletics Level 1 (Sprints / Youth)",
    "World Athletics Level 2 (Specialist)",
    "World Athletics Level 3 (High Performance)",
    "Sains Sukan Level 1 (Sports Science)",
    "Sains Sukan Level 2 (ISN / KBS)",
    "Sains Sukan Level 3 (Master)",
    "MSSM / MSSD School Track & Field Coach",
    "National Athletics Coach",
    "Certified Strength & Conditioning Specialist",
    "Club Coach / Bebas"
)

val SPECIALIZATION_EVENTS = listOf(
    "Sprints (100m, 200m, 400m)",
    "Jumps (Long Jump & Triple Jump)",
    "Hurdles & Relays",
    "Middle & Long Distance (800m - 5000m)",
    "Throws (Shot Put, Discus, Javelin)",
    "All Track & Field Events"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onLoginSuccess: (
        email: String,
        name: String,
        club: String,
        country: String,
        phone: String,
        cert: String,
        specialization: String,
        isSuperAdmin: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val langHook = rememberLanguageHook(currentLanguage, onLanguageChange)

    var isSigningIn by remember { mutableStateOf(false) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }

    // 1st-Time Coach Mandatory Onboarding Registration Form Details
    var fullNameInput by remember { mutableStateOf("") }
    var selectedCountrySpec by remember { mutableStateOf(COUNTRY_DIAL_SPECS.first()) }
    var phoneDigitsInput by remember { mutableStateOf("") }
    var clubNameInput by remember { mutableStateOf("") }
    var selectedCert by remember { mutableStateOf(COACH_CERTIFICATIONS.first()) }
    var selectedSpecialization by remember { mutableStateOf(SPECIALIZATION_EVENTS.first()) }

    var countryDropdownExpanded by remember { mutableStateOf(false) }
    var certDropdownExpanded by remember { mutableStateOf(false) }
    var specDropdownExpanded by remember { mutableStateOf(false) }

    val fullFormattedPhone = remember(selectedCountrySpec, phoneDigitsInput) {
        val cleaned = phoneDigitsInput.trim().removePrefix("0")
        "${selectedCountrySpec.dialCode}$cleaned"
    }

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

            val resolvedName = fullNameInput.trim().ifBlank {
                firebaseUser?.displayName
                    ?: googleIdTokenCredential.displayName
                    ?: if (isSuper) "Coach Salipar Jipun (Superadmin)" else "Coach (${email.substringBefore("@")})"
            }
            val resolvedClub = clubNameInput.trim().ifBlank {
                if (isSuper) "Psyco Track & Field Elite" else "Athletics Club"
            }
            val resolvedPhone = if (phoneDigitsInput.trim().isNotBlank()) {
                fullFormattedPhone
            } else {
                "${selectedCountrySpec.dialCode}195326399"
            }

            Toast.makeText(
                context,
                "Welcome $resolvedName! (7-Day Free Trial Active)",
                Toast.LENGTH_SHORT
            ).show()

            onLoginSuccess(
                email,
                resolvedName,
                resolvedClub,
                selectedCountrySpec.countryName,
                resolvedPhone,
                selectedCert,
                selectedSpecialization,
                isSuper
            )
        } else {
            authErrorMessage = "Unsupported credential type."
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        TrackDarkNavy,
                        Color(0xFF0F172A),
                        Color(0xFF030712)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // LANGUAGE TOGGLE BAR COMPONENT (BM | EN | JP | ZH)
            LanguageToggleBar(
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange,
                compact = false,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            PsycotimexCircularLogo(
                sizeDp = 88,
                modifier = Modifier.testTag("auth_emblem")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "PSYCO TIME X PRO",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                text = langHook.t("auth_subtitle"),
                fontSize = 11.sp,
                color = SilverMetallic,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // AUTO 7-DAY FREE TRIAL BADGE
            Surface(
                color = Color(0xFF10B981).copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = langHook.t("trial_active"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF10B981)
                        )
                        Text(
                            text = langHook.t("free_trial_banner"),
                            fontSize = 11.sp,
                            color = Color.White,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                    Text(
                        text = langHook.t("auth_title"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // 1. MANDATORY FULL NAME FIELD
                    OutlinedTextField(
                        value = fullNameInput,
                        onValueChange = {
                            fullNameInput = it
                            authErrorMessage = null
                        },
                        label = { Text(langHook.t("field_fullname")) },
                        placeholder = { Text("Cth: Coach Ahmad bin Razak / Tanaka Kenji") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ElectricCyan) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_fullname_input")
                    )

                    // 2. COUNTRY & DIAL CODE PICKER (+60, +62, +65, +81, +94, +86, etc.)
                    ExposedDropdownMenuBox(
                        expanded = countryDropdownExpanded,
                        onExpandedChange = { countryDropdownExpanded = !countryDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = "${selectedCountrySpec.flag} ${selectedCountrySpec.countryName} (${selectedCountrySpec.dialCode})",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(langHook.t("field_country")) },
                            leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, tint = ElectricCyan) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("auth_country_picker")
                        )
                        ExposedDropdownMenu(
                            expanded = countryDropdownExpanded,
                            onDismissRequest = { countryDropdownExpanded = false }
                        ) {
                            COUNTRY_DIAL_SPECS.forEach { spec ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("${spec.flag} ${spec.countryName}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = "${spec.dialCode} • ${if (spec.isMalaysia) "MYR (RM)" else "USD ($)"}",
                                                fontSize = 11.sp,
                                                color = if (spec.isMalaysia) ElectricCyan else LaserOrange,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedCountrySpec = spec
                                        countryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 3. MANDATORY PHONE NUMBER WITH COUNTRY DIAL CODE PREFIX (+60 / +62 / +81 / etc.)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = StadiumSurfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = "${selectedCountrySpec.flag} ${selectedCountrySpec.dialCode}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = ElectricCyan
                                )
                            }
                        }

                        OutlinedTextField(
                            value = phoneDigitsInput,
                            onValueChange = {
                                phoneDigitsInput = it.filter { ch -> ch.isDigit() || ch == '-' || ch == ' ' }
                                authErrorMessage = null
                            },
                            label = { Text(langHook.t("field_phone")) },
                            placeholder = { Text("19-5326399 / 812-345678") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = SilverMetallic) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("auth_phone_input")
                        )
                    }

                    // 4. CLUB / SCHOOL / TEAM NAME
                    OutlinedTextField(
                        value = clubNameInput,
                        onValueChange = { clubNameInput = it },
                        label = { Text(langHook.t("field_club")) },
                        placeholder = { Text("Psyco Track & Field / Sekolah Sukan") },
                        leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = FinishLineRed) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_club_input")
                    )

                    // 5. COACH CERTIFICATION PICKER
                    ExposedDropdownMenuBox(
                        expanded = certDropdownExpanded,
                        onExpandedChange = { certDropdownExpanded = !certDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCert,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(langHook.t("field_cert")) },
                            leadingIcon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = SpeedAmber) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = certDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = certDropdownExpanded,
                            onDismissRequest = { certDropdownExpanded = false }
                        ) {
                            COACH_CERTIFICATIONS.forEach { cert ->
                                DropdownMenuItem(
                                    text = { Text(cert, fontSize = 12.sp) },
                                    onClick = {
                                        selectedCert = cert
                                        certDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 6. SPECIALIZATION EVENT PICKER
                    ExposedDropdownMenuBox(
                        expanded = specDropdownExpanded,
                        onExpandedChange = { specDropdownExpanded = !specDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedSpecialization,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(langHook.t("field_spec")) },
                            leadingIcon = { Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = LaserOrange) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = specDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = specDropdownExpanded,
                            onDismissRequest = { specDropdownExpanded = false }
                        ) {
                            SPECIALIZATION_EVENTS.forEach { spec ->
                                DropdownMenuItem(
                                    text = { Text(spec, fontSize = 12.sp) },
                                    onClick = {
                                        selectedSpecialization = spec
                                        specDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // DYNAMIC SUBSCRIPTION RATE PREVIEW BASED ON SELECTED COUNTRY (MYR vs USD $)
                    val isMy = selectedCountrySpec.isMalaysia
                    Surface(
                        color = StadiumSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isMy) ElectricCyan.copy(alpha = 0.4f) else LaserOrange.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = langHook.t("pricing_header"),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isMy) ElectricCyan else LaserOrange
                                )
                                Surface(
                                    color = if (isMy) ElectricCyan.copy(alpha = 0.2f) else LaserOrange.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (isMy) "🇲🇾 MYR (RM)" else "🌐 USD ($)",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isMy) ElectricCyan else LaserOrange,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                SubscriptionPricing.plans.forEach { plan ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = plan.formattedPrice(selectedCountrySpec.countryName),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${plan.athletesLimit} Atlit",
                                            fontSize = 9.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (authErrorMessage != null) {
                        Surface(
                            color = FinishLineRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FinishLineRed.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = FinishLineRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = authErrorMessage ?: "",
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // GOOGLE SIGN-IN BUTTON (validates mandatory Full Name & Contact Number first)
                    Button(
                        onClick = {
                            if (fullNameInput.trim().length < 2 || phoneDigitsInput.trim().length < 5) {
                                authErrorMessage = langHook.t("err_fullname_phone_required")
                                return@Button
                            }
                            val activityContext = context.findActivity()
                            if (activityContext == null) {
                                authErrorMessage = "Activity context not found."
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
                                    // User dismissed picker
                                } catch (e: NoCredentialException) {
                                    authErrorMessage = "No Google account found on device. Please add a Google account in Android Settings."
                                } catch (e: GetCredentialException) {
                                    authErrorMessage = "Google Sign-In failed: ${e.localizedMessage ?: e.message}"
                                } catch (e: Exception) {
                                    authErrorMessage = "Auth error: ${e.localizedMessage ?: e.message}"
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
                                text = "Connecting Google & Cloud...",
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
                                    Text("G", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = langHook.t("btn_google_signin"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F1F1F)
                            )
                        }
                    }

                    // DIRECT CONTACT ADMIN WHATSAPP (+6019-5326399) FOR REGISTRATION / SUBSCRIPTION HELP
                    OutlinedButton(
                        onClick = {
                            val msg = "Salam Admin Psyco Time X Pro (+6019-5326399), saya memerlukan bantuan pendaftaran / langganan APK.\nNama: ${fullNameInput.ifBlank { "-" }}\nNegara: ${selectedCountrySpec.countryName} (${selectedCountrySpec.dialCode})\nTelefon: $fullFormattedPhone\nKelab: ${clubNameInput.ifBlank { "-" }}"
                            val uri = Uri.parse("https://wa.me/${SubscriptionPricing.ADMIN_WHATSAPP_NUMBER}?text=${Uri.encode(msg)}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF25D366)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("auth_contact_admin_btn")
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = langHook.t("contact_admin_whatsapp"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF25D366)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Psyco Time X Pro Sdn. Bhd. • www.psycotimexpro.my\nAdmin Support WhatsApp: ${SubscriptionPricing.ADMIN_WHATSAPP_DISPLAY}",
                fontSize = 10.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}
