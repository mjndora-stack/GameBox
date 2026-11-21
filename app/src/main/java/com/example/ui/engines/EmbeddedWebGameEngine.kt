package com.example.ui.engines

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private class VaultJsBridge(
  private val onAwardXp: (String, Int) -> Unit,
  private val onSubmitScore: (String, Int) -> Unit,
  private val onUnlockAchievement: (String) -> Unit,
) {
  @JavascriptInterface
  fun awardXp(gameId: String, amount: Int) {
    onAwardXp(gameId, amount)
  }

  @JavascriptInterface
  fun submitScore(gameId: String, score: Int) {
    onSubmitScore(gameId, score)
  }

  @JavascriptInterface
  fun unlockAchievement(achievementId: String) {
    onUnlockAchievement(achievementId)
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbeddedWebGameEngine(
  gameId: String,
  assetUrl: String,
  customHtmlContent: String?,
  onAwardXp: (String, Int) -> Unit,
  onSubmitScore: (String, Int) -> Unit,
  onUnlockAchievement: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var isLoading by remember { mutableStateOf(true) }
  var webViewRef by remember { mutableStateOf<WebView?>(null) }

  val latestAwardXp by rememberUpdatedState(onAwardXp)
  val latestSubmitScore by rememberUpdatedState(onSubmitScore)
  val latestUnlockAch by rememberUpdatedState(onUnlockAchievement)

  DisposableEffect(Unit) {
    onDispose {
      webViewRef?.apply {
        stopLoading()
        removeJavascriptInterface("VaultBridge")
        destroy()
      }
      webViewRef = null
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    AndroidView(
      modifier = Modifier.fillMaxSize(),
      factory = { context ->
        WebView(context).apply {
          layoutParams =
            ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT,
            )
          settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            blockNetworkLoads = true // Strictly offline sandboxed execution
            cacheMode = WebSettings.LOAD_NO_CACHE
            useWideViewPort = true
            loadWithOverviewMode = true
          }
          isFocusable = true
          isFocusableInTouchMode = true
          addJavascriptInterface(
            VaultJsBridge(
              onAwardXp = { gId, amt -> post { latestAwardXp(gId.ifBlank { gameId }, amt) } },
              onSubmitScore = { gId, sc -> post { latestSubmitScore(gId.ifBlank { gameId }, sc) } },
              onUnlockAchievement = { achId -> post { latestUnlockAch(achId) } },
            ),
            "VaultBridge",
          )
          webViewClient =
            object : WebViewClient() {
              override fun onPageFinished(view: WebView?, url: String?) {
                isLoading = false
              }
            }
          if (!customHtmlContent.isNullOrBlank()) {
            loadDataWithBaseURL("file:///android_asset/games/", customHtmlContent, "text/html", "UTF-8", null)
          } else {
            loadUrl(assetUrl)
          }
          webViewRef = this
        }
      },
    )

    if (isLoading) {
      CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
  }
}
