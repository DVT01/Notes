package org.dvt01.notes.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Note(
    @PrimaryKey var name: String,
    var text: String
) {
    val fileName: String
        get() = "${name}.txt"
}