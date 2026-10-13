# SpendWise

SpendWise is a portfolio-quality personal finance Android application engineered to demonstrate modern Android, Kotlin, software architecture, accessibility, testing, and production-quality mobile engineering practices.

Inspired by the clean, minimal, and dependable user experience of modern UK banking and digital public services, SpendWise prioritises clarity, robust financial data handling, offline-first reliability, and WCAG-aligned accessibility over flashy fintech aesthetics.

---

## Current Implementation Status

SpendWise is developed incrementally in disciplined phases. The project distinguishes between what is actively implemented, what is currently a navigation placeholder, and what is planned for future phases.

### ✅ Implemented Functionality
- **Financial Dashboard**:
  - All-time overall balance calculation (`total income - total expenses`).
  - Selected month cashflow summary displaying monthly income and monthly expenses.
  - Interactive pill-style month switcher updating monthly cashflow and recent transactions while retaining the all-time balance.
  - Recent transactions list presenting up to five transactions for the selected month, sorted by date and creation time descending.
  - Local wall-clock time-aware greeting resolved via injected `Clock` and `ZoneId` (Morning: 05:00–11:59, Afternoon: 12:00–16:59, Evening: 17:00–20:59, Night: 21:00–04:59) with background transition scheduling.
  - Dedicated loading, error/retry, and empty dashboard states.
  - Direct reuse of domain transaction use cases without creating redundant dashboard data sources.
- **Transaction Management**:
  - Full transaction lifecycle: viewing, creating, editing, and deleting transactions.
  - Hard deletion flow safeguarded by an explicit user confirmation dialog.
  - In-memory list updates with immediate persistence to local storage.
- **Search & Filtering**:
  - Real-time text search querying transaction notes and category names (case-insensitive).
  - Type filtering (`All`, `Income`, `Expense`).
  - Category filtering by catalogue category ID.
  - Date range filtering (`dateFrom`, `dateTo`).
  - Dedicated "Clear Filters" action and tailored filtered-empty state.
- **Add / Edit Transaction Form**:
  - Hero amount input with precise decimal handling and visual currency adornment.
  - Segmented control for toggling transaction type (`Expense` / `Income`).
  - Category picker leveraging the predefined domain category catalogue.
  - Date picker constrained to past dates and today (future dates disallowed; maximum selectable date supplied by ViewModel).
  - Note input supporting up to 500 characters with live character counter.
  - Single-currency GBP presentation (displayed as read-only; explicitly persisted).
  - Window insets and IME padding ensuring the primary Save action remains reachable when the soft keyboard is visible.
  - Clear, user-facing validation error messaging without exposing technical exception internals.
- **Architecture & Local Persistence**:
  - Offline-first Room persistence with database singleton, DAO, entity, and explicit mapper boundaries.
  - Strict MVVM and Clean Architecture dependency flow.
  - Single-activity architecture using Navigation Compose.
  - Parent-scoped `TransactionViewModel` shared across the transaction list and child Add/Edit destinations (`transactions/add`, `transactions/edit/{transactionId}`).
  - Dependency injection provided via Dagger Hilt.
- **Design System & Accessibility**:
  - Material 3 theme supporting full Light and Dark modes.
  - Restrained color palette: deep blue primary brand color, neutral backgrounds/surfaces, and semantic green/red for positive/negative states.
  - Decorative squircle category icon badges (`CategoryIconBadge`) visually mapping categories without adding TalkBack clutter.
  - Information never communicated through color alone (+ / - prefixes, directional icons, and text labels).
  - Merged Compose semantics on interactive transaction rows with `Role.Button` and descriptive TalkBack announcements.
  - Minimum 48dp touch targets across interactive controls.
  - Window inset consumption preventing duplicated status bar spacing.

### 🟡 Placeholders (In Navigation Graph)
- **Analytics, Budget, and Settings**:
  - Top-level bottom navigation destinations exist in `MainActivity` with back-stack state saving and restoration (`popUpTo`, `launchSingleTop = true`, `restoreState = true`).
  - Current destination screens render minimal placeholder views while core transaction and dashboard features are being perfected.

### 📋 Planned / Deferred Features
- **Analytics**: Financial charts, spending trends, category breakdowns, and monthly comparison metrics.
- **Budget**: Monthly spending limits, category budgets, and budget progress indicators.
- **Settings**: Currency preferences, export/import, and accessibility toggles.
- **Receipt Scanning / OCR**: On-device optical character recognition to extract receipt details for user verification.
- **Recurring Payment Reminders**: Reminder-based notifications for upcoming bills without requiring direct banking credentials.
- **Backend Service**: Future backend using Spring Boot, PostgreSQL, JWT authentication, Docker, and REST APIs, paired with Android Retrofit/OkHttp synchronization.
- **Multi-Currency Support & Custom Categories**: Runtime currency selection and user-defined category creation.

---

## Tech Stack & Architecture

| Layer / Concern | Technology |
|---|---|
| **Language** | Kotlin 2.0.21 |
| **UI Framework** | Jetpack Compose (BOM 2025.06.01) |
| **Design System** | Material 3 (1.4.0) |
| **Architecture** | MVVM + Clean Architecture (Single Activity) |
| **Navigation** | Navigation Compose (2.9.8) |
| **Dependency Injection** | Dagger Hilt (2.56.2) |
| **Persistence** | Room (2.8.4) with KSP |
| **Asynchronous Programming** | Kotlin Coroutines (1.8.1) & Flow (`StateFlow`, `SharedFlow`) |
| **Lifecycle & Architecture Components** | AndroidX Lifecycle (2.9.2), Splashscreen (1.2.0) |
| **Testing** | JUnit 4 (4.13.2), `kotlinx-coroutines-test` (1.8.1), Compose UI Test |
| **Build System** | Gradle 8.10.1 (KTS), Android Gradle Plugin 8.10.1 |

### Architectural Flow

SpendWise enforces strict inward dependency flow:

```
Compose UI (Screens / Components)
       ↓
ViewModel (StateFlow / UI Events & Effects)
       ↓
Domain Use Cases
       ↓
Domain Repository Interface
       ↓
Data Repository Implementation
       ↓
Room DAO / Data Source
       ↓
SQLite / Room Database
```

- **Domain Layer (`domain/`)**: Pure Kotlin modules containing business models (`Transaction`, `Money`, `CurrencyCode`, `CategoryId`), catalogue definitions, domain validation rules, repository interfaces, and use cases. Free from Android framework and Room dependencies.
- **Data Layer (`data/`)**: Local persistence via Room (`SpendWiseDatabase`, `TransactionDao`, `TransactionEntity`), explicit two-way mappers (`TransactionMapper`), and repository implementations (`TransactionRepositoryImpl`).
- **Presentation Layer (`presentation/`)**: Feature-oriented packages (`dashboard`, `transaction`, `navigation`, `designsystem`) containing UI composables, ViewModels, UI state, UI events, and UI effects.
- **Dependency Injection (`di/`)**: Hilt modules configuring database singletons, DAO bindings, repository implementations, use cases, and injectable `Clock`/`ZoneId` providers.

---

## Package Structure

```
com.kadambari.spendwise
├── data
│   ├── local
│   │   ├── dao
│   │   ├── database
│   │   └── entity
│   ├── mapper
│   └── repository
│
├── domain
│   ├── model
│   ├── repository
│   └── usecase
│
├── presentation
│   ├── dashboard
│   ├── transaction
│   ├── analytics           # Placeholder destination
│   ├── budget              # Placeholder destination
│   ├── settings            # Placeholder destination
│   ├── navigation
│   ├── components
│   └── designsystem
│       ├── theme
│       └── components
│
├── di
└── util
```

---

## Accessibility & UI Design Philosophy

SpendWise is designed in line with WCAG-aligned accessibility principles applicable to modern Android:

1. **Information Not Conveyed by Color Alone**: Financial amounts explicitly prepend `+` or `-` signs, and visual badges include directional arrow icons and text labels (e.g., `+£3,200.00` Income, `-£42.80` Expense).
2. **Accessible Touch Targets**: Buttons, icons, list items, and interactive selectors adhere to a minimum 48dp touch target.
3. **Screen Reader (TalkBack) Optimisation**:
   - Interactive transaction list items use merged semantics (`Modifier.semantics(mergeDescendants = true)`) with `Role.Button` and custom click action labels, announcing the edit action alongside category, type, formatted amount, date, and note.
   - Decorative visual avatars (`CategoryIconBadge`) set `contentDescription = null` to avoid redundant announcements in merged rows.
4. **Dynamic Font Scaling & Responsiveness**: Flexible layouts and typography tokens prevent text clipping when system font sizes are enlarged.
5. **IME & Inset Awareness**: Form containers apply `.imePadding()` and vertical scrolling so that action buttons remain accessible while the keyboard is active. Scaffold insets are cleanly consumed to prevent double top padding.
6. **Dark Theme Support**: Dedicated dark color schemes with semantic color adjustments ensure high readability and contrast in both light and dark system settings.

---

## Testing & Verification

SpendWise emphasizes reliable automated testing at multiple layers:

- **Domain Unit Tests**: Authoritative validation of monetary precision, currency rules, future date rejection, and note length constraints.
- **Use Case Tests**: Verifying application operations and business rule orchestration using fake repository doubles.
- **Repository Tests**: Verifying data mapping and DAO delegation using in-memory DAO doubles.
- **ViewModel Tests**: Testing UI state emissions, event handling, error mapping, and time-aware greeting calculations. The dashboard test suite uses a mutable test clock advancing alongside coroutine virtual time to verify time-boundary transitions.
- **Compose UI Tests**: Verifying semantic trees, accessibility properties, and screen state rendering.

### Verification Commands

```powershell
# 1. Run all JVM unit tests (debug variant)
.\gradlew.bat testDebugUnitTest

# 2. Verify compilation of Compose UI instrumentation test sources
.\gradlew.bat compileDebugAndroidTestKotlin

# 3. Assemble the debug APK
.\gradlew.bat assembleDebug

# 4. Run all unit tests across all build variants
.\gradlew.bat test
```

> **Note on Instrumentation Tests**: Instrumentation test sources compile cleanly via `compileDebugAndroidTestKotlin`. Execution on physical hardware or automated CI devices is scheduled for dedicated device testing phases. Live emulator observation of the midnight greeting transition remains a pending manual check.

---

## Prerequisites & Getting Started

### Prerequisites
- **JDK**: Java 17 or higher
- **Android Studio**: Android Studio Ladybug (2024.2.1) or newer
- **Android SDK**: `compileSdk = 36`, `targetSdk = 36`, `minSdk = 24`

### Build & Run Instructions

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Kadambari-Kadulkar/SpendWise.git
   cd SpendWise
   ```
2. **Open the project** in Android Studio and allow Gradle to sync.
3. **Execute unit tests**:
   ```powershell
   .\gradlew.bat testDebugUnitTest
   ```
4. **Build the debug APK**:
   ```powershell
   .\gradlew.bat assembleDebug
   ```
   The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.
5. **Run on an Emulator or Device**: Select the `app` run configuration in Android Studio and press **Run**.

---

## Roadmap

Future development milestones follow the incremental engineering approach:

- **Phase A**: Analytics screen with financial spending trends, category distributions, and period comparisons.
- **Phase B**: Budget management with monthly and category spending limits, progress tracking, and threshold warnings.
- **Phase C**: Settings screen with appearance options, data export, and accessibility preferences.
- **Phase D**: Receipt scanning and OCR text extraction for semi-automated expense capture.
- **Phase E**: Non-intrusive recurring payment reminders.
- **Phase F**: Backend synchronization with Spring Boot, PostgreSQL, Docker, and JWT authentication.

---

## Project Documentation

Detailed architectural notes, design decisions, and specifications are maintained in the `docs/` directory:

- [Architecture Guide](docs/ARCHITECTURE.md) — Comprehensive technical architecture, dependency boundaries, persistence mapping, and ViewModel scoping.
- [Product Specification](docs/PRODUCT_SPEC.md) — Product vision, V1 scope, domain rules, currency constraints, and roadmap.
- [Design System Guide](docs/DESIGN_SYSTEM.md) — Visual principles, color tokens, typography, component standards, and accessibility checklist.
- [Development Log](docs/DEVELOPMENT_LOG.md) — Chronological engineering milestones, phase reviews, and verification logs.
