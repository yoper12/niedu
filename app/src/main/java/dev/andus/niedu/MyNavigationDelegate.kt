package dev.andus.niedu

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.NavigationDelegate
import androidx.core.net.toUri

class MyNavigationDelegate(private val context: Context,
                           private val onCanGoBackChanged: (Boolean) -> Unit
) : NavigationDelegate {
    var canGoBack: Boolean = false

    override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
        this.canGoBack = canGoBack
        onCanGoBackChanged(canGoBack)
    }

    override fun onLoadRequest(session: GeckoSession, request: NavigationDelegate.LoadRequest): GeckoResult<AllowOrDeny> {
        if (request.target == NavigationDelegate.TARGET_WINDOW_NEW) {
            val uri = request.uri

            val intent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(true)
                .build()
            intent.launchUrl(context, uri.toUri())
        }

        return GeckoResult.fromValue(AllowOrDeny.ALLOW)
    }
}