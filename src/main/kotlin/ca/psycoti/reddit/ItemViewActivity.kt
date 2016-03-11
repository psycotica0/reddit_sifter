package ca.psycoti.reddit

import android.os.Bundle
import android.app.Activity
import android.webkit.WebView
import android.webkit.WebViewClient

import ca.psycoti.reddit.models.Entry

open class ItemViewActivity : Activity() {
  companion object {
    val URL = "url"
  }
  lateinit var webView: WebView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.item_view)

    webView = findViewById(R.id.item_webview) as WebView
    webView.setWebViewClient(WebViewClient())
    webView.settings.javaScriptEnabled = true

    val url = getIntent().getStringExtra(URL)
    webView.loadUrl(url)
  }
}
