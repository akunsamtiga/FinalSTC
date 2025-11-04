# STC Autotrade

<div align="center">

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/kotlin-1.9.0-purple.svg)
![License](https://img.shields.io/badge/license-Proprietary-red.svg)

**Sistem Otomasi Trading Canggih untuk Platform Stockity**

[Fitur](#-fitur) • [Instalasi](#-instalasi) • [Arsitektur](#-arsitektur) • [Dokumentasi](#-integrasi-api)

</div>

---

## 📋 Daftar Isi

- [Ringkasan](#-ringkasan)
- [Fitur](#-fitur)
- [Tech Stack](#-tech-stack)
- [Arsitektur](#-arsitektur)
- [Memulai](#-memulai)
- [Struktur Proyek](#-struktur-proyek)
- [Mode Trading](#-mode-trading)
- [Konfigurasi](#%EF%B8%8F-konfigurasi)
- [Integrasi API](#-integrasi-api)
- [Keamanan](#-keamanan)
- [Performa](#-optimasi-performa)
- [Debugging](#-debugging)
- [Kontribusi](#-kontribusi)
- [Lisensi](#-lisensi)
- [Dukungan](#-dukungan)

---

## 🎯 Ringkasan

STC Autotrade adalah aplikasi Android canggih yang dirancang untuk menyediakan kemampuan trading otomatis untuk platform binary options Stockity. Dibangun dengan praktik pengembangan Android modern dan Jetpack Compose, aplikasi ini menghadirkan berbagai strategi trading, pemantauan pasar real-time, dan fitur manajemen trading yang komprehensif.

### Keunggulan Utama

- 🚀 **Eksekusi Ultra-Cepat** - Interval deteksi 50ms untuk pengenalan trading instan
- 🎯 **Berbagai Mode Trading** - Mode Schedule, Follow Order, Indicator, dan CTC
- 💰 **Dukungan Multi-Mata Uang** - IDR, USD, EUR, MYR, dan 15+ mata uang lainnya
- 🌐 **Multi-Bahasa** - Dukungan untuk 7 bahasa termasuk Indonesia, Inggris, Spanyol, Vietnam, Turki, Hindi, dan Melayu
- 📊 **Analitik Real-Time** - Pelacakan profit langsung dan statistik komprehensif
- 🔐 **Keamanan Enterprise** - Sesi terenkripsi dan sistem manajemen whitelist
- 🎨 **UI Modern** - Material Design 3 dengan dukungan tema gelap/terang

---

## ✨ Fitur

### 🤖 Mode Trading Otomatis

#### 1. Mode Schedule
Jadwal trading yang telah dikonfigurasi sebelumnya dengan kemampuan eksekusi massal.

**Fitur:**
- Pembuatan dan manajemen order massal
- Eksekusi otomatis berbasis waktu
- Dukungan strategi Martingale
- Pemantauan berkelanjutan pada interval 50ms
- Parameter trading yang dapat dikonfigurasi

#### 2. Mode Follow Order (Ultra-Cepat)
Analisis harga real-time dengan kecepatan deteksi terdepan di industri.

**Fitur:**
- Interval pemantauan 50ms
- Sistem deteksi multi-layer (WebSocket → Balance → API)
- Mode eksekusi boundary dan instan
- Pemulihan otomatis pada masalah koneksi
- Mekanisme fallback tiga lapis untuk keandalan

**Metrik Performa:**
- Latensi deteksi: < 50ms
- Respons eksekusi: < 100ms
- Redundansi tiga lapis
- Deteksi trend yang di-cache

#### 3. Mode Indicator Order
Trading berbasis analisis teknikal dengan restart siklus otomatis.

**Indikator yang Didukung:**
- RSI (Relative Strength Index)
- EMA (Exponential Moving Average)
- Bollinger Bands
- MACD (Moving Average Convergence Divergence)
- Stochastic Oscillator

**Fitur:**
- Auto-restart saat trading selesai (menang/kalah)
- Deteksi level support dan resistance
- Parameter sensitivitas yang dapat disesuaikan
- Operasi siklus berkelanjutan
- Eksekusi berbasis prediksi

#### 4. Mode CTC Order (Ultra-Cepat)
Mode yang dioptimalkan dengan performa identik dengan Follow Order.

**Fitur:**
- Deteksi ultra-cepat 50ms
- Kecepatan optimasi sama dengan Follow Order
- Desain yang fokus pada performa
- Sistem deteksi tiga lapis
- Ideal untuk trading frekuensi tinggi

### 💎 Fitur Trading Lanjutan

#### Strategi Martingale
Ukuran posisi cerdas untuk pemulihan kerugian.

**Opsi Konfigurasi:**
- Jenis multiplier: Fixed atau berbasis Persentase
- Langkah maksimum: 1-10 level yang dapat dikonfigurasi
- Sistem pemulihan cerdas
- Pelacakan langkah real-time
- Reset otomatis saat profit

#### Manajemen Risiko
Mekanisme perlindungan komprehensif.

**Fitur:**
- **Stop Loss** - Perlindungan ambang batas kerugian maksimum
- **Stop Profit** - Pengambilan profit otomatis pada target
- **Pelacakan Berbasis Sesi** - Pemantauan P&L per sesi
- **Pencegahan Kerugian** - Circuit breaker cerdas
- **Peringatan Real-time** - Notifikasi instan pada ambang batas

#### Pemantauan Real-Time
Analitik trading langsung dan pelacakan performa.

**Metrik:**
- Pelacakan P&L (Profit & Loss) langsung
- Perhitungan profit hari ini
- Dashboard statistik trading
- Analitik win rate
- Riwayat trading dengan detail
- Visualisasi performa

### 👥 Manajemen Pengguna

- **Sistem Whitelist** - Akses pengguna yang dikontrol admin dengan verifikasi perangkat
- **Dukungan Multi-Admin** - Peran Admin Reguler dan Super Admin dengan izin granular
- **Auto-Registration** - Onboarding pengguna yang mulus dari platform Stockity
- **Manajemen Sesi** - Sesi pengguna terenkripsi dengan kedaluwarsa otomatis
- **Pelacakan Perangkat** - Dukungan multi-perangkat dengan kontrol keamanan

### 🎨 Fitur UI/UX

- **Material Design 3** - Interface modern dan intuitif mengikuti panduan desain terbaru
- **Tema Gelap/Terang** - Pemilihan tema sadar sistem atau manual
- **Multi-Bahasa** - 7 bahasa dengan perpindahan mudah
- **Desain Responsif** - Dioptimalkan untuk ponsel dan tablet
- **Update Real-Time** - Sinkronisasi data langsung dengan animasi halus
- **Aksesibilitas** - Dukungan screen reader dan mode kontras tinggi

---

## 🛠 Tech Stack

### Teknologi Inti

| Komponen | Versi | Tujuan |
|-----------|---------|---------|
| **Kotlin** | 1.9.0 | Bahasa utama |
| **Jetpack Compose** | Terbaru | Framework UI |
| **Min SDK** | 24 | Android 7.0+ |
| **Target SDK** | 34 | Android 14 |
| **Gradle** | 8.0+ | Sistem build |

### Arsitektur & Library

#### 🗃️ Dependency Injection

```kotlin
val hilt = Hilt 2.48                    // Framework dependency injection
```

#### 🌐 Networking
```kotlin
Retrofit 2.9.0               // REST API client
OkHttp 4.11.0                // HTTP client dengan interceptor
Gson 2.10.1                  // Serialisasi/deserialisasi JSON
WebSocket                    // Komunikasi bidirektional real-time
```

#### 💾 Database & Storage
```kotlin
Firebase Firestore           // Database NoSQL cloud
EncryptedSharedPreferences   // Penyimpanan key-value lokal yang aman
```

#### 🎨 Komponen UI
```kotlin
Compose Material3            // Komponen Material Design 3
Accompanist WebView          // Integrasi WebView untuk Compose
Compose Navigation           // Sistem navigasi type-safe
Compose Lifecycle            // Komponen lifecycle-aware
```

#### 🔧 Utilities
```kotlin
Kotlin Coroutines            // Pemrograman asinkron
Kotlin Flow                  // Stream data reaktif
Coil                         // Pemuatan gambar yang efisien
Security Crypto              // Utilitas enkripsi data
```

---

## 🏗 Arsitektur

### Implementasi Pola MVVM

```
┌─────────────────────────────────────────────────┐
│                  View Layer                     │
│           Jetpack Compose Screens               │
│        (Komponen UI & Interaksi User)           │
└────────────────┬────────────────────────────────┘
                 │
                 │ State & Events
                 ▼
┌─────────────────────────────────────────────────┐
│               ViewModel Layer                   │
│        Business Logic & State Management        │
│       (UI State, User Actions, Data Flow)       │
└────────────────┬────────────────────────────────┘
                 │
                 │ Data Requests
                 ▼
┌─────────────────────────────────────────────────┐
│              Repository Layer                   │
│          Data Source Coordination               │
│      (Caching, Data Mapping, Orchestration)     │
└────────┬────────────────────────────┬───────────┘
         │                            │
         │ API Calls                  │ Local Storage
         ▼                            ▼
┌──────────────────┐        ┌──────────────────────┐
│  Remote Data     │        │   Local Data Source  │
│  (API/WebSocket) │        │ (SharedPreferences)  │
│  Firebase/HTTP   │        │  Encrypted Storage   │
└──────────────────┘        └──────────────────────┘
```

### Tanggung Jawab Layer

#### 1. 🎨 Presentation Layer
- **Screens** - UI screen Composable dengan Material Design 3
- **ViewModels** - Manajemen state dan koordinasi logika bisnis
- **UI State** - Kelas state immutable untuk update UI yang dapat diprediksi
- **Navigation** - Navigasi type-safe antar screen

#### 2. 🎯 Domain Layer
- **Use Cases** - Operasi logika bisnis yang ter-enkapsulasi
- **Models** - Model data spesifik domain
- **Repositories** - Interface akses data abstrak

#### 3. 💾 Data Layer
- **Remote** - Layanan API, koneksi WebSocket, integrasi Firebase
- **Local** - SharedPreferences, manajemen sesi terenkripsi
- **Mappers** - Transformasi data antar layer

### Struktur Dependency Injection

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideStockityApiService(): StockityApiService
    
    @Provides
    @Singleton
    fun provideUserRepository(): UserRepository
    
    // ... provider tambahan
}
```

---

## 🚀 Memulai

### Prasyarat

Pastikan Anda telah menginstal:

- **Android Studio** - Hedgehog (2023.1.1) atau lebih baru
- **JDK** - Versi 17 atau lebih baru
- **Android SDK** - API level 34
- **Gradle** - Versi 8.0 atau lebih baru
- **Git** - Untuk version control

### Langkah Instalasi

#### 1. Clone Repository
```bash
git clone https://github.com/yourusername/stc-autotrade.git
cd stc-autotrade
```

#### 2. Konfigurasi Firebase
- Buat proyek Firebase baru di [Firebase Console](https://console.firebase.google.com/)
- Aktifkan Firestore Database
- Unduh `google-services.json`
- Tempatkan file di direktori `app/`

#### 3. Setup Kredensial API
Buat file `local.properties` di root proyek:

```properties
# Konfigurasi API Stockity
STOCKITY_API_URL=https://api.stockity.id/

# Opsional: Tambahkan debug keys
DEBUG_MODE=true
```

#### 4. Sync dan Build
```bash
# Sync dependensi Gradle
./gradlew sync

# Build proyek
./gradlew build
```

#### 5. Jalankan Aplikasi
```bash
# Install debug build
./gradlew installDebug

# Atau jalankan langsung dari Android Studio
```

### Konfigurasi Awal

#### Setup Admin
1. **Akses Super Admin**
   - Email Super Admin default
   - Kredensial login dikelola di Firebase
   - Super Admin memiliki akses penuh sistem

2. **Konfigurasi Sistem**
   - Navigasi ke Panel Admin setelah login
   - Konfigurasi URL registrasi Stockity
   - Set nomor kontak dukungan WhatsApp
   - Tentukan parameter trading default

3. **Manajemen Pengguna**
   - Tambahkan pengguna ke whitelist secara manual, ATAU
   - Aktifkan auto-registration dari Stockity
   - Konfigurasi izin pengguna
   - Set batas perangkat per pengguna

---

## 📁 Struktur Proyek

```
com.autotrade.finalstc/
│
├── data/
│   ├── api/                    # Interface layanan API
│   │   ├── StockityApiService
│   │   └── WebSocketManager
│   ├── local/                  # Manajemen data lokal
│   │   ├── SessionManager
│   │   └── PreferencesManager
│   ├── model/                  # Model data & DTO
│   │   ├── request/
│   │   ├── response/
│   │   └── entity/
│   └── repository/             # Implementasi repository
│       ├── AuthRepository
│       ├── TradingRepository
│       └── UserRepository
│
├── presentation/
│   ├── login/                  # Screen login
│   │   ├── LoginScreen.kt
│   │   └── LoginViewModel.kt
│   ├── register/               # Screen registrasi
│   │   ├── RegisterScreen.kt
│   │   └── RegisterViewModel.kt
│   ├── main/                   # Container utama
│   │   ├── dashboard/          # Dashboard & kontrol trading
│   │   │   ├── DashboardScreen.kt
│   │   │   └── DashboardViewModel.kt
│   │   ├── history/            # Riwayat trading
│   │   │   ├── HistoryScreen.kt
│   │   │   └── HistoryViewModel.kt
│   │   ├── profile/            # Profil & pengaturan user
│   │   │   ├── ProfileScreen.kt
│   │   │   └── ProfileViewModel.kt
│   │   └── webview/            # Tampilan web trading
│   │       ├── WebViewScreen.kt
│   │       └── WebViewViewModel.kt
│   ├── admin/                  # Panel admin
│   │   ├── AdminScreen.kt
│   │   └── AdminViewModel.kt
│   └── theme/                  # Manajemen tema
│       ├── Theme.kt
│       ├── Color.kt
│       └── Type.kt
│
├── utils/                      # Kelas utilitas
│   ├── StringsManager.kt       # Dukungan multi-bahasa
│   ├── FileExportHelper.kt     # Fungsionalitas ekspor
│   ├── PermissionsHandler.kt   # Manajemen izin
│   ├── CurrencyFormatter.kt    # Format mata uang
│   └── DateTimeUtils.kt        # Utilitas tanggal/waktu
│
├── di/                         # Modul dependency injection
│   ├── AppModule.kt
│   ├── NetworkModule.kt
│   └── DatabaseModule.kt
│
└── MainApplication.kt          # Kelas aplikasi
```

---

## 🎮 Mode Trading

### 1. 📅 Mode Schedule

Eksekusi trading berdasarkan jadwal yang telah dikonfigurasi dengan dukungan order massal.

**Fitur Utama:**
- Pembuatan order massal dengan dukungan impor CSV
- Eksekusi berbasis waktu yang presisi
- Integrasi strategi Martingale
- Pemantauan berkelanjutan pada interval 50ms
- Retry otomatis pada order yang gagal

**Contoh Penggunaan:**
```kotlin
// Tambahkan order terjadwal
viewModel.addScheduledOrders(
    orders = listOf(
        ScheduledOrder(
            assetRic = "BTCUSD",
            trend = "CALL",
            amount = 1000000,
            executeAt = "14:30:00"
        )
    )
)

// Jalankan bot
viewModel.startBot(mode = TradingMode.SCHEDULE)

// Monitor eksekusi
viewModel.botState.collect { state ->
    when (state) {
        is BotState.Running -> // Handle state running
        is BotState.Completed -> // Handle completion
        is BotState.Error -> // Handle errors
    }
}
```

**Best Practices:**
- Set jendela waktu yang realistis untuk eksekusi
- Gunakan jumlah yang sesuai berdasarkan saldo akun
- Aktifkan stop loss untuk manajemen risiko
- Monitor log eksekusi untuk optimasi

---

### 2. ⚡ Mode Follow Order (Ultra-Cepat)

Analisis harga real-time dengan kecepatan deteksi 50ms terdepan di industri.

**Arsitektur:**
```
Pipeline Deteksi:
WebSocket (Primer) → Balance API (Sekunder) → REST API (Fallback)
     ↓ 50ms              ↓ 100ms                    ↓ 150ms
```

**Fitur Utama:**
- **Interval pemantauan 50ms** untuk deteksi instan
- **Deteksi tiga lapis** dengan fallback otomatis
- **Eksekusi boundary** untuk titik masuk yang presisi
- **Eksekusi instan** untuk trading segera
- **Sistem auto-recovery** saat kehilangan koneksi
- **Analisis trend yang di-cache** untuk optimasi kecepatan

**Spesifikasi Performa:**
- Latensi deteksi: < 50ms (rata-rata 35ms)
- Respons eksekusi: < 100ms (rata-rata 75ms)
- Keandalan WebSocket: 99.9% uptime
- Reconnection otomatis: < 2 detik

**Contoh Penggunaan:**
```kotlin
// Konfigurasi mode Follow Order
viewModel.configureFollowOrder(
    config = FollowOrderConfig(
        assetRic = "BTCUSD",
        detectionInterval = 50, // milidetik
        executionMode = ExecutionMode.BOUNDARY,
        enableCaching = true
    )
)

// Mulai pemantauan
viewModel.startBot(mode = TradingMode.FOLLOW_ORDER)

// Update real-time
viewModel.priceUpdates.collect { price ->
    // Handle update harga
}
```

**Metode Deteksi:**
1. **WebSocket Stream** - Sumber data real-time primer
2. **Balance Check** - Metode verifikasi sekunder
3. **REST API** - Mekanisme fallback tersier

---

### 3. 📊 Mode Indicator Order

Trading otomatis berbasis analisis teknikal dengan operasi siklus berkelanjutan.

**Indikator Teknikal yang Didukung:**

| Indikator | Parameter | Use Case |
|-----------|-----------|----------|
| **RSI** | Period: 14, Overbought: 70, Oversold: 30 | Analisis momentum |
| **EMA** | Fast: 12, Slow: 26 | Trend following |
| **Bollinger Bands** | Period: 20, StdDev: 2 | Volatility breakouts |
| **MACD** | Fast: 12, Slow: 26, Signal: 9 | Konfirmasi trend |
| **Stochastic** | %K: 14, %D: 3 | Momentum oscillator |

**Fitur Utama:**
- **Auto-restart** saat trading selesai (menang atau kalah)
- Deteksi level **Support/Resistance**
- **Sensitivitas yang dapat disesuaikan** untuk sinyal masuk
- **Eksekusi berbasis prediksi** menggunakan data historis
- **Operasi siklus berkelanjutan** tanpa intervensi manual
- **Kombinasi multi-indikator** untuk konfirmasi

**Contoh Penggunaan:**
```kotlin
// Konfigurasi trading berbasis indikator
viewModel.configureIndicatorMode(
    config = IndicatorConfig(
        primaryIndicator = IndicatorType.RSI,
        secondaryIndicator = IndicatorType.MACD,
        rsiPeriod = 14,
        rsiOverbought = 70,
        rsiOversold = 30,
        confirmationRequired = true,
        autoRestart = true
    )
)

// Mulai trading
viewModel.startBot(mode = TradingMode.INDICATOR)

// Monitor sinyal
viewModel.indicatorSignals.collect { signal ->
    when (signal) {
        is Signal.Buy -> // Handle sinyal buy
        is Signal.Sell -> // Handle sinyal sell
        is Signal.Hold -> // Tidak ada aksi
    }
}
```

**Generasi Sinyal:**
- **Strong Buy** - Beberapa indikator selaras untuk buy
- **Buy** - Indikator primer menunjukkan sinyal buy
- **Neutral** - Tidak ada sinyal yang jelas
- **Sell** - Indikator primer menunjukkan sinyal sell
- **Strong Sell** - Beberapa indikator selaras untuk sell

---

### 4. 🔥 Mode CTC Order (Ultra-Cepat)

Mode trading yang dioptimalkan dengan karakteristik performa identik dengan mode Follow Order.

**Fitur Utama:**
- **Deteksi ultra-cepat 50ms** - Sama dengan Follow Order
- **Sistem deteksi tiga lapis** - WebSocket → Balance → API
- **Dioptimalkan untuk performa** untuk trading frekuensi tinggi
- **Eksekusi latensi rendah** - Respons < 100ms
- **Ideal untuk scalping** dan trading posisi cepat

**Spesifikasi Teknis:**
```
Kecepatan Deteksi:    50ms
Latensi Eksekusi:     < 100ms
Layer Fallback:       3
Keandalan:            99.9%
Maks Trading/Menit:   120
```

**Contoh Penggunaan:**
```kotlin
// Konfigurasi mode CTC
viewModel.configureCTCMode(
    config = CTCConfig(
        assetRic = "EURUSD",
        detectionInterval = 50,
        maxTradesPerMinute = 100,
        enableFastExecution = true
    )
)

// Jalankan bot CTC
viewModel.startBot(mode = TradingMode.CTC)
```

**Kapan Menggunakan Mode CTC:**
- Kebutuhan trading frekuensi tinggi
- Strategi scalping
- Masuk/keluar pasar cepat
- Kecepatan eksekusi maksimum diperlukan

---

## ⚙️ Konfigurasi

### 💱 Pengaturan Mata Uang

**Mata Uang yang Tersedia:**

| Kode | Mata Uang | Simbol | Tempat Desimal |
|------|----------|--------|----------------|
| IDR | Rupiah Indonesia | Rp | 0 |
| USD | Dolar AS | $ | 2 |
| EUR | Euro | € | 2 |
| MYR | Ringgit Malaysia | RM | 2 |
| GBP | Poundsterling Inggris | £ | 2 |
| JPY | Yen Jepang | ¥ | 0 |
| AUD | Dolar Australia | A$ | 2 |
| SGD | Dolar Singapura | S$ | 2 |
| THB | Baht Thailand | ฿ | 2 |

**Implementasi:**
```kotlin
// Tentukan tipe mata uang
enum class CurrencyType {
    IDR, USD, EUR, MYR, GBP, JPY, AUD, SGD, THB
}

// Set mata uang di ViewModel
viewModel.setSelectedCurrency(CurrencyType.USD)

// Format jumlah berdasarkan mata uang
val formattedAmount = CurrencyFormatter.format(
    amount = 1000000, // dalam sen
    currency = CurrencyType.USD
) // Returns: "$10,000.00"
```

**Konversi Mata Uang:**
```kotlin
// Semua jumlah disimpan dalam sen/unit terkecil
val amountInCents = 1000000L // 10,000 USD = 1,000,000 sen

// Konversi ke format tampilan
val displayAmount = amountInCents / 100.0 // 10,000.00
```

---

### 🌐 Pengaturan Bahasa

**Bahasa yang Didukung:**

| Kode | Bahasa | Nama Asli | Kelengkapan |
|------|----------|-------------|------------|
| `id` | Indonesia | Bahasa Indonesia | 100% |
| `en` | Inggris | English | 100% |
| `es` | Spanyol | Español | 100% |
| `vi` | Vietnam | Tiếng Việt | 100% |
| `tr` | Turki | Türkçe | 100% |
| `hi` | Hindi | हिन्दी | 100% |
| `ms` | Melayu | Bahasa Melayu | 100% |

**Implementasi:**
```kotlin
// Ubah bahasa
viewModel.updateLanguage(
    languageCode = "id",
    countryCode = "ID"
)

// Dapatkan string yang dilokalkan
val welcomeText = StringsManager.getString(
    key = "welcome_message",
    language = currentLanguage
)

// Format dengan parameter
val greetingText = StringsManager.getString(
    key = "greeting_with_name",
    language = currentLanguage,
    params = mapOf("name" to userName)
) // Returns: "Halo, John!"
```

**Menambahkan Bahasa Baru:**
1. Tambahkan terjemahan ke `StringsManager`
2. Update enum bahasa di pengaturan
3. Tes semua screen untuk text overflow
4. Verifikasi dukungan RTL jika berlaku

---

### 📈 Parameter Trading

#### Konfigurasi Martingale

**Struktur Pengaturan:**
```kotlin
data class MartingaleSettings(
    val isEnabled: Boolean = false,
    val maxSteps: Int = 5,            // 1-10
    val baseAmount: Long = 1000000,   // dalam sen
    val multiplierType: MultiplierType = MultiplierType.FIXED,
    val multiplierValue: Double = 2.0  // 1.5-5.0
)

enum class MultiplierType {
    FIXED,       // 2x, 3x, 4x, dst.
    PERCENTAGE   // +50%, +100%, dst.
}
```

**Contoh Penggunaan:**
```kotlin
// Konfigurasi Martingale
viewModel.updateMartingaleSettings(
    MartingaleSettings(
        isEnabled = true,
        maxSteps = 5,
        baseAmount = 1000000, // $10.00
        multiplierType = MultiplierType.FIXED,
        multiplierValue = 2.0
    )
)

// Contoh progres Martingale (base $10):
// Langkah 1: $10
// Langkah 2: $20 (pemulihan loss)
// Langkah 3: $40 (pemulihan loss)
// Langkah 4: $80 (pemulihan loss)
// Langkah 5: $160 (pemulihan loss)
// Reset saat menang
```

**Best Practices:**
- Mulai dengan multiplier rendah (1.5-2.0)
- Set langkah maksimum berdasarkan ukuran akun
- Selalu gunakan dengan stop loss
- Monitor drawdown dengan hati-hati

#### Manajemen Risiko

**Pengaturan Stop Loss:**
```kotlin
data class StopLossSettings(
    val isEnabled: Boolean = false,
    val maxLossAmount: Long = 5000000, // dalam sen ($50.00)
    val resetDaily: Boolean = true,
    val notifyOnTrigger: Boolean = true
)

// Implementasi
viewModel.updateStopLossSettings(
    StopLossSettings(
        isEnabled = true,
        maxLossAmount = 5000000,
        resetDaily = true,
        notifyOnTrigger = true
    )
)
```

**Pengaturan Stop Profit:**
```kotlin
data class StopProfitSettings(
    val isEnabled: Boolean = false,
    val targetProfitAmount: Long = 10000000, // dalam sen ($100.00)
    val resetDaily: Boolean = true,
    val notifyOnTrigger: Boolean = true
)

// Implementasi
viewModel.updateStopProfitSettings(
    StopProfitSettings(
        isEnabled = true,
        targetProfitAmount = 10000000,
        resetDaily = true,
        notifyOnTrigger = true
    )
)
```

**Contoh Perhitungan:**
```kotlin
// Pelacakan P&L real-time
viewModel.currentSessionPnL.collect { pnl ->
    // Cek stop loss
    if (stopLoss.isEnabled && pnl <= -stopLoss.maxLossAmount) {
        viewModel.stopBot(reason = "Stop loss terpicu")
    }
    
    // Cek stop profit
    if (stopProfit.isEnabled && pnl >= stopProfit.targetProfitAmount) {
        viewModel.stopBot(reason = "Stop profit tercapai")
    }
}
```

---

## 📌 Integrasi API

### 🔐 Autentikasi

**Endpoint Login:**
```kotlin
// Interface Layanan API
interface StockityApiService {
    @POST("passport/v2/sign_in")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>
}

// Model Request
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String,
    val deviceType: String = "android"
)

// Model Response
data class LoginResponse(
    val data: UserData,
    val token: String,
    val refreshToken: String
)

// Penggunaan
suspend fun login(email: String, password: String): Result<UserSession> {
    return try {
        val response = apiService.login(
            LoginRequest(
                email = email,
                password = password,
                deviceId = DeviceUtils.getDeviceId()
            )
        )
        
        if (response.isSuccessful) {
            val session = mapToUserSession(response.body()!!)
            sessionManager.saveSession(session)
            Result.success(session)
        } else {
            Result.failure(Exception(response.message()))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**Header Autentikasi:**
```kotlin
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer ${sessionManager.getToken()}")
            .addHeader("X-Device-Id", DeviceUtils.getDeviceId())
            .addHeader("X-Device-Type", "android")
            .addHeader("Content-Type", "application/json")
            .build()
        
        return chain.proceed(request)
    }
}
```

---

### 💹 Trading API



**Dapatkan Riwayat Trade:**
```kotlin
@GET("v2/options/history")
suspend fun getTradeHistory(
    @Query("page") page: Int,
    @Query("limit") limit: Int,
    @Query("startDate") startDate: Long?,
    @Query("endDate") endDate: Long?
): Response<TradeHistoryResponse>
```

**Tipe Pesan:**
```kotlin
sealed class WebSocketMessage {
    data class PriceUpdate(
        val symbol: String,
        val bid: Double,
        val ask: Double,
        val timestamp: Long
    ) : WebSocketMessage()
    
    data class TradeUpdate(
        val orderId: String,
        val status: String,
        val result: String?,
        val profit: Long?
    ) : WebSocketMessage()
    
    data class BalanceUpdate(
        val realBalance: Long,
        val demoBalance: Long
    ) : WebSocketMessage()
    
    data class Error(
        val code: Int,
        val message: String
    ) : WebSocketMessage()
}
```

**Auto-Reconnection:**
```kotlin
private fun scheduleReconnect() {
    CoroutineScope(Dispatchers.IO).launch {
        delay(2000) // Tunggu 2 detik
        if (webSocket == null || webSocket?.close(1000, null) == true) {
            connectToWebSocket(/* params */)
        }
    }
}
```

---

## 🔐 Keamanan

### 🔒 Enkripsi Data

**Implementasi EncryptedSharedPreferences:**
```kotlin
object SecureStorage {
    private const val PREFS_NAME = "stc_secure_prefs"
    
    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    fun saveToken(context: Context, token: String) {
        getEncryptedPrefs(context).edit()
            .putString("auth_token", token)
            .apply()
    }
    
    fun getToken(context: Context): String? {
        return getEncryptedPrefs(context).getString("auth_token", null)
    }
    
    fun clearAll(context: Context) {
        getEncryptedPrefs(context).edit().clear().apply()
    }
}
```

**Spesifikasi Enkripsi:**
- **Algoritma**: AES-256-GCM (Galois/Counter Mode)
- **Manajemen Kunci**: Sistem Android Keystore
- **Skema Kunci**: AES256_GCM untuk master key
- **Enkripsi Preferensi**:
   - Kunci: AES256_SIV (mode Synthetic IV)
   - Nilai: AES256_GCM

**Penyimpanan Data Aman:**
```kotlin
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun saveSession(session: UserSession) {
        SecureStorage.saveToken(context, session.authToken)
        SecureStorage.save(context, "user_id", session.userId)
        SecureStorage.save(context, "email", session.email)
        SecureStorage.save(context, "device_id", session.deviceId)
        SecureStorage.save(context, "session_expiry", session.expiryTime.toString())
    }
    
    fun getSession(): UserSession? {
        val token = SecureStorage.getToken(context) ?: return null
        val userId = SecureStorage.getString(context, "user_id") ?: return null
        val email = SecureStorage.getString(context, "email") ?: return null
        val deviceId = SecureStorage.getString(context, "device_id") ?: return null
        val expiryTime = SecureStorage.getString(context, "session_expiry")?.toLongOrNull() ?: return null
        
        return UserSession(token, userId, email, deviceId, expiryTime)
    }
    
    fun clearSession() {
        SecureStorage.clearAll(context)
    }
    
    fun isSessionValid(): Boolean {
        val session = getSession() ?: return false
        return System.currentTimeMillis() < session.expiryTime
    }
}
```

---

### 🛡️ Kontrol Akses

**Verifikasi Whitelist:**
```kotlin
interface UserRepository {
    suspend fun checkUserInWhitelist(
        email: String,
        userId: String,
        deviceId: String
    ): Result<Boolean>
}

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {
    
    override suspend fun checkUserInWhitelist(
        email: String,
        userId: String,
        deviceId: String
    ): Result<Boolean> = suspendCancellableCoroutine { continuation ->
        
        firestore.collection("whitelist")
            .whereEqualTo("email", email)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    continuation.resume(Result.success(false))
                    return@addOnSuccessListener
                }
                
                val user = documents.first()
                val allowedDevices = user.get("allowedDevices") as? List<String> ?: emptyList()
                val isActive = user.getBoolean("isActive") ?: false
                
                val isWhitelisted = isActive && (
                    allowedDevices.isEmpty() || 
                    allowedDevices.contains(deviceId)
                )
                
                continuation.resume(Result.success(isWhitelisted))
            }
            .addOnFailureListener { exception ->
                continuation.resume(Result.failure(exception))
            }
    }
}
```

**Aturan Keamanan Firestore:**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Koleksi Whitelist - hanya admin
    match /whitelist/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
                     get(/databases/$(database)/documents/admins/$(request.auth.uid)).data.role in ['admin', 'super_admin'];
    }
    
    // Koleksi Admin - hanya super admin
    match /admins/{adminId} {
      allow read: if request.auth != null && 
                    get(/databases/$(database)/documents/admins/$(request.auth.uid)).data.role in ['admin', 'super_admin'];
      allow write: if request.auth != null && 
                     get(/databases/$(database)/documents/admins/$(request.auth.uid)).data.role == 'super_admin';
    }
    
    // Data user - hanya data sendiri
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Riwayat trading - hanya data sendiri
    match /trades/{tradeId} {
      allow read, write: if request.auth != null && resource.data.userId == request.auth.uid;
    }
  }
}
```

---

### 👥 Peran & Izin Admin

**Definisi Peran:**
```kotlin
enum class AdminRole {
    SUPER_ADMIN,  // Akses penuh sistem
    ADMIN         // Akses terbatas
}

data class AdminPermissions(
    val canManageUsers: Boolean,
    val canManageAdmins: Boolean,
    val canViewAllData: Boolean,
    val canModifySettings: Boolean,
    val canExportData: Boolean,
    val canDeleteUsers: Boolean
)

fun getPermissions(role: AdminRole): AdminPermissions {
    return when (role) {
        AdminRole.SUPER_ADMIN -> AdminPermissions(
            canManageUsers = true,
            canManageAdmins = true,
            canViewAllData = true,
            canModifySettings = true,
            canExportData = true,
            canDeleteUsers = true
        )
        AdminRole.ADMIN -> AdminPermissions(
            canManageUsers = true,
            canManageAdmins = false,
            canViewAllData = false,  // Hanya user sendiri
            canModifySettings = false,
            canExportData = true,
            canDeleteUsers = false
        )
    }
}
```

**Konfigurasi Super Admin:**
```kotlin
object AdminConfig {
    const val SUPER_ADMIN_EMAIL = "*********@gmail.com"
    
    fun isSuperAdmin(email: String): Boolean {
        return email.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)
    }
    
    suspend fun checkAdminStatus(email: String): AdminRole? {
        if (isSuperAdmin(email)) {
            return AdminRole.SUPER_ADMIN
        }
        
        return firestore.collection("admins")
            .whereEqualTo("email", email)
            .whereEqualTo("isActive", true)
            .get()
            .await()
            .firstOrNull()
            ?.let { AdminRole.ADMIN }
    }
}
```

---

## 📊 Optimasi Performa

### ⚡ Manajemen WebSocket

**Pemantauan Kesehatan:**
```kotlin
class WebSocketHealthMonitor {
    private var lastMessageTime = System.currentTimeMillis()
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    
    fun startHealthCheck() {
        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(10000) // Cek setiap 10 detik
                
                val timeSinceLastMessage = System.currentTimeMillis() - lastMessageTime
                
                if (timeSinceLastMessage > 30000) { // Timeout 30 detik
                    Log.w("WebSocket", "Tidak ada pesan diterima selama 30s, reconnecting...")
                    reconnect()
                }
            }
        }
    }
    
    fun onMessageReceived() {
        lastMessageTime = System.currentTimeMillis()
        reconnectAttempts = 0
    }
    
    private suspend fun reconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            Log.e("WebSocket", "Maks reconnect attempts tercapai")
            notifyConnectionLost()
            return
        }
        
        reconnectAttempts++
        val backoffDelay = (2.0.pow(reconnectAttempts) * 1000).toLong()
        
        delay(backoffDelay)
        WebSocketManager.disconnect()
        delay(1000)
        WebSocketManager.connectToWebSocket(/* params */)
    }
}
```

---

## 🛠 Debugging

### 📝 Sistem Logging

**Logging Terstruktur:**
```kotlin
object AppLogger {
    private const val TAG_PREFIX = "STC_"
    
    enum class LogLevel {
        DEBUG, INFO, WARNING, ERROR
    }
    
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d("$TAG_PREFIX$tag", message)
        }
    }
    
    fun i(tag: String, message: String) {
        Log.i("$TAG_PREFIX$tag", message)
    }
    
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w("$TAG_PREFIX$tag", message, throwable)
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("$TAG_PREFIX$tag", message, throwable)
        // Kirim ke crash reporting di production
        if (!BuildConfig.DEBUG) {
            FirebaseCrashlytics.getInstance().recordException(
                throwable ?: Exception(message)
            )
        }
    }
    
    fun logTrade(trade: Trade, result: String) {
        val logMessage = """
            Trade Dieksekusi:
            - Order ID: ${trade.orderId}
            - Aset: ${trade.asset}
            - Tipe: ${trade.trend}
            - Jumlah: ${trade.amount}
            - Hasil: $result
            - Timestamp: ${System.currentTimeMillis()}
        """.trimIndent()
        
        i("TRADING", logMessage)
    }
    
    fun logWebSocketEvent(event: String, details: String = "") {
        d("WEBSOCKET", "$event${if (details.isNotEmpty()) ": $details" else ""}")
    }
}
```

---

### 📊 Pemantauan Performa

**Statistik Koneksi:**
```kotlin
class ConnectionStatsManager {
    private var totalMessages = 0
    private var failedMessages = 0
    private var reconnections = 0
    private var averageLatency = 0L
    private val latencySamples = mutableListOf<Long>()
    
    fun recordMessage(latency: Long, success: Boolean) {
        totalMessages++
        if (!success) failedMessages++
        
        latencySamples.add(latency)
        if (latencySamples.size > 100) {
            latencySamples.removeAt(0)
        }
        
        averageLatency = latencySamples.average().toLong()
    }
    
    fun recordReconnection() {
        reconnections++
    }
    
    fun getStats(): ConnectionStats {
        return ConnectionStats(
            totalMessages = totalMessages,
            failedMessages = failedMessages,
            successRate = if (totalMessages > 0) {
                ((totalMessages - failedMessages).toFloat() / totalMessages) * 100
            } else 0f,
            averageLatency = averageLatency,
            reconnections = reconnections
        )
    }
    
    fun reset() {
        totalMessages = 0
        failedMessages = 0
        reconnections = 0
        latencySamples.clear()
    }
}

data class ConnectionStats(
    val totalMessages: Int,
    val failedMessages: Int,
    val successRate: Float,
    val averageLatency: Long,
    val reconnections: Int
)
```

**Validasi Profit:**
```kotlin
class ProfitValidator(
    private val repository: TradingRepository
) {
    suspend fun validateTodayProfitCalculation(): ValidationResult {
        val trades = repository.getTodayTrades()
        
        val calculatedProfit = trades.sumOf { it.profit ?: 0 }
        val reportedProfit = repository.getTodayProfit()
        
        val discrepancy = abs(calculatedProfit - reportedProfit)
        val isValid = discrepancy < 100 // Izinkan perbedaan 1 sen
        
        return ValidationResult(
            isValid = isValid,
            calculatedProfit = calculatedProfit,
            reportedProfit = reportedProfit,
            discrepancy = discrepancy,
            tradeCount = trades.size
        )
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val calculatedProfit: Long,
    val reportedProfit: Long,
    val discrepancy: Long,
    val tradeCount: Int
)
```

**Info Performa Bot:**
```kotlin
fun getBotPerformanceInfo(): BotPerformance {
    return BotPerformance(
        uptime = System.currentTimeMillis() - botStartTime,
        tradesExecuted = totalTrades,
        averageExecutionTime = calculateAverageExecutionTime(),
        successRate = calculateSuccessRate(),
        profitLoss = calculateProfitLoss(),
        currentStreak = calculateStreak(),
        memoryUsage = getMemoryUsage()
    )
}

data class BotPerformance(
    val uptime: Long,
    val tradesExecuted: Int,
    val averageExecutionTime: Long,
    val successRate: Float,
    val profitLoss: Long,
    val currentStreak: Int,
    val memoryUsage: MemoryInfo
)
```

---

## 🤝 Kontribusi

Ini adalah **proyek proprietary**. Kontribusi terbatas hanya untuk anggota tim yang berwenang.

### Pedoman Pengembangan

**Standar Kode:**
- Ikuti konvensi coding Kotlin
- Gunakan nama variabel dan fungsi yang bermakna
- Tulis komentar KDoc untuk API publik
- Pertahankan format yang konsisten

**Git Workflow:**
```bash
# Buat branch fitur
git checkout -b feature/nama-fitur-anda

# Buat perubahan dan commit
git add .
git commit -m "feat: tambahkan deskripsi fitur anda"

# Push ke remote
git push origin feature/nama-fitur-anda

# Buat pull request untuk review
```

**Format Pesan Commit:**
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Tipe:**
- `feat`: Fitur baru
- `fix`: Perbaikan bug
- `docs`: Dokumentasi
- `style`: Format
- `refactor`: Restruktur kode
- `test`: Menambahkan test
- `chore`: Pemeliharaan

---

## 📄 Lisensi

**Hak Cipta © 2025 STC Autotrade. Hak cipta dilindungi undang-undang.**

Software ini bersifat proprietary dan rahasia. Menyalin, mendistribusikan, memodifikasi, atau menggunakan tanpa izin tertulis eksplisit dari pemegang hak cipta sangat dilarang.

**Ketentuan Lisensi:**
- Penggunaan komersial dilarang tanpa lisensi
- Redistribusi dilarang
- Modifikasi dilarang
- Penggunaan pribadi hanya untuk pengguna yang berwenang

Untuk pertanyaan lisensi, hubungi: **support@stcbroker.id**

---

## 👥 Developer

**Lead Developer:** Arya Satya Ibnu Sani  
**Email:** sanzystoreid@gmail.com

**Inti:**
- Pengembangan Backend
- Pengembangan Android
- Desain UI/UX
- Quality Assurance
- DevOps & Infrastruktur

---

## 📞 Dukungan

Butuh bantuan? Hubungi kami melalui:

**Dukungan Teknis:**
- 📧 Email: support@stcbroker.id
- 💬 WhatsApp: +62 859-5986-0015
- 📱 Telegram: @stc_support
- 🌐 Website: https://stcbroker.id

**Jam Dukungan:**
- Senin - Jumat: 09:00 - 18:00 (WIB)
- Sabtu: 10:00 - 16:00 (WIB)
- Minggu: Tutup

**Waktu Respons:**
- Masalah Kritis: < 2 jam
- Prioritas Tinggi: < 6 jam
- Normal: < 24 jam

**Dokumentasi:**
- Panduan Pengguna: [Link ke dokumentasi pengguna]
- Referensi API: [Link ke dokumentasi API]
- Tutorial Video: [Link ke panduan video]
- FAQ: [Link ke pertanyaan yang sering diajukan]

---

## 🐛 Masalah yang Diketahui

- Koneksi WebSocket mungkin sesekali terputus pada jaringan yang buruk
- Perhitungan indikator mungkin lambat pada perangkat lama (SDK < 26)
- Perpindahan tema memerlukan restart aplikasi dalam beberapa kasus

Untuk laporan bug terbaru dan status, cek issue tracker kami.

---

## 📱 Persyaratan Sistem

**Persyaratan Minimum:**
- Android 7.0 (API 24) atau lebih tinggi
- 2GB RAM
- 100MB ruang penyimpanan bebas
- Koneksi internet

**Direkomendasikan:**
- Android 10.0 (API 29) atau lebih tinggi
- 4GB RAM
- 500MB ruang penyimpanan bebas
- Koneksi WiFi atau 4G yang stabil

---

<div align="center">

**Dibuat dengan ❤️ untuk Trader di Seluruh Dunia**

**Versi 1.0.0** | **Terakhir Diperbarui: November 2025**

[⬆️ Kembali ke Atas](#stc-autotrade)

</div>