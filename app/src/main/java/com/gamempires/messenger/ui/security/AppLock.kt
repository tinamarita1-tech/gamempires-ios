package com.gamempires.messenger.ui.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Optional fingerprint lock: when turned on in Settings, the app asks for the fingerprint (or the phone's
 * screen lock as a fallback) when it is opened, and again after it has been in the background for 30 s.
 * Incoming calls are never blocked by the lock.
 */
object AppLock {
    private const val PREFS = "gamempires_security"
    private const val KEY_ENABLED = "fingerprint_lock"
    private const val RELOCK_AFTER_MS = 30_000L

    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()
    private var backgroundSince = 0L // 0 = fresh process

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context) = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (!enabled) _locked.value = false
    }

    /** True when the phone has a fingerprint/face or at least a screen lock set up. */
    fun isAvailable(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val manager = context.getSystemService(BiometricManager::class.java) ?: return false
            manager.canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
        } else {
            context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true
        }
    }

    fun onForeground(context: Context, callLaunch: Boolean) {
        if (!isEnabled(context) || callLaunch) return
        val away = backgroundSince == 0L || SystemClock.elapsedRealtime() - backgroundSince > RELOCK_AFTER_MS
        if (away) _locked.value = true
    }

    fun onBackground() {
        backgroundSince = SystemClock.elapsedRealtime()
    }

    fun authenticate(context: Context, title: String, onSuccess: () -> Unit, onError: (String) -> Unit = {}) {
        val activity = context.findActivity() ?: return onError("Cannot show the fingerprint prompt here")
        val builder = BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle("Use your fingerprint or the phone's screen lock")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(AUTHENTICATORS)
        } else {
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true)
        }
        runCatching {
            builder.build().authenticate(CancellationSignal(), activity.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onError(errString.toString())
                })
        }.onFailure { onError(it.message ?: "Fingerprint prompt failed") }
    }

    fun unlock(context: Context) {
        authenticate(context, "Unlock GamEmpires Messenger", onSuccess = { _locked.value = false })
    }

    private val AUTHENTICATORS: Int
        get() = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    private fun Context.findActivity(): Activity? {
        var c: Context? = this
        while (c is ContextWrapper) {
            if (c is Activity) return c
            c = c.baseContext
        }
        return null
    }
}
