package com.aubrey.miboxlauncher

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var grid: RecyclerView
    private lateinit var adapter: HomeAdapter
    private lateinit var pinStore: PinStore
    private val loader = Executors.newSingleThreadExecutor()

    private var loaded: LoadedApps? = null
    private var pins: List<String> = emptyList()
    private var focusFirstTile = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pinStore = PinStore(getSharedPreferences("launcher", MODE_PRIVATE))

        adapter = HomeAdapter(onLaunch = ::launch, onTogglePin = ::togglePin)
        grid = findViewById(R.id.grid)
        grid.layoutManager = GridLayoutManager(this, HomeModel.COLUMNS).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int) = adapter.spanSize(position)
            }
        }
        grid.adapter = adapter
        grid.itemAnimator = null

        findViewById<View>(R.id.settings).setOnClickListener { openSettings() }
    }

    override fun onResume() {
        super.onResume()
        pins = pinStore.read()
        loader.execute {
            val result = AppRepository.load(this)
            runOnUiThread {
                loaded = result
                render()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Home pressed while the launcher is already showing: go back to the top.
        focusFirstTile = true
        grid.scrollToPosition(0)
        render()
    }

    override fun onDestroy() {
        loader.shutdown()
        super.onDestroy()
    }

    /** A launcher has nowhere to go back to. */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() = Unit

    private fun render() {
        val current = loaded ?: return
        adapter.submit(HomeModel.build(current.apps, pins), current)
        if (focusFirstTile) {
            focusFirstTile = false
            grid.post {
                val position = adapter.firstAppPosition()
                if (position >= 0) {
                    grid.scrollToPosition(position)
                    grid.post { grid.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus() }
                }
            }
        }
    }

    private fun launch(entry: AppEntry) {
        val intent = Intent(Intent.ACTION_MAIN)
            .setClassName(entry.packageName, entry.activityName)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.cannot_open, entry.label), Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePin(entry: AppEntry) {
        val updated = HomeModel.togglePin(pins, entry.packageName)
        if (updated == pins) {
            Toast.makeText(this, R.string.favourites_full, Toast.LENGTH_SHORT).show()
            return
        }
        pins = updated
        pinStore.write(updated)
        val message = if (entry.packageName in updated) R.string.pinned else R.string.unpinned
        Toast.makeText(this, getString(message, entry.label), Toast.LENGTH_SHORT).show()
        render()
    }

    private fun openSettings() {
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_settings, Toast.LENGTH_SHORT).show()
        }
    }
}
