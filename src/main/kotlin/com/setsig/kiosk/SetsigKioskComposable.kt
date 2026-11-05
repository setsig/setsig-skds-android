package com.setsig.kiosk

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.setsig.SetsigClient
import com.setsig.model.KioskTokenResponse
import kotlinx.coroutines.delay

/**
 * Self-refreshing dynamic QR terminal display for Android POS, tablet kiosks, and gate terminals.
 */
@Composable
fun SetsigKioskComposable(
    client: SetsigClient,
    venueId: String,
    refreshIntervalSeconds: Int = 60,
    modifier: Modifier = Modifier
) {
    var tokenData by remember { mutableStateOf<KioskTokenResponse?>(null) }
    var secondsRemaining by remember { mutableIntStateOf(refreshIntervalSeconds) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch dynamic token function
    suspend fun fetchToken() {
        isLoading = true
        errorMessage = null
        try {
            val response = client.generateKioskToken(venueId, refreshIntervalSeconds)
            tokenData = response
            secondsRemaining = response.ttlRemaining
        } catch (e: Throwable) {
            errorMessage = e.message ?: "Failed to generate terminal token"
        } finally {
            isLoading = false
        }
    }

    // Countdown and automatic refresh loop
    LaunchedEffect(venueId, refreshIntervalSeconds) {
        fetchToken()
        while (true) {
            delay(1000L)
            if (secondsRemaining <= 1) {
                fetchToken()
            } else {
                secondsRemaining -= 1
            }
        }
    }

    val progress = (secondsRemaining.toFloat() / refreshIntervalSeconds.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "qr_countdown")
    val ringColor = if (secondsRemaining <= 10) Color(0xFFEF4444) else Color(0xFF3B82F6)

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(260.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background track ring
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = Color.DarkGray.copy(alpha = 0.3f),
                strokeWidth = 6.dp
            )

            // Dynamic progress countdown ring
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize(),
                color = ringColor,
                strokeWidth = 6.dp,
                strokeCap = StrokeCap.Round
            )

            // QR Card Container
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .background(Color(0xFF18181B), shape = RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading && tokenData == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = Color.White
                    )
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFF87171),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else if (tokenData != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "[ Dynamic QR Code ]",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tokenData!!.tokenId.take(12),
                            color = Color(0xFFA1A1AA),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Countdown Badge
        Box(
            modifier = Modifier
                .background(Color(0xFF27272A), shape = CircleShape)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "${secondsRemaining}s",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = ringColor
            )
        }
    }
}

