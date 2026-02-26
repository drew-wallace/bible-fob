# Bible data file locations

Bible data is expected under Android assets (`app/src/main/assets/`).

## Default (non-versioned) paths

- Whole Bible JSON: `bible/whole_bible.json`
- Per-book JSON directory: `bible/books/`
  - Example: `bible/books/john.json`, `bible/books/1_corinthians.json`
- Prebuilt SQLite DB: `database/bible.db`
- SQL schema/reference file: `database/schema.sql`

## Multi-version paths (NASB, NET, ...)

Use the versioned helpers in `DataSourcePaths` and `AssetBibleRepositoryFactory.createForVersion(...)`.

For version `NET`:

- Whole JSON: `bible/NET_bible.json`
- Per-book folder: `bible/NET_books/`
  - Canonical naming mode example files: `bible/NET_books/1 Peter.json`, `bible/NET_books/Acts.json`
- Prebuilt SQLite DB (if you have one): `database/NET_bible.db`
- SQL dump (if your source is a `.sql` file): `database/NET_bible.sql`

Use `AssetBackedSQLiteOpenHelper.forVersion(...)` for prebuilt `.db` assets and
`AssetSqlDumpSQLiteOpenHelper.forVersion(...)` for `.sql` dump imports.

These defaults are defined in `DataSourcePaths`.
