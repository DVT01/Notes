package org.dvt01.notes.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Note(var name: String, var text: String) {

    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    val fileName: String
        get() = "$name.txt"
}