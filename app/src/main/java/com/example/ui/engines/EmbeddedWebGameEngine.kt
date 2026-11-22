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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.audio.GameSfx
import com.example.audio.VaultAudioEngine
import org.json.JSONObject

private class VaultJsBridge(
  private val onAwardXp: (String, Int) -> Unit,
  private val onSubmitScore: (String, Int) -> Unit,
  private val onUnlockAchievement: (String) -> Unit,
  private val onSaveQuickResume: (String, String, String) -> Unit,
  private val onClearQuickResume: (String) -> Unit,
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

  @JavascriptInterface
  fun saveQuickResume(gameId: String, summaryLabel: String, stateJson: String) {
    onSaveQuickResume(gameId, summaryLabel, stateJson)
  }

  @JavascriptInterface
  fun clearQuickResume(gameId: String) {
    onClearQuickResume(gameId)
  }

  @JavascriptInterface
  fun playSfx(sfxName: String) {
    runCatching { GameSfx.valueOf(sfxName.uppercase()) }
      .getOrNull()
      ?.let { VaultAudioEngine.playGameSfx(it) }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbeddedWebGameEngine(
  gameId: String,
  assetUrl: String,
  customHtmlContent: String?,
  initialResumeJson: String? = null,
  saveTriggerCount: Int = 0,
  onAwardXp: (String, Int) -> Unit,
  onSubmitScore: (String, Int) -> Unit,
  onUnlockAchievement: (String) -> Unit,
  onSaveQuickResume: (String, String, String) -> Unit = { _, _, _ -> },
  onClearQuickResume: (String) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  var isLoading by remember { mutableStateOf(true) }
  var webViewRef by remember { mutableStateOf<WebView?>(null) }

  val latestAwardXp by rememberUpdatedState(onAwardXp)
  val latestSubmitScore by rememberUpdatedState(onSubmitScore)
  val latestUnlockAch by rememberUpdatedState(onUnlockAchievement)
  val latestSaveResume by rememberUpdatedState(onSaveQuickResume)
  val latestClearResume by rememberUpdatedState(onClearQuickResume)
  val latestInitialResume by rememberUpdatedState(initialResumeJson)

  LaunchedEffect(saveTriggerCount) {
    if (saveTriggerCount > 0) {
      webViewRef?.evaluateJavascript(
        "window.VaultRequestQuickResume && window.VaultRequestQuickResume();",
        null,
      )
    }
  }

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
              onSaveQuickResume = { gId, label, json ->
                post { latestSaveResume(gId.ifBlank { gameId }, label, json) }
              },
              onClearQuickResume = { gId -> post { latestClearResume(gId.ifBlank { gameId }) } },
            ),
            "VaultBridge",
          )
          webViewClient =
            object : WebViewClient() {
              override fun onPageFinished(view: WebView?, url: String?) {
                isLoading = false
                val saved = latestInitialResume
                if (!saved.isNullOrBlank()) {
                  val quoted = JSONObject.quote(saved)
                  view?.evaluateJavascript(
                    "window.VaultApplyQuickResume && window.VaultApplyQuickResume($quoted);",
                    null,
                  )
                }
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
