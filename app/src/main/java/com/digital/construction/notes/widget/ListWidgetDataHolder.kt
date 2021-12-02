package com.digital.construction.notes.widget

import com.digital.construction.notes.model.Note

object ListWidgetDataHolder {
    /**
     * The key is the id of the widget
     * The value is the note for that widget
     */
    val noteWidgets = mutableMapOf<Int, Note>()
}