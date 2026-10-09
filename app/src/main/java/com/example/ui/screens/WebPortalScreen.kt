package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.*

/**
 * Tab Web Portal Psyco Time X Pro:
 * Membolehkan pengguna melayari www.psycotimexpro.my terus dari dalam APK
 * untuk visit, mendaftar kejohanan, sports-management, training-management,
 * membeli produk sukan trek & padang atau melihat keputusan live ET.
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebPortalScreen(
    initialUrl: String = "https://www.psycotimexpro.my",
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var canGoBack by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableStateOf(0) }
    var hasError by remember { mutableStateOf(false) }

    // Intercept back navigation
    BackHandler(enabled = canGoBack) {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onNavigateBack()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TrackDarkNavy)
    ) {
        // Quick Navigation Links bar
        Surface(
            color = StadiumSurface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (webViewInstance?.canGoBack() == true) {
                                    webViewInstance?.goBack()
                                } else {
                                    onNavigateBack()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Undur", tint = Color.White)
                        }

                        IconButton(
                            onClick = { webViewInstance?.reload() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Muat Semula", tint = ElectricCyan)
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "www.psycotimexpro.my",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = currentUrl.take(45),
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    // Open Home
                    FilledTonalButton(
                        onClick = {
                            currentUrl = "https://www.psycotimexpro.my"
                            webViewInstance?.loadUrl(currentUrl)
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = StadiumSurfaceVariant),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp), tint = ElectricCyan)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Home", fontSize = 11.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Direct shortcut chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickWebChip("Latihan ET", "https://www.psycotimexpro.my/training-management") { url ->
                        currentUrl = url
                        webViewInstance?.loadUrl(url)
                    }
                    QuickWebChip("Kejohanan", "https://www.psycotimexpro.my/sports-management") { url ->
                        currentUrl = url
                        webViewInstance?.loadUrl(url)
                    }
                    QuickWebChip("Kedai Produk", "https://www.psycotimexpro.my/kedai") { url ->
                        currentUrl = url
                        webViewInstance?.loadUrl(url)
                    }
                    QuickWebChip("Result Live", "https://www.psycotimexpro.my/result-et") { url ->
                        currentUrl = url
                        webViewInstance?.loadUrl(url)
                    }
                }
            }
        }

        // Progress indicator
        if (isLoading) {
            LinearProgressIndicator(
                progress = { loadProgress / 100f },
                color = ElectricCyan,
                trackColor = StadiumSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // WebView or Offline fallback card
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadProgress = newProgress
                                isLoading = newProgress < 100
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                hasError = false
                                url?.let { currentUrl = it }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                canGoBack = view?.canGoBack() == true
                                url?.let { currentUrl = it }
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                if (request?.isForMainFrame == true) {
                                    hasError = true
                                    isLoading = false
                                }
                            }
                        }

                        loadUrl(currentUrl)
                        webViewInstance = this
                    }
                },
                update = { view ->
                    webViewInstance = view
                },
                modifier = Modifier.fillMaxSize().testTag("web_portal_view")
            )

            // Offline / Pre-deployment informative view
            if (hasError) {
                Surface(
                    color = StadiumSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .align(Alignment.Center)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Portal Web Psyco Time X Pro",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Halaman disasarkan ke: $currentUrl\n\nSemua fail web telah disediakan di folder htdocs/ dan sedia untuk dihubungkan secara langsung ke domain www.psycotimexpro.my pada hosting pelayan anda.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                hasError = false
                                webViewInstance?.reload()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                        ) {
                            Text("Cuba Lagi", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickWebChip(label: String, url: String, onSelect: (String) -> Unit) {
    FilterChip(
        selected = false,
        onClick = { onSelect(url) },
        label = { Text(label, fontSize = 10.sp) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = StadiumSurfaceVariant,
            labelColor = Color.White
        )
    )
}
