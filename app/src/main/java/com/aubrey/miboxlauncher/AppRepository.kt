package com.aubrey.miboxlauncher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable

/** Apps plus their artwork. Loaded off the main thread. */
class LoadedApps(
    val apps: List<AppEntry>,
    private val art: Map<String, Art>,
) {
    class Art(val drawable: Drawable, val isBanner: Boolean)

    fun artFor(packageName: String): Art? = art[packageName]
}

object AppRepository {
    /** Lists every launchable app except this launcher. Call from a background thread. */
    fun load(context: Context): LoadedApps {
        val pm = context.packageManager
        val leanback = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val phone = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val infos = pm.queryIntentActivities(leanback, 0) + pm.queryIntentActivities(phone, 0)

        val apps = mutableListOf<AppEntry>()
        val art = mutableMapOf<String, LoadedApps.Art>()
        for (info in infos) {
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName || art.containsKey(pkg)) continue
            apps += AppEntry(pkg, info.loadLabel(pm).toString(), info.activityInfo.name)
            val banner = info.activityInfo.loadBanner(pm)
            art[pkg] = if (banner != null) {
                LoadedApps.Art(banner, isBanner = true)
            } else {
                LoadedApps.Art(info.loadIcon(pm), isBanner = false)
            }
        }
        return LoadedApps(apps, art)
    }
}

/** Remembers the pinned packages, in order. */
class PinStore(private val prefs: SharedPreferences) {
    fun read(): List<String> =
        prefs.getString(KEY, "").orEmpty().split(SEPARATOR).filter { it.isNotEmpty() }

    fun write(pins: List<String>) {
        prefs.edit().putString(KEY, pins.joinToString(SEPARATOR)).apply()
    }

    private companion object {
        const val KEY = "pins"
        const val SEPARATOR = "\n"
    }
}
