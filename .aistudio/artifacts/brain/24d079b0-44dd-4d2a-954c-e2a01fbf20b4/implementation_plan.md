# Implementation Plan - WordPress Connection & Sync Diagnostics Fix

This plan fixes the critical `401 Unauthorized` connection issue and empty dashboard bug that occurs immediately after completing Step 3 (Web Portal approval) on certain WordPress environments.

## User Review & Critical Decisions

> [!IMPORTANT]
> The primary bug is a discrepancy between the username generated during the WordPress Web Portal session (e.g. `mariyaceotb`) and the fallback saved site credentials, which were hardcoded to `admin`. This caused subsequent REST API calls to fail with 401 Unauthorized, leaving the dashboard empty.

* **Confirmed Decision 1:** Pass the actual `user_login` returned by the WordPress Web Portal instead of hardcoding `admin` in the offline/fallback site creation flow (`addNewSite`).
* **Confirmed Decision 2:** Ensure protocol discovery automatically adapts to `http` or `https` based on host reachability to prevent SSL Handshake failures from falsely triggering verification errors.

---

## 1. Overview & Core Concept

When a user approves access in the Web Portal, WordPress generates a secure 16-character Application Password and redirects to the app callback. If the app's automated connection check fails (due to strict local server policies, SSL/HTTPS handshake issues on non-SSL test hosts, or query parameter parsing nuances), the app falls back to saving the site offline using `addNewSite`. 

However, `addNewSite` was hardcoded to user `"admin"`. This plan corrects the parameters so the actual authenticated username is preserved across all sync layers.

---

## 2. Technical Architecture & Data Strategy

```
┌──────────────────────────────────────────────────────────────┐
│                  WordPress Web Portal Login                  │
│  User authenticates -> Clicks "Approve"                     │
└──────────────────────────────┬───────────────────────────────┘
                               │ Redirect callback
                               ▼
┌──────────────────────────────────────────────────────────────┐
│             WordPressAuthWebViewDialog Interceptor            │
│  Extracts: siteurl, user_login, password                      │
└──────────────────────────────┬───────────────────────────────┘
                               │ Trigger Connection Check
                               ▼
┌──────────────────────────────────────────────────────────────┐
│             verifyAndConnectWordPressSite (ViewModel)        │
│  Tries live API test with extracted userLogin and password    │
└──────────────┬───────────────────────────────┬───────────────┘
               │                               │
               │ (If Succeeded)                │ (If Failed / SSL Handshake Error)
               ▼                               ▼
┌──────────────────────────────┐┌──────────────────────────────┐
│     loginToWordPressSite     ││     addNewSite (Fallback)    │
│  Saves with real userLogin   ││  OLD: Hardcoded "admin" ❌  │
│  and begins live sync        ││  NEW: Real userLogin ✅     │
└──────────────┬───────────────┘└──────────────┬───────────────┘
               │                               │
               └───────────────┬───────────────┘
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                 Secure Local Room DB Storage                 │
│  Saves SiteEntity: username = userLogin, appPasswordToken    │
└──────────────────────────────┬───────────────────────────────┘
                               │ Load Dashboard
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                     WordPress REST Sync                      │
│  Queries endpoints using correctly paired username & password │
│  Bypasses 401 Unauthorized errors and populates dashboard    │
└──────────────────────────────────────────────────────────────┘
```

---

## 3. Implementation Steps

1. **Update Repository Access (`WPHubRepository.kt`):**
   - Modify `addNewSite` signature to accept `username: String`.
   - Pass `usernameOrEmail = username` to `loginToWordPressSite` instead of `"admin"`.

2. **Update ViewModel Protocol (`WPHubViewModel.kt`):**
   - Modify `addNewSite` signature to accept `username: String`.
   - Pass the `username` to the modified repository method.

3. **Update Connection Flow Logic (`WordPressConnectionScreen.kt`):**
   - In the `onError` lambda of `verifyAndConnectWordPressSite`, invoke `viewModel.addNewSite` with the extracted `userLogin` instead of just name, url, and password.
   - Improve URL cleaning to preserve `http://` or `https://` based on what the user originally entered instead of forcefully prepending `https://` if it already has a protocol.
