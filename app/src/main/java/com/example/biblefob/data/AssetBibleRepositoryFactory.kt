package com.example.biblefob.data

import android.content.Context

object AssetBibleRepositoryFactory {
    fun create(
        context: Context,
        wholeBibleAssetPath: String? = DataSourcePaths.WHOLE_BIBLE_JSON,
        perBookAssetFolder: String = DataSourcePaths.PER_BOOK_JSON_DIR,
        bookFileNameStyle: BookFileNameStyle = BookFileNameStyle.SLUG
    ): BibleRepository {
        val wholeBibleJson = wholeBibleAssetPath
            ?.let { path -> runCatching { context.assets.open(path).bufferedReader().use { it.readText() } }.getOrNull() }

        val perBookLoader: (Book) -> String? = { book ->
            val filename = resolveBookFileName(book.name, bookFileNameStyle)
            val path = "$perBookAssetFolder/$filename.json"
            runCatching { context.assets.open(path).bufferedReader().use { it.readText() } }.getOrNull()
        }

        return JsonBibleRepository(
            wholeBibleJson = wholeBibleJson,
            perBookJsonLoader = perBookLoader
        )
    }

    fun createForVersion(
        context: Context,
        version: String,
        preferWholeBibleFile: Boolean = true,
        bookFileNameStyle: BookFileNameStyle = BookFileNameStyle.CANONICAL
    ): BibleRepository {
        val wholeBiblePath = if (preferWholeBibleFile) DataSourcePaths.wholeBibleJson(version) else null
        return create(
            context = context,
            wholeBibleAssetPath = wholeBiblePath,
            perBookAssetFolder = DataSourcePaths.perBookJsonDir(version),
            bookFileNameStyle = bookFileNameStyle
        )
    }
}

enum class BookFileNameStyle {
    CANONICAL,
    SLUG
}

internal fun resolveBookFileName(bookName: String, style: BookFileNameStyle): String {
    return when (style) {
        BookFileNameStyle.CANONICAL -> bookName
        BookFileNameStyle.SLUG -> bookName.lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]+"), "")
    }
}
