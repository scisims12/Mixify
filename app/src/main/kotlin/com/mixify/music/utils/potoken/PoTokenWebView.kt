*** Begin Patch
*** Update File: app/src/main/kotlin/com/mixify/music/utils/potoken/PoTokenWebView.kt
@@
-        private const val TAG = "PoTokenWebView"
-        private const val GOOGLE_API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw"
+        private const val TAG = "PoTokenWebView"
+        // The API key is injected at build time into BuildConfig.YOUTUBE_API_KEY.
+        // Do NOT hardcode keys in source control.
+        private val GOOGLE_API_KEY: String
+            get() = BuildConfig.YOUTUBE_API_KEY
@@
         private fun makeBotguardServiceRequest(
             url: String,
             data: String,
             handleResponseBody: (String) -> Unit,
         ) {
+        // Guard: ensure API key provided
+        if (GOOGLE_API_KEY.isBlank()) {
+            val ex = PoTokenException("Missing YOUTUBE_API_KEY (set in local.properties or CI).")
+            onInitializationErrorCloseAndCancel(ex)
+            return
+        }
*** End Patch
