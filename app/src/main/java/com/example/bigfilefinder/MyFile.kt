package com.example.bigfilefinder

import java.io.File

class MyFile(val path: String) : Comparable<MyFile> {
    val size : Int = (File(path).length() / 1024).toInt()

    override fun compareTo(other: MyFile): Int {
        return this.size - other.size
    }
}