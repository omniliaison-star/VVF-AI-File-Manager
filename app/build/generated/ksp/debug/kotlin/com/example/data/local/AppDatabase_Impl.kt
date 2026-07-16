package com.example.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _fileDao: Lazy<FileDao> = lazy {
    FileDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(2,
        "eba2957270e172bb63654a6957b5867e", "9aa99b8179da434f5d751fd59b7c57aa") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `indexed_files` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `path` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `size` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `isVault` INTEGER NOT NULL, `classification` TEXT NOT NULL, `hash` TEXT NOT NULL, `contentSummary` TEXT, `summaryKeywords` TEXT)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'eba2957270e172bb63654a6957b5867e')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `indexed_files`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsIndexedFiles: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsIndexedFiles.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsIndexedFiles.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsIndexedFiles.put("path", TableInfo.Column("path", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsIndexedFiles.put("mimeType", TableInfo.Column("mimeType", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsIndexedFiles.put("size", TableInfo.Column("size", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsIndexedFiles.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsIndexedFiles.put("isVault", TableInfo.Column("isVault", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsIndexedFiles.put("classification", TableInfo.Column("classification", "TEXT", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsIndexedFiles.put("hash", TableInfo.Column("hash", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsIndexedFiles.put("contentSummary", TableInfo.Column("contentSummary", "TEXT", false,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsIndexedFiles.put("summaryKeywords", TableInfo.Column("summaryKeywords", "TEXT",
            false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysIndexedFiles: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesIndexedFiles: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoIndexedFiles: TableInfo = TableInfo("indexed_files", _columnsIndexedFiles,
            _foreignKeysIndexedFiles, _indicesIndexedFiles)
        val _existingIndexedFiles: TableInfo = read(connection, "indexed_files")
        if (!_infoIndexedFiles.equals(_existingIndexedFiles)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |indexed_files(com.example.data.model.IndexedFile).
              | Expected:
              |""".trimMargin() + _infoIndexedFiles + """
              |
              | Found:
              |""".trimMargin() + _existingIndexedFiles)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "indexed_files")
  }

  public override fun clearAllTables() {
    super.performClear(false, "indexed_files")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(FileDao::class, FileDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun fileDao(): FileDao = _fileDao.value
}
