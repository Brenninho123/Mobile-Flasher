package com.mobileflasher.app.ui

import com.mobileflasher.app.i18n.StringKey
import com.mobileflasher.app.model.Tool

fun Tool.labelKey(): StringKey = when (this) {
    Tool.SELECT -> StringKey.ToolSelect
    Tool.ERASER -> StringKey.ToolEraser
    Tool.EYEDROPPER -> StringKey.ToolEyedropper
    Tool.RECTANGLE -> StringKey.ToolRectangle
    Tool.ELLIPSE -> StringKey.ToolEllipse
    Tool.LINE -> StringKey.ToolLine
    Tool.PEN -> StringKey.ToolPencil
}
