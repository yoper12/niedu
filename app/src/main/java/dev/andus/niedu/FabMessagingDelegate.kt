package dev.andus.niedu

import org.json.JSONObject
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.WebExtension

class FabMessagingDelegate(private val activity: MainActivity) : WebExtension.MessageDelegate {
    override fun onMessage(
        nativeApp: String,
        message: Any,
        sender: WebExtension.MessageSender
    ): GeckoResult<Any>? {
        if (nativeApp != "dev.andus.niedu.fab") return null

        try {
            val msgObj = message as JSONObject
            val action = msgObj.optString("action")
            val url = msgObj.optString("url")

            if (action == "updateFab") {
                activity.runOnUiThread {
                    activity.updateFabPosition(url)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return GeckoResult.fromValue(JSONObject())
    }
}