package com.f7developer.tapeback.ui.lock

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.f7developer.tapeback.R
import com.f7developer.tapeback.ui.components.AppIconArt
import com.f7developer.tapeback.ui.components.retroBackground
import com.f7developer.tapeback.ui.theme.BungeeFamily
import com.f7developer.tapeback.ui.theme.Retro

/** True when the phone has a screen lock (PIN, pattern, password or biometrics) to verify against. */
fun canUseDeviceLock(context: Context): Boolean =
    context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

/**
 * Returns a function that asks the user to verify with fingerprint, face or screen lock, and reports
 * success or failure to [onResult]. Android 10+ uses [BiometricPrompt] with a device-credential
 * fallback; Android 8–9 uses the system confirm-credential screen.
 */
@Composable
fun rememberDeviceAuthenticator(onResult: (Boolean) -> Unit): (title: String, subtitle: String) -> Unit {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val callback by rememberUpdatedState(onResult)
    val legacy = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        callback(it.resultCode == Activity.RESULT_OK)
    }
    return { title, subtitle ->
        when {
            !canUseDeviceLock(context) -> callback(false)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && activity != null -> {
                val builder = BiometricPrompt.Builder(activity).setTitle(title).setSubtitle(subtitle)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    builder.setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                    )
                } else {
                    @Suppress("DEPRECATION")
                    builder.setDeviceCredentialAllowed(true)
                }
                runCatching {
                    builder.build().authenticate(
                        CancellationSignal(),
                        context.mainExecutor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = callback(true)
                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = callback(false)
                        },
                    )
                }.onFailure { callback(false) }
            }
            else -> {
                val keyguard = context.getSystemService(KeyguardManager::class.java)
                @Suppress("DEPRECATION")
                val intent = keyguard?.createConfirmDeviceCredentialIntent(title, subtitle)
                if (intent != null) legacy.launch(intent) else callback(false)
            }
        }
    }
}

/** Full-screen cover shown while the app is locked. Prompts once on appear; the button retries. */
@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val palette = Retro.palette
    val title = stringResource(R.string.lock_prompt_title)
    val subtitle = stringResource(R.string.lock_prompt_subtitle)
    val authenticate = rememberDeviceAuthenticator { ok -> if (ok) onUnlocked() }
    LaunchedEffect(Unit) { authenticate(title, subtitle) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .retroBackground(palette.background, palette.backgroundGlow)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppIconArt(size = 96.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.brand_wordmark),
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = BungeeFamily),
            color = palette.accent,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.lock_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp),
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = { authenticate(title, subtitle) }) {
            Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.lock_unlock))
        }
    }
}
