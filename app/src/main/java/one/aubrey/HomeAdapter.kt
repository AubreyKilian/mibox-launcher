package one.aubrey

import android.graphics.Outline
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HomeAdapter(
    private val onLaunch: (AppEntry) -> Unit,
    private val onTogglePin: (AppEntry) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var cells: List<Cell> = emptyList()
    private var loaded: LoadedApps? = null

    /** Package of the app tile that last had focus. */
    var lastFocusedPackage: String? = null
        private set

    private var topRow: Set<Int> = emptySet()

    fun submit(newCells: List<Cell>, newLoaded: LoadedApps) {
        cells = newCells
        loaded = newLoaded
        topRow = HomeModel.topRowPositions(newCells)
        notifyDataSetChanged()
    }

    fun spanSize(position: Int): Int =
        if (cells[position] is Cell.Header) HomeModel.COLUMNS else 1

    /** Package of the first app tile, or null when there is none. */
    fun firstAppPackage(): String? = cells.firstNotNullOfOrNull { (it as? Cell.App)?.entry?.packageName }

    /** Position of the tile for [packageName], or -1 when it is not shown. */
    fun positionOf(packageName: String): Int =
        cells.indexOfFirst { it is Cell.App && it.entry.packageName == packageName }

    override fun getItemCount() = cells.size

    override fun getItemViewType(position: Int) = when (cells[position]) {
        is Cell.Header -> TYPE_HEADER
        is Cell.App -> TYPE_APP
        is Cell.EmptySlot -> TYPE_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> Simple(inflater.inflate(R.layout.item_header, parent, false))
            TYPE_EMPTY -> Simple(inflater.inflate(R.layout.item_empty, parent, false))
            else -> AppHolder(inflater.inflate(R.layout.item_app, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val cell = cells[position]) {
            is Cell.Header -> (holder.itemView as TextView).text = cell.title
            is Cell.EmptySlot -> holder.itemView.findViewById<TextView>(R.id.hint).apply {
                text = if (cell.showHint) context.getString(R.string.pin_hint) else ""
            }
            is Cell.App -> (holder as AppHolder).bind(cell, isTopRow = position in topRow)
        }
    }

    private class Simple(view: View) : RecyclerView.ViewHolder(view)

    private inner class AppHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.icon)
        private val label: TextView = view.findViewById(R.id.label)
        private var boundPackage: String? = null

        init {
            val radius = view.resources.getDimension(R.dimen.tile_radius)
            view.findViewById<View>(R.id.card).apply {
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(v: View, outline: Outline) {
                        outline.setRoundRect(0, 0, v.width, v.height, radius)
                    }
                }
                clipToOutline = true
            }
            view.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) lastFocusedPackage = boundPackage
                v.animate().scaleX(if (hasFocus) FOCUS_SCALE else 1f)
                    .scaleY(if (hasFocus) FOCUS_SCALE else 1f)
                    .setDuration(120)
                    .start()
            }
        }

        fun bind(cell: Cell.App, isTopRow: Boolean) {
            val entry = cell.entry
            boundPackage = entry.packageName
            // Marks the top row. MainActivity.dispatchKeyEvent sends Up from these tiles to the gear.
            itemView.nextFocusUpId = if (isTopRow) R.id.settings else View.NO_ID
            label.text = entry.label
            val art = loaded?.artFor(entry.packageName)
            icon.setImageDrawable(art?.drawable)
            icon.scaleType = if (art?.isBanner == true) {
                ImageView.ScaleType.CENTER_CROP
            } else {
                ImageView.ScaleType.FIT_CENTER
            }
            val pad = if (art?.isBanner == true) 0 else (itemView.resources.displayMetrics.density * 16).toInt()
            icon.setPadding(pad, pad, pad, pad)
            itemView.setOnClickListener { onLaunch(entry) }
            itemView.setOnLongClickListener {
                onTogglePin(entry)
                true
            }
        }
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_APP = 1
        const val TYPE_EMPTY = 2
        const val FOCUS_SCALE = 1.12f
    }
}
