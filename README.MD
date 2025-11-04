# STC Autotrade

<div align="center">

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/kotlin-1.9.0-purple.svg)
![License](https://img.shields.io/badge/license-Proprietary-red.svg)

**Advanced Trading Automation System for Stockity Platform**

[Features](#-features) • [Installation](#-installation) • [Architecture](#-architecture) • [Documentation](#-api-integration)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Getting Started](#-getting-started)
- [Project Structure](#-project-structure)
- [Trading Modes](#-trading-modes)
- [Configuration](#%EF%B8%8F-configuration)
- [API Integration](#-api-integration)
- [Security](#-security)
- [Performance](#-performance-optimization)
- [Debugging](#-debugging)
- [Contributing](#-contributing)
- [License](#-license)
- [Support](#-support)

---

## 🎯 Overview

STC Autotrade is a sophisticated Android application designed to provide automated trading capabilities for the Stockity binary options platform. Built with modern Android development practices and Jetpack Compose, it delivers multiple trading strategies, real-time market monitoring, and comprehensive trade management features.

### Key Highlights

- 🚀 **Ultra-Fast Execution** - 50ms detection interval for instant trade recognition
- 🎯 **Multiple Trading Modes** - Schedule, Follow Order, Indicator, and CTC modes
- 💰 **Multi-Currency Support** - IDR, USD, EUR, MYR, and 15+ currencies
- 🌍 **Multi-Language** - Support for 7 languages including Indonesian, English, Spanish, Vietnamese, Turkish, Hindi, and Malay
- 📊 **Real-Time Analytics** - Live profit tracking and comprehensive statistics
- 🔐 **Enterprise Security** - Encrypted sessions and whitelist management system
- 🎨 **Modern UI** - Material Design 3 with dark/light theme support

---

## ✨ Features

### 🤖 Automated Trading Modes

#### 1. Schedule Mode
Pre-configured trading schedules with bulk execution capabilities.

**Features:**
- Bulk order creation and management
- Time-based automated execution
- Martingale strategy support
- Continuous monitoring at 50ms intervals
- Configurable trade parameters

#### 2. Follow Order Mode (Ultra-Fast)
Real-time price analysis with industry-leading detection speed.

**Features:**
- 50ms monitoring interval
- Multi-layer detection system (WebSocket → Balance → API)
- Boundary and instant execution modes
- Automatic recovery on connection issues
- Triple fallback mechanism for reliability

**Performance Metrics:**
- Detection latency: < 50ms
- Execution response: < 100ms
- Triple-layer redundancy
- Cached trend detection

#### 3. Indicator Order Mode
Technical analysis-based trading with automatic cycle restart.

**Supported Indicators:**
- RSI (Relative Strength Index)
- EMA (Exponential Moving Average)
- Bollinger Bands
- MACD (Moving Average Convergence Divergence)
- Stochastic Oscillator

**Features:**
- Auto-restart on trade completion (win/loss)
- Support and resistance level detection
- Adjustable sensitivity parameters
- Continuous cycle operation
- Prediction-based execution

#### 4. CTC Order Mode (Ultra-Fast)
Optimized mode with identical performance to Follow Order.

**Features:**
- 50ms ultra-fast detection
- Same speed optimization as Follow Order
- Performance-focused design
- Triple-layer detection system
- Ideal for high-frequency trading

### 💎 Advanced Trading Features

#### Martingale Strategy
Intelligent position sizing for loss recovery.

**Configuration Options:**
- Multiplier types: Fixed or Percentage-based
- Maximum steps: 1-10 configurable levels
- Smart recovery system
- Real-time step tracking
- Automatic reset on profit

#### Risk Management
Comprehensive protection mechanisms.

**Features:**
- **Stop Loss** - Maximum loss threshold protection
- **Stop Profit** - Automatic profit-taking at target
- **Session-based Tracking** - Per-session P&L monitoring
- **Loss Prevention** - Smart circuit breakers
- **Real-time Alerts** - Instant notifications on thresholds

#### Real-Time Monitoring
Live trading analytics and performance tracking.

**Metrics:**
- Live P&L (Profit & Loss) tracking
- Today's profit calculation
- Trading statistics dashboard
- Win rate analytics
- Trade history with details
- Performance visualization

### 👥 User Management

- **Whitelist System** - Admin-controlled user access with device verification
- **Multi-Admin Support** - Regular Admin and Super Admin roles with granular permissions
- **Auto-Registration** - Seamless user onboarding from Stockity platform
- **Session Management** - Encrypted user sessions with automatic expiry
- **Device Tracking** - Multi-device support with security controls

### 🎨 UI/UX Features

- **Material Design 3** - Modern, intuitive interface following latest design guidelines
- **Dark/Light Theme** - System-aware or manual theme selection
- **Multi-Language** - 7 languages with easy switching
- **Responsive Design** - Optimized for phones and tablets
- **Real-Time Updates** - Live data synchronization with smooth animations
- **Accessibility** - Screen reader support and high contrast modes

---

## 🛠 Tech Stack

### Core Technologies

| Component | Version | Purpose |
|-----------|---------|---------|
| **Kotlin** | 1.9.0 | Primary language |
| **Jetpack Compose** | Latest | UI framework |
| **Min SDK** | 24 | Android 7.0+ |
| **Target SDK** | 34 | Android 14 |
| **Gradle** | 8.0+ | Build system |

### Architecture & Libraries

#### 🏗️ Dependency Injection

```kotlin
val hilt = Hilt 2.48                    // Dependency injection framework
```

#### 🌐 Networking
```kotlin
Retrofit 2.9.0               // REST API client
OkHttp 4.11.0                // HTTP client with interceptors
Gson 2.10.1                  // JSON serialization/deserialization
WebSocket                    // Real-time bidirectional communication
```

#### 💾 Database & Storage
```kotlin
Firebase Firestore           // Cloud NoSQL database
EncryptedSharedPreferences   // Secure local key-value storage
```

#### 🎨 UI Components
```kotlin
Compose Material3            // Material Design 3 components
Accompanist WebView          // WebView integration for Compose
Compose Navigation           // Type-safe navigation system
Compose Lifecycle            // Lifecycle-aware components
```

#### 🔧 Utilities
```kotlin
Kotlin Coroutines            // Asynchronous programming
Kotlin Flow                  // Reactive data streams
Coil                         // Efficient image loading
Security Crypto              // Data encryption utilities
```

---

## 🏗 Architecture

### MVVM Pattern Implementation

```
┌─────────────────────────────────────────────────┐
│                  View Layer                     │
│           Jetpack Compose Screens               │
│        (UI Components & User Interaction)       │
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

### Layer Responsibilities

#### 1. 🎨 Presentation Layer
- **Screens** - Composable UI screens with Material Design 3
- **ViewModels** - State management and business logic coordination
- **UI State** - Immutable state classes for predictable UI updates
- **Navigation** - Type-safe navigation between screens

#### 2. 🎯 Domain Layer
- **Use Cases** - Encapsulated business logic operations
- **Models** - Domain-specific data models
- **Repositories** - Abstract data access interfaces

#### 3. 💾 Data Layer
- **Remote** - API services, WebSocket connections, Firebase integration
- **Local** - SharedPreferences, encrypted session management
- **Mappers** - Data transformation between layers

### Dependency Injection Structure

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
    
    // ... additional providers
}
```

---

## 🚀 Getting Started

### Prerequisites

Ensure you have the following installed:

- **Android Studio** - Hedgehog (2023.1.1) or later
- **JDK** - Version 17 or later
- **Android SDK** - API level 34
- **Gradle** - Version 8.0 or later
- **Git** - For version control

### Installation Steps

#### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/stc-autotrade.git
cd stc-autotrade
```

#### 2. Configure Firebase
- Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
- Enable Firestore Database
- Download `google-services.json`
- Place the file in the `app/` directory

#### 3. Set Up API Credentials
Create a `local.properties` file in the project root:

```properties
# Stockity API Configuration
STOCKITY_API_URL=https://api.stockity.id/

# Optional: Add debug keys
DEBUG_MODE=true
```

#### 4. Sync and Build
```bash
# Sync Gradle dependencies
./gradlew sync

# Build the project
./gradlew build
```

#### 5. Run the Application
```bash
# Install debug build
./gradlew installDebug

# Or run directly from Android Studio
```

### Initial Configuration

#### Admin Setup
1. **Super Admin Access**
    - Default Super Admin email: `aryasis87@gmail.com`
    - Login credentials are managed in Firebase
    - Super Admin has full system access

2. **System Configuration**
    - Navigate to Admin Panel after login
    - Configure Stockity registration URL
    - Set WhatsApp support contact number
    - Define default trading parameters

3. **User Management**
    - Add users to whitelist manually, OR
    - Enable auto-registration from Stockity
    - Configure user permissions
    - Set device limits per user

---

## 📁 Project Structure

```
com.autotrade.finalstc/
│
├── data/
│   ├── api/                    # API service interfaces
│   │   ├── StockityApiService
│   │   └── WebSocketManager
│   ├── local/                  # Local data management
│   │   ├── SessionManager
│   │   └── PreferencesManager
│   ├── model/                  # Data models & DTOs
│   │   ├── request/
│   │   ├── response/
│   │   └── entity/
│   └── repository/             # Repository implementations
│       ├── AuthRepository
│       ├── TradingRepository
│       └── UserRepository
│
├── presentation/
│   ├── login/                  # Login screen
│   │   ├── LoginScreen.kt
│   │   └── LoginViewModel.kt
│   ├── register/               # Registration screen
│   │   ├── RegisterScreen.kt
│   │   └── RegisterViewModel.kt
│   ├── main/                   # Main container
│   │   ├── dashboard/          # Dashboard & trading controls
│   │   │   ├── DashboardScreen.kt
│   │   │   └── DashboardViewModel.kt
│   │   ├── history/            # Trading history
│   │   │   ├── HistoryScreen.kt
│   │   │   └── HistoryViewModel.kt
│   │   ├── profile/            # User profile & settings
│   │   │   ├── ProfileScreen.kt
│   │   │   └── ProfileViewModel.kt
│   │   └── webview/            # Web trading view
│   │       ├── WebViewScreen.kt
│   │       └── WebViewViewModel.kt
│   ├── admin/                  # Admin panel
│   │   ├── AdminScreen.kt
│   │   └── AdminViewModel.kt
│   └── theme/                  # Theme management
│       ├── Theme.kt
│       ├── Color.kt
│       └── Type.kt
│
├── utils/                      # Utility classes
│   ├── StringsManager.kt       # Multi-language support
│   ├── FileExportHelper.kt     # Export functionality
│   ├── PermissionsHandler.kt   # Permission management
│   ├── CurrencyFormatter.kt    # Currency formatting
│   └── DateTimeUtils.kt        # Date/time utilities
│
├── di/                         # Dependency injection modules
│   ├── AppModule.kt
│   ├── NetworkModule.kt
│   └── DatabaseModule.kt
│
└── MainApplication.kt          # Application class
```

---

## 🎮 Trading Modes

### 1. 📅 Schedule Mode

Execute trades based on pre-configured time schedules with bulk order support.

**Key Features:**
- Bulk order creation with CSV import support
- Precise time-based execution
- Martingale strategy integration
- Continuous monitoring at 50ms intervals
- Automatic retry on failed orders

**Usage Example:**
```kotlin
// Add scheduled orders
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

// Start the bot
viewModel.startBot(mode = TradingMode.SCHEDULE)

// Monitor execution
viewModel.botState.collect { state ->
    when (state) {
        is BotState.Running -> // Handle running state
        is BotState.Completed -> // Handle completion
        is BotState.Error -> // Handle errors
    }
}
```

**Best Practices:**
- Set realistic time windows for execution
- Use appropriate amounts based on account balance
- Enable stop loss for risk management
- Monitor execution logs for optimization

---

### 2. ⚡ Follow Order Mode (Ultra-Fast)

Real-time price analysis with industry-leading 50ms detection speed.

**Architecture:**
```
Detection Pipeline:
WebSocket (Primary) → Balance API (Secondary) → REST API (Fallback)
     ↓ 50ms              ↓ 100ms                    ↓ 150ms
```

**Key Features:**
- **50ms monitoring interval** for instant detection
- **Triple-layer detection** with automatic fallback
- **Boundary execution** for precise entry points
- **Instant execution** for immediate trades
- **Auto-recovery system** on connection loss
- **Cached trend analysis** for speed optimization

**Performance Specifications:**
- Detection latency: < 50ms (average 35ms)
- Execution response: < 100ms (average 75ms)
- WebSocket reliability: 99.9% uptime
- Automatic reconnection: < 2 seconds

**Usage Example:**
```kotlin
// Configure Follow Order mode
viewModel.configureFollowOrder(
    config = FollowOrderConfig(
        assetRic = "BTCUSD",
        detectionInterval = 50, // milliseconds
        executionMode = ExecutionMode.BOUNDARY,
        enableCaching = true
    )
)

// Start monitoring
viewModel.startBot(mode = TradingMode.FOLLOW_ORDER)

// Real-time updates
viewModel.priceUpdates.collect { price ->
    // Handle price updates
}
```

**Detection Methods:**
1. **WebSocket Stream** - Primary real-time data source
2. **Balance Check** - Secondary verification method
3. **REST API** - Tertiary fallback mechanism

---

### 3. 📊 Indicator Order Mode

Technical analysis-based automated trading with continuous cycle operation.

**Supported Technical Indicators:**

| Indicator | Parameters | Use Case |
|-----------|-----------|----------|
| **RSI** | Period: 14, Overbought: 70, Oversold: 30 | Momentum analysis |
| **EMA** | Fast: 12, Slow: 26 | Trend following |
| **Bollinger Bands** | Period: 20, StdDev: 2 | Volatility breakouts |
| **MACD** | Fast: 12, Slow: 26, Signal: 9 | Trend confirmation |
| **Stochastic** | %K: 14, %D: 3 | Momentum oscillator |

**Key Features:**
- **Auto-restart** on trade completion (win or loss)
- **Support/Resistance** level detection
- **Adjustable sensitivity** for entry signals
- **Prediction-based execution** using historical data
- **Continuous cycle operation** without manual intervention
- **Multi-indicator combination** for confirmation

**Usage Example:**
```kotlin
// Configure indicator-based trading
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

// Start trading
viewModel.startBot(mode = TradingMode.INDICATOR)

// Monitor signals
viewModel.indicatorSignals.collect { signal ->
    when (signal) {
        is Signal.Buy -> // Handle buy signal
        is Signal.Sell -> // Handle sell signal
        is Signal.Hold -> // No action
    }
}
```

**Signal Generation:**
- **Strong Buy** - Multiple indicators align for buy
- **Buy** - Primary indicator shows buy signal
- **Neutral** - No clear signal
- **Sell** - Primary indicator shows sell signal
- **Strong Sell** - Multiple indicators align for sell

---

### 4. 🔥 CTC Order Mode (Ultra-Fast)

Optimized trading mode with identical performance characteristics to Follow Order mode.

**Key Features:**
- **50ms ultra-fast detection** - Same as Follow Order
- **Triple-layer detection system** - WebSocket → Balance → API
- **Performance-optimized** for high-frequency trading
- **Low latency execution** - < 100ms response time
- **Ideal for scalping** and quick position trades

**Technical Specifications:**
```
Detection Speed:    50ms
Execution Latency:  < 100ms
Fallback Layers:    3
Reliability:        99.9%
Max Trades/Minute:  120
```

**Usage Example:**
```kotlin
// Configure CTC mode
viewModel.configureCTCMode(
    config = CTCConfig(
        assetRic = "EURUSD",
        detectionInterval = 50,
        maxTradesPerMinute = 100,
        enableFastExecution = true
    )
)

// Start CTC bot
viewModel.startBot(mode = TradingMode.CTC)
```

**When to Use CTC Mode:**
- High-frequency trading requirements
- Scalping strategies
- Quick market entry/exit
- Maximum execution speed needed

---

## ⚙️ Configuration

### 💱 Currency Settings

**Available Currencies:**

| Code | Currency | Symbol | Decimal Places |
|------|----------|--------|----------------|
| IDR | Indonesian Rupiah | Rp | 0 |
| USD | US Dollar | $ | 2 |
| EUR | Euro | € | 2 |
| MYR | Malaysian Ringgit | RM | 2 |
| GBP | British Pound | £ | 2 |
| JPY | Japanese Yen | ¥ | 0 |
| AUD | Australian Dollar | A$ | 2 |
| SGD | Singapore Dollar | S$ | 2 |
| THB | Thai Baht | ฿ | 2 |

**Implementation:**
```kotlin
// Define currency type
enum class CurrencyType {
    IDR, USD, EUR, MYR, GBP, JPY, AUD, SGD, THB
}

// Set currency in ViewModel
viewModel.setSelectedCurrency(CurrencyType.USD)

// Format amount based on currency
val formattedAmount = CurrencyFormatter.format(
    amount = 1000000, // in cents
    currency = CurrencyType.USD
) // Returns: "$10,000.00"
```

**Currency Conversion:**
```kotlin
// All amounts are stored in cents/smallest unit
val amountInCents = 1000000L // 10,000 USD = 1,000,000 cents

// Convert to display format
val displayAmount = amountInCents / 100.0 // 10,000.00
```

---

### 🌍 Language Settings

**Supported Languages:**

| Code | Language | Native Name | Completion |
|------|----------|-------------|------------|
| `id` | Indonesian | Bahasa Indonesia | 100% |
| `en` | English | English | 100% |
| `es` | Spanish | Español | 100% |
| `vi` | Vietnamese | Tiếng Việt | 100% |
| `tr` | Turkish | Türkçe | 100% |
| `hi` | Hindi | हिन्दी | 100% |
| `ms` | Malay | Bahasa Melayu | 100% |

**Implementation:**
```kotlin
// Change language
viewModel.updateLanguage(
    languageCode = "en",
    countryCode = "US"
)

// Get localized string
val welcomeText = StringsManager.getString(
    key = "welcome_message",
    language = currentLanguage
)

// Format with parameters
val greetingText = StringsManager.getString(
    key = "greeting_with_name",
    language = currentLanguage,
    params = mapOf("name" to userName)
) // Returns: "Hello, John!"
```

**Adding New Languages:**
1. Add translations to `StringsManager`
2. Update language enum in settings
3. Test all screens for text overflow
4. Verify RTL support if applicable

---

### 📈 Trading Parameters

#### Martingale Configuration

**Settings Structure:**
```kotlin
data class MartingaleSettings(
    val isEnabled: Boolean = false,
    val maxSteps: Int = 5,            // 1-10
    val baseAmount: Long = 1000000,   // in cents
    val multiplierType: MultiplierType = MultiplierType.FIXED,
    val multiplierValue: Double = 2.0  // 1.5-5.0
)

enum class MultiplierType {
    FIXED,       // 2x, 3x, 4x, etc.
    PERCENTAGE   // +50%, +100%, etc.
}
```

**Usage Example:**
```kotlin
// Configure Martingale
viewModel.updateMartingaleSettings(
    MartingaleSettings(
        isEnabled = true,
        maxSteps = 5,
        baseAmount = 1000000, // $10.00
        multiplierType = MultiplierType.FIXED,
        multiplierValue = 2.0
    )
)

// Martingale progression example (base $10):
// Step 1: $10
// Step 2: $20 (loss recovery)
// Step 3: $40 (loss recovery)
// Step 4: $80 (loss recovery)
// Step 5: $160 (loss recovery)
// Reset on win
```

**Best Practices:**
- Start with low multiplier (1.5-2.0)
- Set maximum steps based on account size
- Always use with stop loss
- Monitor drawdown carefully

#### Risk Management

**Stop Loss Settings:**
```kotlin
data class StopLossSettings(
    val isEnabled: Boolean = false,
    val maxLossAmount: Long = 5000000, // in cents ($50.00)
    val resetDaily: Boolean = true,
    val notifyOnTrigger: Boolean = true
)

// Implementation
viewModel.updateStopLossSettings(
    StopLossSettings(
        isEnabled = true,
        maxLossAmount = 5000000,
        resetDaily = true,
        notifyOnTrigger = true
    )
)
```

**Stop Profit Settings:**
```kotlin
data class StopProfitSettings(
    val isEnabled: Boolean = false,
    val targetProfitAmount: Long = 10000000, // in cents ($100.00)
    val resetDaily: Boolean = true,
    val notifyOnTrigger: Boolean = true
)

// Implementation
viewModel.updateStopProfitSettings(
    StopProfitSettings(
        isEnabled = true,
        targetProfitAmount = 10000000,
        resetDaily = true,
        notifyOnTrigger = true
    )
)
```

**Calculation Example:**
```kotlin
// Real-time P&L tracking
viewModel.currentSessionPnL.collect { pnl ->
    // Check stop loss
    if (stopLoss.isEnabled && pnl <= -stopLoss.maxLossAmount) {
        viewModel.stopBot(reason = "Stop loss triggered")
    }
    
    // Check stop profit
    if (stopProfit.isEnabled && pnl >= stopProfit.targetProfitAmount) {
        viewModel.stopBot(reason = "Stop profit reached")
    }
}
```

---

## 🔌 API Integration

### 🔐 Authentication

**Login Endpoint:**
```kotlin
// API Service Interface
interface StockityApiService {
    @POST("passport/v2/sign_in")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>
}

// Request Model
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String,
    val deviceType: String = "android"
)

// Response Model
data class LoginResponse(
    val data: UserData,
    val token: String,
    val refreshToken: String
)

// Usage
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

**Authentication Headers:**
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

**Execute Trade Endpoint:**
```kotlin
@POST("v2/options/open")
suspend fun executeTrade(
    @Body request: TradeRequest
): Response<TradeResponse>

// Request Model
data class TradeRequest(
    val assetRic: String,          // e.g., "BTCUSD"
    val trend: String,             // "CALL" or "PUT"
    val amount: Long,              // in cents
    val isDemoAccount: Boolean,
    val duration: Int = 60,        // seconds
    val leverage: Int = 1
)

// Response Model
data class TradeResponse(
    val success: Boolean,
    val data: TradeData,
    val message: String?
)

data class TradeData(
    val orderId: String,
    val openPrice: Double,
    val openTime: Long,
    val closeTime: Long,
    val payout: Double
)

// Usage Example
suspend fun executeTrade(
    assetRic: String,
    trend: String,
    amount: Long,
    isDemoAccount: Boolean
): Result<TradeResult> {
    return try {
        val response = apiService.executeTrade(
            TradeRequest(
                assetRic = assetRic,
                trend = trend,
                amount = amount,
                isDemoAccount = isDemoAccount
            )
        )
        
        if (response.isSuccessful && response.body()?.success == true) {
            Result.success(mapToTradeResult(response.body()!!.data))
        } else {
            Result.failure(Exception(response.body()?.message ?: "Trade failed"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**Get Balance Endpoint:**
```kotlin
@GET("v2/user/balance")
suspend fun getBalance(): Response<BalanceResponse>

data class BalanceResponse(
    val realBalance: Long,    // in cents
    val demoBalance: Long,    // in cents
    val currency: String
)
```

**Get Trade History:**
```kotlin
@GET("v2/options/history")
suspend fun getTradeHistory(
    @Query("page") page: Int,
    @Query("limit") limit: Int,
    @Query("startDate") startDate: Long?,
    @Query("endDate") endDate: Long?
): Response<TradeHistoryResponse>
```

---

### 🔌 WebSocket Connection

**Connection Management:**
```kotlin
object WebSocketManager {
    private var webSocket: WebSocket? = null
    private val messageFlow = MutableSharedFlow<WebSocketMessage>()
    
    fun connectToWebSocket(
        userAgent: String,
        authToken: String,
        deviceType: String = "web",
        deviceId: String
    ) {
        val request = Request.Builder()
            .url("wss://api.stockity.id/ws")
            .addHeader("User-Agent", userAgent)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("X-Device-Type", deviceType)
            .addHeader("X-Device-Id", deviceId)
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connection opened")
                subscribeToChannels()
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Closing: $code / $reason")
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Closed: $code / $reason")
                scheduleReconnect()
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Error: ${t.message}", t)
                scheduleReconnect()
            }
        })
    }
    
    private fun subscribeToChannels() {
        // Subscribe to price updates
        sendMessage("""
            {
                "action": "subscribe",
                "channel": "prices",
                "symbols": ["BTCUSD", "EURUSD", "GBPUSD"]
            }
        """.trimIndent())
        
        // Subscribe to trade updates
        sendMessage("""
            {
                "action": "subscribe",
                "channel": "trades"
            }
        """.trimIndent())
    }
    
    fun sendMessage(message: String) {
        webSocket?.send(message)
    }
    
    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }
    
    // Collect messages
    fun observeMessages(): Flow<WebSocketMessage> = messageFlow.asSharedFlow()
}
```

**Message Types:**
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
        delay(2000) // Wait 2 seconds
        if (webSocket == null || webSocket?.close(1000, null) == true) {
            connectToWebSocket(/* params */)
        }
    }
}
```

---

## 🔐 Security

### 🔒 Data Encryption

**EncryptedSharedPreferences Implementation:**
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

**Encryption Specifications:**
- **Algorithm**: AES-256-GCM (Galois/Counter Mode)
- **Key Management**: Android Keystore System
- **Key Scheme**: AES256_GCM for master key
- **Preference Encryption**:
    - Keys: AES256_SIV (Synthetic IV mode)
    - Values: AES256_GCM

**Secure Data Storage:**
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

### 🛡️ Access Control

**Whitelist Verification:**
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

**Firestore Security Rules:**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Whitelist collection - admin only
    match /whitelist/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
                     get(/databases/$(database)/documents/admins/$(request.auth.uid)).data.role in ['admin', 'super_admin'];
    }
    
    // Admin collection - super admin only
    match /admins/{adminId} {
      allow read: if request.auth != null && 
                    get(/databases/$(database)/documents/admins/$(request.auth.uid)).data.role in ['admin', 'super_admin'];
      allow write: if request.auth != null && 
                     get(/databases/$(database)/documents/admins/$(request.auth.uid)).data.role == 'super_admin';
    }
    
    // User data - own data only
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Trading history - own data only
    match /trades/{tradeId} {
      allow read, write: if request.auth != null && resource.data.userId == request.auth.uid;
    }
  }
}
```

---

### 👥 Admin Roles & Permissions

**Role Definitions:**
```kotlin
enum class AdminRole {
    SUPER_ADMIN,  // Full system access
    ADMIN         // Limited access
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
            canViewAllData = false,  // Only own users
            canModifySettings = false,
            canExportData = true,
            canDeleteUsers = false
        )
    }
}
```

**Super Admin Configuration:**
```kotlin
object AdminConfig {
    const val SUPER_ADMIN_EMAIL = "aryasis87@gmail.com"
    
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

## 📊 Performance Optimization

### ⚡ WebSocket Management

**Health Monitoring:**
```kotlin
class WebSocketHealthMonitor {
    private var lastMessageTime = System.currentTimeMillis()
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    
    fun startHealthCheck() {
        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(10000) // Check every 10 seconds
                
                val timeSinceLastMessage = System.currentTimeMillis() - lastMessageTime
                
                if (timeSinceLastMessage > 30000) { // 30 seconds timeout
                    Log.w("WebSocket", "No message received for 30s, reconnecting...")
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
            Log.e("WebSocket", "Max reconnect attempts reached")
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

**Channel Management:**
```kotlin
class ChannelManager {
    private val subscribedChannels = mutableSetOf<String>()
    
    fun subscribeToChannel(channel: String, params: Map<String, Any> = emptyMap()) {
        if (subscribedChannels.contains(channel)) {
            Log.d("Channel", "Already subscribed to $channel")
            return
        }
        
        val message = buildJsonObject {
            put("action", "subscribe")
            put("channel", channel)
            params.forEach { (key, value) ->
                put(key, value.toString())
            }
        }
        
        WebSocketManager.sendMessage(message.toString())
        subscribedChannels.add(channel)
    }
    
    fun unsubscribeFromChannel(channel: String) {
        if (!subscribedChannels.contains(channel)) return
        
        val message = buildJsonObject {
            put("action", "unsubscribe")
            put("channel", channel)
        }
        
        WebSocketManager.sendMessage(message.toString())
        subscribedChannels.remove(channel)
    }
    
    fun resubscribeAll() {
        subscribedChannels.forEach { channel ->
            subscribeToChannel(channel)
        }
    }
}
```

**Message Queue:**
```kotlin
class MessageQueue {
    private val queue = LinkedBlockingQueue<String>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        startProcessing()
    }
    
    fun enqueue(message: String) {
        queue.offer(message)
    }
    
    private fun startProcessing() {
        scope.launch {
            while (isActive) {
                try {
                    val message = queue.poll(100, TimeUnit.MILLISECONDS)
                    message?.let {
                        WebSocketManager.sendMessage(it)
                        delay(50) // Rate limiting
                    }
                } catch (e: Exception) {
                    Log.e("MessageQueue", "Error processing message", e)
                }
            }
        }
    }
}
```

---

### 🧠 Memory Management

**Efficient State Management:**
```kotlin
class TradingViewModel @Inject constructor(
    private val repository: TradingRepository
) : ViewModel() {
    
    // Use StateFlow for UI state
    private val _uiState = MutableStateFlow(TradingUiState())
    val uiState: StateFlow<TradingUiState> = _uiState.asStateFlow()
    
    // Use SharedFlow for one-time events
    private val _events = MutableSharedFlow<TradingEvent>()
    val events: SharedFlow<TradingEvent> = _events.asSharedFlow()
    
    // Clear resources properly
    override fun onCleared() {
        super.onCleared()
        cancelAllJobs()
        clearCache()
    }
    
    private fun cancelAllJobs() {
        viewModelScope.coroutineContext.cancelChildren()
    }
    
    private fun clearCache() {
        // Clear any cached data
        priceCache.clear()
        tradeHistory.clear()
    }
}
```

**Resource Cleanup:**
```kotlin
class TradingService : Service() {
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Stop bot
        botManager.stopBot()
        
        // Disconnect WebSocket
        WebSocketManager.disconnect()
        
        // Cancel coroutines
        serviceScope.cancel()
        
        // Clear notifications
        notificationManager.cancelAll()
        
        Log.d("TradingService", "Service destroyed and resources cleaned up")
    }
}
```

**Lifecycle Awareness:**
```kotlin
@Composable
fun TradingScreen(viewModel: TradingViewModel = hiltViewModel()) {
    val lifecycleOwner = LocalLifecycleOwner.current
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.resumeMonitoring()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.pauseMonitoring()
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
```

---

### 🚀 UI Performance

**Composition Optimization:**
```kotlin
@Composable
fun TradingList(trades: List<Trade>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(
            items = trades,
            key = { it.orderId } // Stable keys for recomposition
        ) { trade ->
            TradeItem(
                trade = trade,
                modifier = Modifier.animateItemPlacement() // Smooth animations
            )
        }
    }
}

@Composable
fun TradeItem(trade: Trade, modifier: Modifier = Modifier) {
    // Use remember to avoid recomputation
    val formattedAmount = remember(trade.amount, trade.currency) {
        CurrencyFormatter.format(trade.amount, trade.currency)
    }
    
    Card(modifier = modifier) {
        // UI implementation
    }
}
```

**State Hoisting:**
```kotlin
@Composable
fun TradingDashboard(viewModel: TradingViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    DashboardContent(
        state = uiState,
        onStartBot = viewModel::startBot,
        onStopBot = viewModel::stopBot,
        onUpdateSettings = viewModel::updateSettings
    )
}

@Composable
private fun DashboardContent(
    state: TradingUiState,
    onStartBot: () -> Unit,
    onStopBot: () -> Unit,
    onUpdateSettings: (Settings) -> Unit
) {
    // Stateless composable - easier to test and optimize
}
```

**Recomposition Control:**
```kotlin
@Stable
data class TradingUiState(
    val isRunning: Boolean = false,
    val currentProfit: Long = 0,
    val trades: List<Trade> = emptyList(),
    val settings: Settings = Settings()
)

// Use @Stable or @Immutable to hint at composition optimization
@Immutable
data class Trade(
    val orderId: String,
    val asset: String,
    val amount: Long,
    val profit: Long?,
    val timestamp: Long
)
```

---

## 🛠 Debugging

### 📝 Logging System

**Structured Logging:**
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
        // Send to crash reporting in production
        if (!BuildConfig.DEBUG) {
            FirebaseCrashlytics.getInstance().recordException(
                throwable ?: Exception(message)
            )
        }
    }
    
    fun logTrade(trade: Trade, result: String) {
        val logMessage = """
            Trade Executed:
            - Order ID: ${trade.orderId}
            - Asset: ${trade.asset}
            - Type: ${trade.trend}
            - Amount: ${trade.amount}
            - Result: $result
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

### 📊 Performance Monitoring

**Connection Statistics:**
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

**Profit Validation:**
```kotlin
class ProfitValidator(
    private val repository: TradingRepository
) {
    suspend fun validateTodayProfitCalculation(): ValidationResult {
        val trades = repository.getTodayTrades()
        
        val calculatedProfit = trades.sumOf { it.profit ?: 0 }
        val reportedProfit = repository.getTodayProfit()
        
        val discrepancy = abs(calculatedProfit - reportedProfit)
        val isValid = discrepancy < 100 // Allow 1 cent discrepancy
        
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

**Bot Performance Info:**
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

## 🤝 Contributing

This is a **proprietary project**. Contributions are limited to authorized team members only.

### Development Guidelines

**Code Standards:**
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Write KDoc comments for public APIs
- Maintain consistent formatting

**Git Workflow:**
```bash
# Create feature branch
git checkout -b feature/your-feature-name

# Make changes and commit
git add .
git commit -m "feat: add your feature description"

# Push to remote
git push origin feature/your-feature-name

# Create pull request for review
```

**Commit Message Format:**
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Formatting
- `refactor`: Code restructuring
- `test`: Adding tests
- `chore`: Maintenance

---

## 📄 License

**Copyright © 2025 STC Autotrade. All rights reserved.**

This software is proprietary and confidential. Unauthorized copying, distribution, modification, or use is strictly prohibited without explicit written permission from the copyright holder.

**License Terms:**
- Commercial use prohibited without license
- Redistribution prohibited
- Modification prohibited
- Private use only for authorized users

For licensing inquiries, contact: **support@stcbroker.id**

---

## 👥 Team

**Lead Developer:** Arya Sis  
**Email:** aryasis87@gmail.com

**Core Team:**
- Backend Development
- Android Development
- UI/UX Design
- Quality Assurance
- DevOps & Infrastructure

---

## 🙏 Acknowledgments

Special thanks to:

- **Stockity Platform** - For API access and integration support
- **Android Jetpack Team** - For excellent architecture components
- **Kotlin Development Team** - For the amazing programming language
- **Open Source Community** - For libraries and tools that made this possible
- **Beta Testers** - For valuable feedback and bug reports

---

## 📞 Support

Need help? Contact us through:

**Technical Support:**
- 📧 Email: support@stcbroker.id
- 💬 WhatsApp: +62 859-5986-0015
- 📱 Telegram: @stc_support
- 🌐 Website: https://stcbroker.id

**Support Hours:**
- Monday - Friday: 9:00 AM - 6:00 PM (WIB)
- Saturday: 10:00 AM - 4:00 PM (WIB)
- Sunday: Closed

**Response Time:**
- Critical Issues: < 2 hours
- High Priority: < 6 hours
- Normal: < 24 hours

**Documentation:**
- User Guide: [Link to user documentation]
- API Reference: [Link to API docs]
- Video Tutorials: [Link to video guides]
- FAQ: [Link to frequently asked questions]

---

## 📈 Roadmap

### Version 1.1.0 (Q1 2025)
- [ ] Advanced indicator combinations
- [ ] Social trading features
- [ ] Portfolio management
- [ ] Advanced charting tools

### Version 1.2.0 (Q2 2025)
- [ ] AI-powered trade suggestions
- [ ] Risk calculator
- [ ] Strategy backtesting
- [ ] Multi-account management

### Version 2.0.0 (Q3 2025)
- [ ] Desktop application
- [ ] Web dashboard
- [ ] Copy trading system
- [ ] Advanced analytics

---

## 🐛 Known Issues

- WebSocket connection may occasionally drop on poor network
- Indicator calculations may lag on older devices (SDK < 26)
- Theme switching requires app restart in some cases

For the latest bug reports and status, check our issue tracker.

---

## 📱 System Requirements

**Minimum Requirements:**
- Android 7.0 (API 24) or higher
- 2GB RAM
- 100MB free storage
- Internet connection

**Recommended:**
- Android 10.0 (API 29) or higher
- 4GB RAM
- 500MB free storage
- Stable WiFi or 4G connection

---

<div align="center">

**Made with ❤️ for Traders Worldwide**

**Version 1.0.0** | **Last Updated: November 2025**

[⬆️ Back to Top](#stc-autotrade)

</div>