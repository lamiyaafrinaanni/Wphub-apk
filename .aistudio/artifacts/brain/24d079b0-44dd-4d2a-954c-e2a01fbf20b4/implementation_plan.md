# Codebase Audit & Decoupling Plan (Production-Ready Alignment)

Following your clarifying feedback, we will perform a total codebase cleanup to transition SiteDeck into a strict, production-ready, live-only utility. All mock datasets, simulated offline fallbacks, and the Apex WP Demo Site sandbox option will be completely purged.

---

## 📋 Audit Targets

### 1. 🛑 Apex WP Demo Site Options Removal
* **`WordPressConnectionScreen.kt`**: Remove any "Connect with Demo Site" or demo sandbox buttons/indicators on the connection welcome card.
* **`WPHubViewModel.kt`**: Remove `connectWithDemoSite` function and related demo-state flows.
* **`WordPressLoginScreen.kt`**: Check and clean any demo-fill or legacy mock login options.

### 2. 🔌 Offline Mock & Simulated Data Purge
* **`WordPressRestClient.kt` / `WordPressRepository.kt`**:
  * Strip out simulated orders, dummy products, mock posts, and fake alerts generator logic.
  * Ensure the app operates strictly on real-time data fetched from active REST APIs.
* **`WPHubViewModel.kt`**:
  * Remove simulated push/alert notification loops (such as `triggerSimulatedOrderAlert`, `triggerSimulatedCommentAlert`, etc.).
  * Ensure the notification system responds only to authentic webhook payloads.

### 3. 🔍 Codebase Diagnostics & Syntax Safety
* Audit all Java/Kotlin files for redundant classes, dead imports, type-unsafe casting, and duplicate properties.
* Execute a full clean build verification to guarantee zero syntax or compiler errors.

---

## 🚀 Execution Strategy
1. **Remove Connection-Screen Demo Triggers** from `WordPressConnectionScreen.kt` and `WPHubViewModel.kt`.
2. **Remove Simulated Notification Loops** from `WPHubViewModel.kt` and corresponding testing UI tabs.
3. **Audit and Clean** repository logic (`WPHubRepository.kt` / `WordPressRestClient.kt`) to remove mock fallbacks on network error.
4. **Compile & Unit Test Verification** to ensure the build remains 100% green and error-free.
