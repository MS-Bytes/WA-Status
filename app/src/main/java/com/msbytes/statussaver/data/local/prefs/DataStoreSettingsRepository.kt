package com.msbytes.statussaver.data.local.prefs

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.msbytes.statussaver.domain.model.AppSettings
import com.msbytes.statussaver.domain.model.SortOrder
import com.msbytes.statussaver.domain.model.ThemeMode
import com.msbytes.statussaver.domain.repository.SettingsRepository
import com.msbytes.statussaver.security.PinHasher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

// File names are referenced by res/xml backup rules: "settings" is backed up, "security" never.
private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
private val Context.securityStore: DataStore<Preferences> by preferencesDataStore(name = "security")

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private val store get() = context.settingsStore
    private val security get() = context.securityStore

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val NOTIF_PROMPT = booleanPreferencesKey("notification_prompt_shown")
        val NOTIFY = booleanPreferencesKey("notify_new")
        val LIVE = booleanPreferencesKey("live_detection")
        val BIOMETRIC = booleanPreferencesKey("biometric")
        val SORT = stringPreferencesKey("sort_order")
        val AD_FREE_UNTIL = longPreferencesKey("ad_free_until")
        val SAVE_COUNT = intPreferencesKey("save_count")
        val WIDGET_COUNT = intPreferencesKey("widget_new_count")
        val LAST_SEEN = longPreferencesKey("last_seen")
        val LAST_NOTIFIED = longPreferencesKey("last_notified")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
    }

    private val safeStore = store.data.catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
    private val safeSecurity = security.data.catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }

    override val settings: Flow<AppSettings> =
        kotlinx.coroutines.flow.combine(safeStore, safeSecurity) { p, s ->
            AppSettings(
                themeMode = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
                dynamicColor = p[Keys.DYNAMIC] ?: true,
                onboardingDone = p[Keys.ONBOARDING] ?: false,
                autoSave = p[Keys.AUTO_SAVE] ?: false,
                notifyNewStatuses = p[Keys.NOTIFY] ?: true,
                liveDetection = p[Keys.LIVE] ?: false,
                notificationPromptShown = p[Keys.NOTIF_PROMPT] ?: false,
                appLockEnabled = s[Keys.PIN_HASH] != null,
                biometricEnabled = p[Keys.BIOMETRIC] ?: false,
                sortOrder = p[Keys.SORT]?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() } ?: SortOrder.NEWEST,
                adFreeUntil = p[Keys.AD_FREE_UNTIL] ?: 0L,
            )
        }.distinctUntilChanged()

    override val widgetNewCount: Flow<Int> = safeStore.map { it[Keys.WIDGET_COUNT] ?: 0 }.distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) = put(Keys.THEME, mode.name)
    override suspend fun setDynamicColor(enabled: Boolean) = put(Keys.DYNAMIC, enabled)
    override suspend fun setOnboardingDone() = put(Keys.ONBOARDING, true)
    override suspend fun setAutoSave(enabled: Boolean) = put(Keys.AUTO_SAVE, enabled)
    override suspend fun setNotificationPromptShown() = put(Keys.NOTIF_PROMPT, true)
    override suspend fun setNotifyNewStatuses(enabled: Boolean) = put(Keys.NOTIFY, enabled)
    override suspend fun setLiveDetection(enabled: Boolean) = put(Keys.LIVE, enabled)
    override suspend fun setSortOrder(order: SortOrder) = put(Keys.SORT, order.name)
    override suspend fun setAdFreeUntil(epochMillis: Long) = put(Keys.AD_FREE_UNTIL, epochMillis)
    override suspend fun setBiometricEnabled(enabled: Boolean) = put(Keys.BIOMETRIC, enabled)
    override suspend fun setWidgetNewCount(count: Int) = put(Keys.WIDGET_COUNT, count)

    override suspend fun lastSeen(): Long = safeStore.first()[Keys.LAST_SEEN] ?: 0L
    override suspend fun setLastSeen(epochMillis: Long) = put(Keys.LAST_SEEN, epochMillis)

    override suspend fun lastNotified(): Long = safeStore.first()[Keys.LAST_NOTIFIED] ?: 0L
    override suspend fun setLastNotified(epochMillis: Long) = put(Keys.LAST_NOTIFIED, epochMillis)

    override suspend fun incrementSaveCount(): Int {
        var result = 0
        store.edit { result = (it[Keys.SAVE_COUNT] ?: 0) + 1; it[Keys.SAVE_COUNT] = result }
        return result
    }

    override suspend fun setPin(pin: String?) {
        security.edit { prefs ->
            if (pin == null) {
                prefs.remove(Keys.PIN_HASH)
                prefs.remove(Keys.PIN_SALT)
            } else {
                val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
                val saltText = Base64.encodeToString(salt, Base64.NO_WRAP)
                prefs[Keys.PIN_SALT] = saltText
                prefs[Keys.PIN_HASH] = PinHasher.hash(pin, saltText)
            }
        }
        if (pin == null) setBiometricEnabled(false)
    }

    override suspend fun verifyPin(pin: String): Boolean {
        val prefs = safeSecurity.first()
        val hash = prefs[Keys.PIN_HASH] ?: return false
        val salt = prefs[Keys.PIN_SALT] ?: return false
        return PinHasher.hash(pin, salt) == hash
    }

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        store.edit { it[key] = value }
    }
}
