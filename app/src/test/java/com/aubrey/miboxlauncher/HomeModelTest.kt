package com.aubrey.miboxlauncher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeModelTest {
    private fun app(pkg: String, label: String = pkg) = AppEntry(pkg, label, "$pkg.Main")

    @Test
    fun emptyPinsGiveFiveEmptySlotsAndSortedApps() {
        val cells = HomeModel.build(listOf(app("b", "Banana"), app("a", "apple")), emptyList())

        assertEquals(Cell.Header("Favourites"), cells[0])
        assertEquals(Cell.EmptySlot(showHint = true), cells[1])
        (2..5).forEach { assertEquals(Cell.EmptySlot(showHint = false), cells[it]) }
        assertEquals(Cell.Header("Apps"), cells[6])
        assertEquals("apple", (cells[7] as Cell.App).entry.label)
        assertEquals("Banana", (cells[8] as Cell.App).entry.label)
        assertEquals(9, cells.size)
    }

    @Test
    fun pinnedAppsComeFirstInPinOrderAndLeaveTheGrid() {
        val apps = listOf(app("a"), app("b"), app("c"))
        val cells = HomeModel.build(apps, listOf("c", "a"))

        assertEquals(Cell.App(apps[2], pinned = true), cells[1])
        assertEquals(Cell.App(apps[0], pinned = true), cells[2])
        assertEquals(Cell.EmptySlot(showHint = true), cells[3])
        val grid = cells.drop(cells.indexOf(Cell.Header("Apps")) + 1)
        assertEquals(listOf(Cell.App(apps[1], pinned = false)), grid)
    }

    @Test
    fun pinsForUninstalledAppsAreIgnored() {
        val cells = HomeModel.build(listOf(app("a")), listOf("gone", "a"))

        assertEquals(Cell.App(app("a"), pinned = true), cells[1])
        assertTrue(cells[2] is Cell.EmptySlot)
    }

    @Test
    fun duplicatePinsCountOnce() {
        val cells = HomeModel.build(listOf(app("a")), listOf("a", "a"))

        assertEquals(1, cells.count { it is Cell.App && it.pinned })
    }

    @Test
    fun togglePinAddsRemovesAndStopsAtFive() {
        assertEquals(listOf("a"), HomeModel.togglePin(emptyList(), "a"))
        assertEquals(emptyList<String>(), HomeModel.togglePin(listOf("a"), "a"))

        val full = listOf("a", "b", "c", "d", "e")
        assertEquals(full, HomeModel.togglePin(full, "f"))
        assertEquals(listOf("a", "b", "d", "e"), HomeModel.togglePin(full, "c"))
    }
}
