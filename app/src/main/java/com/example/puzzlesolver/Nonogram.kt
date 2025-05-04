package com.example.puzzlesolver

import android.os.Parcel
import android.os.Parcelable

data class Nonogram(
    val grid: Array<IntArray>,
    val title: String = "Generated Nonogram"
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.createIntArray()?.let {
            val size = Math.sqrt(it.size.toDouble()).toInt()
            Array(size) { i -> IntArray(size) { j -> it[i * size + j] } }
        } ?: emptyArray(),
        parcel.readString() ?: "Generated Nonogram"
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeIntArray(grid.flatMap { it.toList() }.toIntArray())
        parcel.writeString(title)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Nonogram> {
        override fun createFromParcel(parcel: Parcel): Nonogram {
            return Nonogram(parcel)
        }

        override fun newArray(size: Int): Array<Nonogram?> {
            return arrayOfNulls(size)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Nonogram

        if (!grid.contentDeepEquals(other.grid)) return false
        if (title != other.title) return false

        return true
    }

    override fun hashCode(): Int {
        var result = grid.contentDeepHashCode()
        result = 31 * result + title.hashCode()
        return result
    }
}