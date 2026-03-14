package dev.andus.niedu

import android.content.SharedPreferences
import android.os.Bundle
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebExtensionController
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.edit
import androidx.activity.enableEdgeToEdge
import com.google.android.material.color.DynamicColors
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.mozilla.geckoview.BuildConfig
import org.mozilla.geckoview.GeckoRuntimeSettings

class MainActivity : AppCompatActivity() {

    private lateinit var geckoView: GeckoView
    private lateinit var geckoSession: GeckoSession
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var extensionController: WebExtensionController
    private var baseUrl: String? = null
    private var symbol: String? = null
    private var journalType: String? = null
    companion object {
        private lateinit var geckoRuntime: GeckoRuntime
        private var isRuntimeInitialized = false
    }
    val navigationDelegate = MyNavigationDelegate(this) { canGoBack ->
        backCallback.isEnabled = canGoBack
    }
    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            geckoSession.goBack()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main_activity)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
    val density = resources.displayMetrics.density

    findViewById<FrameLayout>(R.id.frameLayout).setPadding(
        systemBars.left, systemBars.top, systemBars.right, systemBars.bottom
    )

    val fab = findViewById<FloatingActionButton>(R.id.changeAccountFab)
    val fabParams = fab.layoutParams as ViewGroup.MarginLayoutParams
    fabParams.bottomMargin = systemBars.bottom + (16 * density).toInt()
    fabParams.rightMargin = systemBars.right + (16 * density).toInt()
    fab.layoutParams = fabParams

    val progressBar = findViewById<LinearProgressIndicator>(R.id.linearProgressBar)
    val pbParams = progressBar.layoutParams as ViewGroup.MarginLayoutParams
    pbParams.bottomMargin = systemBars.bottom
    progressBar.layoutParams = pbParams

    insets
}

        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)

        journalType = getJournalType()
        if (journalType == null) {
            showJournalTypeDialog()
        }

        symbol = getSymbol()
        baseUrl = getBaseUrl()

        geckoView = findViewById(R.id.geckoView)
        setupGeckoView()

        val fab = findViewById<FloatingActionButton>(R.id.changeAccountFab)
        fab.setOnClickListener {
            showJournalTypeDialog()
        }

        installExtensions()
        loadLoginPage()

        onBackPressedDispatcher.addCallback(this, backCallback)

        UpdateChecker(this).checkForUpdate()
    }

    private fun setupGeckoView() {
        if (!isRuntimeInitialized) {
            val runtimeSettings = GeckoRuntimeSettings.Builder()
                .consoleOutput(BuildConfig.DEBUG)
                .build()
            geckoRuntime = GeckoRuntime.create(this, runtimeSettings)
            isRuntimeInitialized = true
        }
        extensionController = geckoRuntime.webExtensionController

        val settings = GeckoSessionSettings.Builder()
            .allowJavascript(true)
            .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
            .build()

        geckoSession = GeckoSession(settings)

        geckoSession.navigationDelegate = navigationDelegate
        geckoSession.promptDelegate = MyPromptDelegate(this)

        val loadingOverlay = findViewById<View>(R.id.loadingOverlay)
        val linearProgressBar = findViewById<LinearProgressIndicator>(R.id.linearProgressBar)

        geckoSession.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                loadingOverlay.visibility = View.VISIBLE
                linearProgressBar.visibility = View.VISIBLE
                linearProgressBar.progress = 0
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                linearProgressBar.setProgressCompat(progress, true)
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                loadingOverlay.visibility = View.GONE
                linearProgressBar.visibility = View.GONE
            }
        }

        geckoSession.open(geckoRuntime)
        geckoView.setSession(geckoSession)
    }

    private fun installExtensions() {
        geckoRuntime.webExtensionController.ensureBuiltIn(
            "resource://android/assets/ifv/",
            "j.skup.test@gmail.com"
        )
        geckoRuntime.webExtensionController.ensureBuiltIn(
            "resource://android/assets/autoLogin/",
            "vulcan-auto-login@andus.dev"
        )
        geckoRuntime.webExtensionController.ensureBuiltIn(
            "resource://android/assets/wasm_accelerated_solver_module_for_vulcan-1.2/",
            "wasm@andus.dev"
        )
        geckoRuntime.webExtensionController.ensureBuiltIn(
            "resource://android/assets/fabController/",
            "fab-controller@andus.dev",
        ).accept { extension ->
            runOnUiThread {
                if (extension == null) {
                    return@runOnUiThread
                }
                geckoSession.webExtensionController.setMessageDelegate(
                    extension,
                    FabMessagingDelegate(this@MainActivity),
                    "dev.andus.niedu.fab"
                )
            }
        }
    }

    private fun loadLoginPage() {
        if (journalType == "zwykły" && symbol == null) {
            showSymbolInputDialog()
        } else {
            val loginUrl = when (journalType) {
                "zwykły" -> "https://dziennik-uczen.vulcan.net.pl/$symbol/App"
                else -> "https://eduvulcan.pl/logowanie"
            }
            geckoSession.loadUri(loginUrl)
        }
    }

    fun updateFabPosition(url: String?) {
        val fab = findViewById<FloatingActionButton>(R.id.changeAccountFab)
        val density = resources.displayMetrics.density

        val isTimetable = url?.endsWith("planZajec") == true
        val isHome = url?.contains("dziennik-logowanie") == true || url?.contains("uczen.") == false

        val targetTranslationY = when {
            isTimetable -> -114f * density
            !isHome -> -70f * density
            else -> 0f
        }

        fab.animate()
            .translationY(targetTranslationY)
            .setDuration(250)
            .start()
    }

    private fun saveSymbol(symbol: String) {
        sharedPreferences.edit {
            putString("symbol", symbol)
        }
    }

    private fun getSymbol(): String? {
        return sharedPreferences.getString("symbol", null)
    }

    private fun getBaseUrl(): String? {
        return sharedPreferences.getString("base_url", null)
    }

    private fun getJournalType(): String? {
        return sharedPreferences.getString("journal_type", null)
    }

    private fun saveJournalType(type: String) {
        sharedPreferences.edit {
            putString("journal_type", type)
        }
        journalType = type
    }

    private fun showJournalTypeDialog() {
        val options = arrayOf("edu", "zwykły")
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("Wybierz typ dziennika")
        builder.setItems(options) { _, which ->
            val selectedType = options[which]
            saveJournalType(selectedType)
            Toast.makeText(this, "Wybrano: $selectedType", Toast.LENGTH_SHORT).show()
            loadLoginPage()
        }
        builder.show()
    }

    private fun showSymbolInputDialog() {
        val input = EditText(this)
        input.hint = "Wpisz symbol"
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Wpisz symbol")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val symbolInput = input.text.toString()
                if (symbolInput.isNotEmpty()) {
                    symbol = symbolInput
                    saveSymbol(symbolInput)
                    loadLoginPage()
                } else {
                    Toast.makeText(this, "Symbol nie może być pusty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Anuluj") { dialog, _ -> dialog.cancel() }
            .create()

        dialog.show()
    }

    override fun onDestroy() {
        geckoSession.close()
        super.onDestroy()
    }
}