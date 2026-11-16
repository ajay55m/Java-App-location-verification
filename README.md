# 4S-Contracting — Mobile Workforce & Site Attendance Portal

An enterprise Android application built for **4S-Contracting** to manage construction workforce operations, real-time site attendance, and cross-site relocations with tamper-proof GPS geofencing and live camera identity verification.

---

## 🚀 Key Features

### 1. Dual-Track Auto Location & Geofence Verification
- **High-Speed Site Detection**: Runs background GPS acquisition (`FusedLocationProviderClient`) in parallel with site geofence matching to auto-detect the user's active site in under 200ms.
- **Bounding-Box Geofencing**: Computes coordinates against high-precision latitude/longitude bounding boxes with adaptive GPS accuracy buffers.
- **Anti-Mock GPS Protection**: Detects and rejects mock/spoofed GPS providers (`LocationVerifier.isMockLocation`).
- **Seamless Manual Fallback**: If satellite reception is weak or the user is off-site, gracefully provides a searchable dropdown for manual project selection and override.

### 2. Live Geo-Fenced Camera Attendance
- **Anti-Tamper Live Capture**: Enforces real-time live camera capture for worker clock-ins (`LocationActivity`).
- **WAF-Optimized Payload Stream**: Automatically downsamples and compresses captured images to $< 50\text{ KB}$ (JPEG 60%), preventing ModSecurity WAF 406 rejections while preserving facial clarity.
- **PHP Stream Decoding & Sanitization**: Custom binary stream sanitizer (`ImageLoaderHelper`) strips BOM/whitespace/null-byte prefixes to reliably decode live profile streams.

### 3. Workforce & Relocation Management
- **Labour Roster**: Real-time worker cards with status badges (`NOT CLOCKED IN`, `PRESENT`, `LOCKED_OTHER`).
- **Site Movement**: Worker transfers between sites with automated travel-time tracking.
- **Supervisor Self-Attendance**: Dedicated self-punch and historical attendance audit feed.
- **Time Tamper Proofing**: Enforces device automatic network time (`AUTO_TIME`) to prevent timesheet falsification.

### 4. Modern Light-Theme UI
- Modern, clean aesthetic matching the corporate identity (sapphire blue `#03045E`, signature mid-blue `#0077B6`, vibrant cyan `#0284C7`, and light cyan `#E0F2FE`).
- High-contrast roster cards and modern vector profile avatars.

---

## 🛠️ Tech Stack & Architecture

- **Platform**: Native Android (Java 17)
- **Minimum SDK**: Android 5.0 (API 21) | **Target SDK**: Android 14 (API 34)
- **Build System**: Gradle 8.13 (AGP 8.13.2) with Java Desugaring enabled
- **Architecture**: Clean Architecture (Domain, Data, UI, Core)
- **Networking**: Retrofit 2.11 + OkHttp 4.12 & Volley 1.2
- **Location Services**: Google Play Services Location 21.3 (`FusedLocationProviderClient`)
- **Image Pipeline**: Glide 4.16 + Custom Binary Stream Sanitizer + LRU Memory Cache
- **UI Components**: Material Design 3, ConstraintLayout 2.2, ViewBinding
- **Push & Diagnostics**: Firebase Cloud Messaging & Firebase Crashlytics

---

## 📁 Project Directory Structure

```text
4s-contracting/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml          # Core permissions & activity declarations
│   │   │   ├── java/com/app/fourscontracting/
│   │   │   │   ├── core/                    # Network clients, session, date/image utils
│   │   │   │   ├── data/                    # Repositories, APIs, models, image loaders
│   │   │   │   ├── domain/                  # Geofence & mock location verifier engine
│   │   │   │   └── ui/                      # Dashboard, Labour, Move Site, Self-Attendance
│   │   │   └── res/
│   │   │       ├── drawable/                # Themes, vectors, avatar models, card rings
│   │   │       ├── layout/                  # Screen layouts, dialogs, item cards
│   │   │       └── values/                  # Colors, dimensions, typography, styles
│   │   └── test/                            # Unit test suite (106 tests)
│   ├── build.gradle                         # App module configuration & dependencies
│   └── proguard-rules.pro                   # R8/ProGuard rules
├── build.gradle                             # Top-level build configuration
├── settings.gradle                          # Module inclusions
└── README.md                                # Project documentation
```

---

## 🧪 Build & Testing

### Running Unit Tests
Execute the local JUnit unit test suite:
```bash
./gradlew testDebugUnitTest
```

### Building the Debug APK
Generate the testing APK artifact:
```bash
./gradlew assembleDebug
```
The compiled output is located at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📲 Client Testing & Distribution Notice

> [!IMPORTANT]
> **Client Testing Instructions**:
> For client evaluation, distribute the signed APK (`app-debug.apk`) directly or via **Firebase App Distribution** / internal testing track. Clients do **not** need access to the source code repository to test the application.
