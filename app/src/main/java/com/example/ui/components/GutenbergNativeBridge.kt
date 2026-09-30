package com.example.ui.components

import android.webkit.JavascriptInterface

class GutenbergNativeBridge(
    private val onContentChanged: (title: String, content: String, jsonBlocks: String) -> Unit,
    private val onEditorReady: () -> Unit,
    private val onPostSaved: (status: String) -> Unit
) {
    @JavascriptInterface
    fun onPostContentChanged(title: String, content: String, jsonBlocks: String) {
        onContentChanged(title, content, jsonBlocks)
    }

    @JavascriptInterface
    fun notifyEditorReady() {
        onEditorReady()
    }

    @JavascriptInterface
    fun notifyPostSaved(status: String) {
        onPostSaved(status)
    }
}
