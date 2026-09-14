package com.geometry.rowplacement

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RowPlacementTest {
    @Test fun emptyRowKeepsPreferredPosition() {
        assertEquals(Placement("a", 30.0, 20.0), RowPlacement.find(listOf(Row("a", 100.0)), 20.0, preferredLeft = 30.0))
    }
    @Test fun noRowsReturnsNoSpace() { assertNull(RowPlacement.find(emptyList(), 10.0)) }
    @Test fun zeroWidthRowReturnsNoSpace() { assertNull(RowPlacement.find(listOf(Row("a", 0.0)), 1.0)) }
    @Test fun oversizeDoesNotGetClampedIntoAnOverlap() { assertNull(RowPlacement.find(listOf(Row("a", 10.0)), 11.0)) }
    @Test fun exactFitIncludesPadding() {
        assertEquals(5.0, RowPlacement.find(listOf(Row("a", 30.0)), 20.0, padding = 5.0)?.left)
    }
    @Test fun excessivePaddingReturnsNoSpace() { assertNull(RowPlacement.find(listOf(Row("a", 10.0)), 1.0, padding = 6.0)) }
    @Test fun preferenceIsClampedToEdges() {
        val rows = listOf(Row("a", 100.0))
        assertEquals(4.0, RowPlacement.find(rows, 20.0, preferredLeft = -50.0, padding = 4.0)?.left)
        assertEquals(76.0, RowPlacement.find(rows, 20.0, preferredLeft = 500.0, padding = 4.0)?.left)
    }
    @Test fun exactGapTouchingIsAllowed() {
        val rows = listOf(Row("a", 100.0, listOf(Occupied(0.0, 30.0), Occupied(60.0, 40.0))))
        assertEquals(35.0, RowPlacement.find(rows, 20.0, gap = 5.0)?.left)
        assertNull(RowPlacement.find(rows, 20.01, gap = 5.0))
    }
    @Test fun narrowGapIsNotSkippedBySampledSearch() {
        val rows = listOf(Row("a", 10000.0, listOf(Occupied(0.0, 123.125), Occupied(123.375, 9876.625))))
        assertEquals(123.125, RowPlacement.find(rows, .25, preferredLeft = 5000.0)?.left)
    }
    @Test fun nearestGapWinsAndTiesGoLeft() {
        val rows = listOf(Row("a", 100.0, listOf(Occupied(40.0, 20.0))))
        assertEquals(30.0, RowPlacement.find(rows, 10.0, preferredLeft = 45.0)?.left)
        assertEquals(60.0, RowPlacement.find(rows, 10.0, preferredLeft = 46.0)?.left)
    }
    @Test fun overlappingAndUnsortedOccupiedBoundsFormOneBlockedRegion() {
        val rows = listOf(Row("a", 100.0, listOf(Occupied(40.0, 20.0), Occupied(10.0, 50.0), Occupied(20.0, 10.0))))
        assertEquals(65.0, RowPlacement.find(rows, 20.0, gap = 5.0)?.left)
    }
    @Test fun preferredRowWinsEvenIfAnotherRowIsCloser() {
        assertEquals("b", RowPlacement.find(listOf(Row("a", 100.0), Row("b", 100.0)), 10.0, "b")?.rowId)
    }
    @Test fun fallsBackInCallerOrder() {
        val rows = listOf(Row("a", 100.0), Row("full", 10.0, listOf(Occupied(0.0, 10.0))), Row("b", 100.0))
        assertEquals("a", RowPlacement.find(rows, 10.0, "full")?.rowId)
    }
    @Test fun allRowsFullReturnsNoSpace() {
        assertNull(RowPlacement.find(listOf(Row("a", 10.0, listOf(Occupied(0.0, 10.0))), Row("b", 0.0)), 5.0))
    }
    @Test fun manyItemsFitWithoutAnArbitraryCountCap() {
        val occupied = mutableListOf<Occupied>()
        repeat(50) {
            val result = assertNotNull(RowPlacement.find(listOf(Row("a", 1000.0, occupied)), 10.0, preferredLeft = 500.0, gap = 2.0))
            assertTrue(occupied.all { o -> result.right + 2.0 <= o.left || o.right + 2.0 <= result.left })
            occupied.add(Occupied(result.left, result.width))
        }
    }
    @Test fun largerRenderedWidthCanForceFallback() {
        val rows = listOf(Row("a", 50.0), Row("b", 100.0))
        assertEquals("a", RowPlacement.find(rows, 40.0)?.rowId)
        assertEquals("b", RowPlacement.find(rows, 40.0 * 2.0)?.rowId)
    }
    @Test fun uniformUnitScalingPreservesPlacement() {
        val result = assertNotNull(RowPlacement.find(listOf(Row("a", 100.0, listOf(Occupied(0.0, 30.0)))), 20.0, gap = 2.0))
        val scaled = assertNotNull(RowPlacement.find(listOf(Row("a", 400.0, listOf(Occupied(0.0, 120.0)))), 80.0, gap = 8.0))
        assertEquals(result.left * 4, scaled.left)
    }
    @Test fun inputsAreNotMutated() {
        val items = mutableListOf(Occupied(70.0, 10.0), Occupied(10.0, 10.0))
        val before = items.toList()
        RowPlacement.find(listOf(Row("a", 100.0, items)), 10.0)
        assertEquals(before, items)
    }
    @Test fun rejectsInvalidNumbersAndIdentifiers() {
        for (bad in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -1.0, 0.0)) {
            assertFailsWith<IllegalArgumentException> { RowPlacement.find(emptyList(), bad) }
        }
        for (bad in listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0)) {
            assertFailsWith<IllegalArgumentException> { Row("a", bad) }
            assertFailsWith<IllegalArgumentException> { RowPlacement.find(emptyList(), 1.0, gap = bad) }
            assertFailsWith<IllegalArgumentException> { RowPlacement.find(emptyList(), 1.0, padding = bad) }
        }
        assertFailsWith<IllegalArgumentException> { RowPlacement.find(emptyList(), 1.0, preferredLeft = Double.NaN) }
        assertFailsWith<IllegalArgumentException> { RowPlacement.find(listOf(Row("a", 1.0), Row("a", 1.0)), 1.0) }
        assertFailsWith<IllegalArgumentException> { RowPlacement.find(emptyList(), 1.0, "missing") }
        assertFailsWith<IllegalArgumentException> { Row("", 1.0) }
        assertFailsWith<IllegalArgumentException> { Row("a", 1.0, listOf(Occupied(0.0, 2.0))) }
        assertFailsWith<IllegalArgumentException> { Occupied(Double.MAX_VALUE, Double.MAX_VALUE) }
    }
    @Test fun integerLayoutsMatchIndependentExhaustiveOracle() {
        val random = Random(71245)
        repeat(1000) {
            val rowWidth = random.nextInt(1, 150)
            val width = random.nextInt(1, 30)
            val gap = random.nextInt(0, 6)
            val padding = random.nextInt(0, 8)
            val preferred = random.nextInt(-20, 170)
            val occupied = List(random.nextInt(0, 15)) {
                val left = random.nextInt(rowWidth)
                Occupied(left.toDouble(), random.nextInt(1, rowWidth - left + 1).toDouble())
            }
            val expected = (padding..(rowWidth - padding - width)).filter { x ->
                occupied.all { x + width + gap <= it.left || it.right + gap <= x }
            }.minWithOrNull(compareBy<Int> { abs(it - preferred) }.thenBy { it })
            val actual = RowPlacement.find(listOf(Row("a", rowWidth.toDouble(), occupied)), width.toDouble(),
                preferredLeft = preferred.toDouble(), gap = gap.toDouble(), padding = padding.toDouble())
            assertEquals(expected?.toDouble(), actual?.left, "case $it")
        }
    }
}
