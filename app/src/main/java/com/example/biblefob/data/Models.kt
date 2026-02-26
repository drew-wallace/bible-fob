package com.example.biblefob.data

data class Book(
    val id: Int,
    val name: String
)

data class Chapter(
    val book: Book,
    val number: Int
)

data class Verse(
    val book: Book,
    val chapter: Int,
    val number: Int,
    val text: String
)

data class PassageRange(
    val startBook: Book,
    val startChapter: Int,
    val startVerse: Int,
    val endBook: Book,
    val endChapter: Int,
    val endVerse: Int
)
