package com.f7developer.tapeback

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.f7developer.tapeback.ui.RootViewModel
import com.f7developer.tapeback.ui.Routes
import com.f7developer.tapeback.ui.TapeBackRoot
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val container get() = (application as TapeBackApp).container
    private val pendingRoute = MutableStateFlow<String?>(null)

    private val rootVm: RootViewModel by viewModels {
        viewModelFactory { initializer { RootViewModel((application as TapeBackApp).container) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splash.setKeepOnScreenCondition { rootVm.settings.value == null }
        splash.setOnExitAnimationListener { provider ->
            val view: View = provider.view
            view.animate()
                .alpha(0f)
                .scaleX(1.12f)
                .scaleY(1.12f)
                .setDuration(320L)
                .withEndAction { provider.remove() }
                .start()
        }
        if (savedInstanceState == null) handleIntent(intent)
        setContent { TapeBackRoot(rootVm, pendingRoute) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_RECORD -> {
                container.recordRequest.value = true
                pendingRoute.value = Routes.DECK
            }
            ACTION_DIARY -> pendingRoute.value = Routes.DIARY
            ACTION_OPEN_CAPSULES -> pendingRoute.value = Routes.CAPSULES
        }
    }

    companion object {
        const val ACTION_RECORD = "com.f7developer.tapeback.action.RECORD"
        const val ACTION_DIARY = "com.f7developer.tapeback.action.DIARY"
        const val ACTION_OPEN_CAPSULES = "com.f7developer.tapeback.action.CAPSULES"
    }
}
