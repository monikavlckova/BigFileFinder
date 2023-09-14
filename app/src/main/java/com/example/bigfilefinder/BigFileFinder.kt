package com.example.bigfilefinder

import java.util.PriorityQueue

class BigFileFinder(var count: Int) {

    private var dirs = mutableListOf<MyDirectory>()
    var biggestFilesPQ = PriorityQueue<MyFile>()

    fun increaseCount() {
        count++
    }

    fun decreaseCount() {
        if (count > 0) count--
    }

    fun addDirectories(list: MutableList<String>) {
        dirs = mutableListOf()
        for (i in 0 until list.size) {
            addDirectory(list[i])
        }
        biggestFilesPQ.clear()
    }

    private fun addDirectory(path: String) {
        val directory = MyDirectory(path)
        directory.addFiles()
        dirs.add(directory)
    }

    fun addToPQ(file: MyFile) {
        biggestFilesPQ.offer(file)
        if (biggestFilesPQ.size > count) biggestFilesPQ.poll()
    }

    fun findBigFiles(): List<MyFile> {
        dirs.forEach { dir -> dir.files.forEach { addToPQ(it) } }
        val solution = mutableListOf<MyFile>()
        while (!biggestFilesPQ.isEmpty()) {
            solution.add(biggestFilesPQ.poll())
        }
        return solution.reversed()
    }

}