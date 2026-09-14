import com.geometry.rowplacement.Occupied
import com.geometry.rowplacement.Row
import com.geometry.rowplacement.RowPlacement

fun main() {
    val rows = listOf(
        Row("top", 300.0, listOf(Occupied(8.0, 284.0))),
        Row("bottom", 300.0, listOf(Occupied(8.0, 40.0))),
    )
    val placement = RowPlacement.find(
        rows, itemWidth = 60.0, preferredRowId = "top",
        preferredLeft = 24.0, gap = 8.0, padding = 8.0,
    )
    check(placement?.rowId == "bottom" && placement.left == 56.0)
    println(placement)
    // Render at placement.left. Save it in your own state before adding another item.
}
