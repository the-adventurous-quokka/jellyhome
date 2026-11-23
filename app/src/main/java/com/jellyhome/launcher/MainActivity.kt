package com.jellyhome.launcher

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * JellyHome: a full-screen home screen for Fire TV / Android TV.
 * The UI lives in assets/index.html; this activity hosts it and exposes a small
 * "Android" bridge for listing/launching apps and handing videos to a player.
 */
class MainActivity : ComponentActivity() {

    private lateinit var web: WebView
    private val appHost = "appassets.androidplatform.net"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Serve assets from an https origin so YouTube embeds get a proper referrer.
        val loader = WebViewAssetLoader.Builder()
            .setDomain(appHost)
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web = WebView(this)
        web.setBackgroundColor(Color.BLACK)
        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            // Most home Jellyfin servers are plain http on the LAN.
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            allowFileAccess = false
            // Ignore the TV's accessibility text size and any zooming; the page scales itself.
            textZoom = 100
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            useWideViewPort = false
            loadWithOverviewMode = false
        }
        web.setInitialScale(0)
        web.isVerticalScrollBarEnabled = false
        web.isHorizontalScrollBarEnabled = false
        // Re-fit the page whenever the view's real size is known or changes.
        web.addOnLayoutChangeListener { _, l, t, r, b, oldL, oldT, oldR, oldB ->
            if (r - l != oldR - oldL || b - t != oldB - oldT) web.evaluateJavascript("window.jhFit && jhFit()", null)
        }
        web.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(request.url)

            // Never navigate the home screen itself away from our page.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                request.isForMainFrame && request.url.host != appHost
        }
        web.webChromeClient = object : WebChromeClient() {
            // Hide the grey "play" poster WebView shows before a video starts.
            override fun getDefaultVideoPoster(): Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
        web.addJavascriptInterface(Bridge(), "Android")
        setContentView(web)
        web.isFocusable = true
        web.requestFocus()

        // Back never leaves the home screen; the page decides what it means.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript("window.jhBack && jhBack()", null)
            }
        })

        web.loadUrl("https://$appHost/assets/index.html")
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
        web.evaluateJavascript("window.jhResume && jhResume()", null)
    }

    override fun onPause() {
        web.evaluateJavascript("window.jhPause && jhPause()", null)
        web.onPause()
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Home pressed while we're already in front: go back to the top of the page.
        web.evaluateJavascript("window.jhBack && jhBack()", null)
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }

    private fun toast(msg: String) = runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }

    private fun toDataUrl(d: Drawable, w: Int, h: Int): String {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        d.setBounds(0, 0, w, h)
        d.draw(c)
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        bmp.recycle()
        return "data:image/png;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    inner class Bridge {

        /** Visible size in CSS pixels ("960x540" on a 1080p Fire TV). */
        @JavascriptInterface
        fun screenSize(): String {
            val d = resources.displayMetrics.density
            var w = web.width
            var h = web.height
            if (w <= 0 || h <= 0) { w = resources.displayMetrics.widthPixels; h = resources.displayMetrics.heightPixels }
            return "${Math.round(w / d)}x${Math.round(h / d)}"
        }

        /** Installed launchable apps as JSON: [{pkg, label, icon, banner}] */
        @JavascriptInterface
        fun getApps(): String {
            val pm = packageManager
            val found = LinkedHashMap<String, JSONObject>()
            for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
                val query = Intent(Intent.ACTION_MAIN).addCategory(category)
                for (ri in pm.queryIntentActivities(query, 0)) {
                    val pkg = ri.activityInfo.packageName
                    if (pkg == packageName || found.containsKey(pkg)) continue
                    try {
                        val banner = ri.activityInfo.loadBanner(pm) ?: ri.activityInfo.applicationInfo.loadBanner(pm)
                        val icon = if (banner != null) toDataUrl(banner, 320, 180) else toDataUrl(ri.loadIcon(pm), 144, 144)
                        found[pkg] = JSONObject()
                            .put("pkg", pkg)
                            .put("label", ri.loadLabel(pm).toString())
                            .put("icon", icon)
                            .put("banner", banner != null)
                    } catch (e: Exception) { /* skip apps whose artwork can't be drawn */ }
                }
            }
            val sorted = found.values.sortedBy { it.optString("label").lowercase() }
            return JSONArray(sorted).toString()
        }

        @JavascriptInterface
        fun launchApp(pkg: String) = runOnUiThread {
            val pm = packageManager
            val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
            if (intent == null) { toast("That app isn't installed any more"); return@runOnUiThread }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try { startActivity(intent) } catch (e: Exception) { toast("Couldn't open that app") }
        }

        /** Hand a Jellyfin stream to the TV's video player (VLC, MX Player, Kodi…). */
        @JavascriptInterface
        fun playVideo(url: String, title: String, positionMs: Long) = runOnUiThread {
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(Uri.parse(url), "video/*")
                .putExtra("title", title)
                .putExtra("position", positionMs)   // VLC: resume position in ms
                .putExtra("from_start", positionMs <= 0)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try { startActivity(intent) } catch (e: ActivityNotFoundException) {
                toast("Install a video player such as VLC to play videos")
            }
        }

        @JavascriptInterface
        fun openSettings() = runOnUiThread {
            try { startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            catch (e: Exception) { toast("Couldn't open settings") }
        }
    }
}
