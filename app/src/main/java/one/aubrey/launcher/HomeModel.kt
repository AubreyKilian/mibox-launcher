package one.aubrey.launcher

/** One launchable app. Pure data, so the layout logic is unit-testable. */
data class AppEntry(
    val packageName: String,
    val label: String,
    val activityName: String,
)

/** One cell of the home grid. */
sealed interface Cell {
    /** Full-width section title. */
    data class Header(val title: String) : Cell

    /** An app tile. [pinned] is true for the favourites row. */
    data class App(val entry: AppEntry, val pinned: Boolean) : Cell

    /** An unused favourite slot. [showHint] is true for the first one only. */
    data class EmptySlot(val showHint: Boolean) : Cell
}

object HomeModel {
    const val COLUMNS = 5
    const val MAX_FAVOURITES = 5

    /**
     * Builds the grid: a favourites header, [MAX_FAVOURITES] slots, an apps header,
     * then every unpinned app sorted by label.
     * Pins for apps that are no longer installed are ignored.
     */
    fun build(apps: List<AppEntry>, pins: List<String>): List<Cell> {
        val byPackage = apps.associateBy { it.packageName }
        val pinned = pins.distinct().mapNotNull { byPackage[it] }.take(MAX_FAVOURITES)
        val pinnedPackages = pinned.map { it.packageName }.toSet()
        val rest = apps
            .filter { it.packageName !in pinnedPackages }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })

        val cells = mutableListOf<Cell>(Cell.Header("Favourites"))
        pinned.forEach { cells += Cell.App(it, pinned = true) }
        repeat(MAX_FAVOURITES - pinned.size) { index ->
            cells += Cell.EmptySlot(showHint = index == 0)
        }
        cells += Cell.Header("Apps")
        rest.forEach { cells += Cell.App(it, pinned = false) }
        return cells
    }

    /**
     * Positions of the tiles in the top row of focusable tiles: the pinned tiles when there are
     * any, else the first [COLUMNS] app tiles. Pressing Up on one of these moves focus to the gear.
     */
    fun topRowPositions(cells: List<Cell>): Set<Int> {
        val apps = cells.withIndex().filter { it.value is Cell.App }
        val pinned = apps.filter { (it.value as Cell.App).pinned }
        val topRow = if (pinned.isNotEmpty()) pinned else apps.take(COLUMNS)
        return topRow.map { it.index }.toSet()
    }

    /** Pins [packageName], or unpins it when already pinned. Returns [pins] unchanged when full. */
    fun togglePin(pins: List<String>, packageName: String): List<String> = when {
        packageName in pins -> pins.filterNot { it == packageName }
        pins.size >= MAX_FAVOURITES -> pins
        else -> pins + packageName
    }
}
