package com.example.biblefob

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.biblefob.navigation.BibleFobApp
import com.example.biblefob.ui.theme.BibleFobTheme

class MainActivity : ComponentActivity() {
    private var deepLinkUriString by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!handleIncomingDeepLink(intent)) {
            return
        }

        setContent {
            BibleFobTheme {
                BibleFobApp(deepLinkUriString = deepLinkUriString)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (!handleIncomingDeepLink(intent)) {
            return
        }
    }

    private fun handleIncomingDeepLink(intent: Intent?): Boolean {
        val deepLinkUri = intent?.data
        deepLinkUriString = deepLinkUri?.toString()

        if (deepLinkUri == null || isSupportedPassageUri(deepLinkUri)) {
            return true
        }

        openInExternalBrowser(deepLinkUri)
        finish()
        return false
    }

    private fun isSupportedPassageUri(uri: Uri): Boolean {
        return uri.path == "/passage"
    }

    private fun openInExternalBrowser(uri: Uri) {
        val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        val externalBrowserActivity = packageManager
            .queryIntentActivities(browserIntent, 0)
            .firstOrNull { it.activityInfo.packageName != packageName }

        if (externalBrowserActivity != null) {
            browserIntent.setClassName(
                externalBrowserActivity.activityInfo.packageName,
                externalBrowserActivity.activityInfo.name
            )
        }

        startActivity(browserIntent)
    }
}
