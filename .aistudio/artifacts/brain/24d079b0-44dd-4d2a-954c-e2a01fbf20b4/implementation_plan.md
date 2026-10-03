# GutenbergEditor Hybrid WebView Component

Create a mobile-optimized `GutenbergEditor` Jetpack Compose component that wraps an Android `WebView`, injecting custom JavaScript and CSS or loading the authentic WordPress Gutenberg editor (`wp-admin/post-new.php` / `post.php`) with a dual-mode fallback to an embedded local HTML Gutenberg block engine with bidirectional JS-to-Kotlin `JavascriptInterface` communication.

---

### User Review & Critical Decisions

- **Hybrid Strategy**:
  - **Primary Engine**: Loads the remote WordPress site's authentic Gutenberg block editor (`/wp-admin/post-new.php?post_type=post`) inside an authenticated Android `WebView` using stored session cookies/Application Credentials, hiding WP admin chrome (`#adminmenumain`, `#wpadminbar`) via CSS injection for a distraction-free mobile screen.
  - **Offline/Standalone Local Engine**: Provides an embedded HTML5 Gutenberg JS Block Engine (`/assets/gutenberg_editor.html`) powered by `@wordpress/blocks` & `@wordpress/block-editor` bundles for offline drafting, syncing directly with Room local database entities.
- **JavascriptInterface Bridge**:
  - Exposes `@JavascriptInterface` (`GutenbergNativeBridge`) to capture `getBlocks()`, `savePost()`, and `onContentChange()` events from the WebView back to Kotlin state (`ViewModel`).

---

### 1. Overview & Core Concept

- **What It Does**: Renders a rich mobile Gutenberg post editor inside a Jetpack Compose `AndroidView(factory = { WebView(it) })`. Supports block insertion, live preview, publish settings, and full mobile toolbar integration.
- **Key Value**: Delivers the exact WordPress block editor experience natively inside the Android mobile app with instant bidirectional synchronization.

---

### 2. User Experience & Visual Design

- **Top Bar Controls**: Compact mobile top bar with Back, Undo/Redo, Block Inserter (`+`), Visual/Code mode switch, and Publish/Update button.
- **Mobile Floating Block Inserter**: Quick-add chips for Paragraph, Heading, Image, Quote, Code, and Custom HTML at the bottom of the canvas.
- **WebView Styling**: CSS overrides to remove WP admin sidebar, header, and footer, leaving a full-width mobile Gutenberg canvas.

---

### 3. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                   GutenbergEditor Composable                 │
│ ┌───────────────────────┐   ┌─────────────────────────────┐ │
│ │ Top Bar & Block Chips │   │ WebView (AndroidView)       │ │
│ └───────────┬───────────┘   └──────────────┬──────────────┘ │
└─────────────┼──────────────────────────────┼────────────────┘
              │                              │
              ▼                              ▼
    ┌──────────────────┐          ┌───────────────────────┐
    │  GutenbergState  │◄─────────┤ GutenbergNativeBridge │
    └─────────┬────────┘          └───────────────────────┘
              │                              ▲
              │                              │
              ▼                              │
    ┌──────────────────┐          ┌───────────────────────┐
    │ WPHubViewModel   │─────────►│ evaluateJavascript()  │
    └──────────────────┘          └───────────────────────┘
```

#### Key Components:
1. `GutenbergEditor.kt`: Compose wrapper containing `AndroidView` for `WebView`, top toolbar, floating block toolbar, and publishing modal.
2. `GutenbergNativeBridge.kt`: `@JavascriptInterface` bridging JS `window.AndroidGutenberg.onPostContentChanged(json)` to Kotlin Flow/State.
3. `GutenbergJsInjector.kt`: Injects mobile responsive CSS (`#wpbody-content { padding: 0 }`, `.edit-post-header { top: 0 }`) and custom JS bridge scripts into the WebView upon `onPageFinished`.

---

### 4. Step-by-Step Implementation Steps

1. **Create `GutenbergNativeBridge`**:
   - Define interface methods: `onPostUpdated(title, content, jsonBlocks)`, `onEditorReady()`, `onBlockSelected(blockName)`.
2. **Create `GutenbergEditor.kt` Composable**:
   - Configure `WebView` settings: `javaScriptEnabled = true`, `domStorageEnabled = true`, `databaseEnabled = true`.
   - Setup `WebViewClient` to handle cookies and inject custom CSS for hiding WP Admin chrome.
3. **Integrate into Content & Edit Flows**:
   - Add `GutenbergEditor` tab or full-screen dialog in `ContentScreen` and `WPHubApp`.
