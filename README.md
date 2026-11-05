# setsig Android SDK (`setsig-android`)

Official Android SDK for setsig.com physical presence verification, dynamic kiosk terminals, and Google Play Integrity hardware attestation.

## Installation

### Gradle (Kotlin DSL)

Add the JitPack or MavenCentral repository to your root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = java.net.URI("https://jitpack.io") }
    }
}
```

Add the dependency to your app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.setsig:setsig-android:1.0.0")
    
    // Coroutines & Play Integrity (optional but recommended for attestation)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("com.google.android.play:integrity:1.3.0")
}
```

## Quick Start

### 1. Initialize Client

```kotlin
import com.setsig.SetsigClient

val client = SetsigClient(
    apiKey = "sk_live_...",
    baseUrl = "https://api.setsig.com"
)
```

### 2. Verify Physical Presence (Client/User Device)

```kotlin
import com.setsig.model.VerifySessionRequest
import com.setsig.integrity.PlayIntegrityHelper

// 1. (Optional) Request Google Play Integrity hardware attestation
val attestationToken = PlayIntegrityHelper.requestIntegrityToken(
    context = appContext,
    cloudProjectNumber = 123456789012L,
    clientToken = "usr_99812",
    challenge = scannedTokenId
)

// 2. Submit verification to setsig Core Engine
val result = client.verifyPresence(
    VerifySessionRequest(
        venueId = "ven_hq_ist_001",
        tokenId = scannedTokenId,
        clientToken = "usr_99812",
        latitude = 41.0082,
        longitude = 28.9784,
        accuracy = 4.2,
        attestationToken = attestationToken
    )
)

println("Verified: ${result.status}, Distance: ${result.distanceMeters}m")
```

### 3. Dynamic Kiosk Display (Jetpack Compose)

Embed a self-refreshing dynamic QR kiosk terminal into your POS, tablet, or gate terminal:

```kotlin
import androidx.compose.runtime.Composable
import com.setsig.kiosk.SetsigKioskComposable

@Composable
fun GateKioskScreen(setsigClient: SetsigClient) {
    SetsigKioskComposable(
        client = setsigClient,
        venueId = "ven_hq_ist_001",
        refreshIntervalSeconds = 60
    )
}
```

## License

Apache-2.0

