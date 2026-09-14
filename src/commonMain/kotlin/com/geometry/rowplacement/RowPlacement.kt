package com.geometry.rowplacement

import kotlin.math.abs

/** Horizontal bounds in caller-selected units. Use the same unit for every input. */
data class Occupied(val left: Double, val width: Double) {
    init {
        require(left.isFinite() && left >= 0.0) { "left must be finite and nonnegative" }
        require(width.isFinite() && width > 0.0) { "width must be finite and positive" }
        require((left + width).isFinite()) { "item end must be finite" }
    }
    val right: Double get() = left + width
}

/** Existing bounds may overlap; the search treats them as occupied, not as an error. */
data class Row(
    val id: String,
    val width: Double,
    val occupied: List<Occupied> = emptyList(),
) {
    init {
        require(id.isNotBlank()) { "row id must not be blank" }
        require(width.isFinite() && width >= 0.0) { "row width must be finite and nonnegative" }
        require(occupied.all { it.right <= width }) { "occupied bounds must fit inside the row" }
    }
}

data class Placement(val rowId: String, val left: Double, val width: Double) {
    val right: Double get() = left + width
}

/** Pure one-dimensional placement; does not move existing items or persist any state. */
object RowPlacement {
    /**
     * Tries the preferred row first, then remaining rows in caller order.
     * Within a row, chooses the legal left edge nearest [preferredLeft]. Ties go left.
     * Width is the final rendered width, including any scale applied by the caller.
     * Returns null when no row fits. Invalid inputs throw IllegalArgumentException.
     * O(sum(n log n)) time and O(max(n)) temporary space for n occupied bounds per row.
     */
    fun find(
        rows: List<Row>,
        itemWidth: Double,
        preferredRowId: String? = null,
        preferredLeft: Double = 0.0,
        gap: Double = 0.0,
        padding: Double = 0.0,
    ): Placement? {
        require(itemWidth.isFinite() && itemWidth > 0.0) { "itemWidth must be finite and positive" }
        require(preferredLeft.isFinite()) { "preferredLeft must be finite" }
        require(gap.isFinite() && gap >= 0.0) { "gap must be finite and nonnegative" }
        require(padding.isFinite() && padding >= 0.0) { "padding must be finite and nonnegative" }
        require(rows.map { it.id }.distinct().size == rows.size) { "row ids must be unique" }
        require(preferredRowId == null || rows.any { it.id == preferredRowId }) { "preferred row must exist" }
        val ordered = if (preferredRowId == null) rows else
            rows.filter { it.id == preferredRowId } + rows.filter { it.id != preferredRowId }
        for (row in ordered) {
            // Revalidate in case a caller supplied a mutable list and changed it after construction.
            require(row.occupied.all { it.right <= row.width }) { "occupied bounds must fit inside the row" }
            val limit = row.width - padding
            var cursor = padding
            var best: Double? = null
            fun consider(end: Double) {
                val maxLeft = end - itemWidth
                if (maxLeft < cursor) return
                val candidate = preferredLeft.coerceIn(cursor, maxLeft)
                // Floating-point subtraction can round up. Never return an overflowing bound.
                if (candidate + itemWidth > end) return
                val previous = best
                if (previous == null || abs(candidate - preferredLeft) < abs(previous - preferredLeft) ||
                    (abs(candidate - preferredLeft) == abs(previous - preferredLeft) && candidate < previous)) {
                    best = candidate
                }
            }
            for (item in row.occupied.sortedBy { it.left }) {
                consider(minOf(limit, item.left - gap))
                cursor = maxOf(cursor, item.right + gap)
                if (cursor > limit) break
            }
            consider(limit)
            if (best != null) return Placement(row.id, best!!, itemWidth)
        }
        return null
    }
}
