package com.example.biblefob

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.biblefob.navigation.BibleFobApp
import com.example.biblefob.ui.theme.BibleFobTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deepLinkUri = intent?.data
        if (deepLinkUri != null && !isSupportedPassageUri(deepLinkUri)) {
            openInExternalBrowser(deepLinkUri)
            finish()
            return
        }

        setContent {
            BibleFobTheme {
                BibleFobApp()
            }
        }
    }

    private fun isSupportedPassageUri(uri: Uri): Boolean {
        return uri.path == "/passage"
    }

    private fun openInExternalBrowser(uri: Uri) {
        startActivity(
            Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
        )
    }
}
