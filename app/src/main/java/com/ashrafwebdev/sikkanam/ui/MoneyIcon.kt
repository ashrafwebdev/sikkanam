package com.ashrafwebdev.sikkanam.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * A banknote for the Money tab, drawn in code because the core icon set has no money icon.
 * The diamond in the middle is cut out of the note.
 */
val MoneyIcon: ImageVector = ImageVector.Builder(
    name = "Money",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
    moveTo(2f, 6f)
    lineTo(22f, 6f)
    lineTo(22f, 18f)
    lineTo(2f, 18f)
    close()
    moveTo(12f, 9f)
    lineTo(15f, 12f)
    lineTo(12f, 15f)
    lineTo(9f, 12f)
    close()
}.build()
