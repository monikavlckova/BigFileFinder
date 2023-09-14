package com.example.bigfilefinder

import java.nio.file.Files
import java.nio.file.Paths

class MyDirectory(val path: String) {
    var files = mutableListOf<MyFile>()

    fun addFiles() {
        Files.walk(Paths.get(path))
            .filter { Files.isRegularFile(it) }
            .forEach {
                files.add(MyFile(it.toString()))
            }
    }
}