package com.example.`data`.local

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.example.`data`.model.IndexedFile
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class FileDao_Impl(
  __db: RoomDatabase,
) : FileDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfIndexedFile: EntityInsertAdapter<IndexedFile>

  private val __deleteAdapterOfIndexedFile: EntityDeleteOrUpdateAdapter<IndexedFile>

  private val __updateAdapterOfIndexedFile: EntityDeleteOrUpdateAdapter<IndexedFile>
  init {
    this.__db = __db
    this.__insertAdapterOfIndexedFile = object : EntityInsertAdapter<IndexedFile>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `indexed_files` (`id`,`name`,`path`,`mimeType`,`size`,`timestamp`,`isVault`,`classification`,`hash`,`contentSummary`,`summaryKeywords`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: IndexedFile) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.path)
        statement.bindText(4, entity.mimeType)
        statement.bindLong(5, entity.size)
        statement.bindLong(6, entity.timestamp)
        val _tmp: Int = if (entity.isVault) 1 else 0
        statement.bindLong(7, _tmp.toLong())
        statement.bindText(8, entity.classification)
        statement.bindText(9, entity.hash)
        val _tmpContentSummary: String? = entity.contentSummary
        if (_tmpContentSummary == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpContentSummary)
        }
        val _tmpSummaryKeywords: String? = entity.summaryKeywords
        if (_tmpSummaryKeywords == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpSummaryKeywords)
        }
      }
    }
    this.__deleteAdapterOfIndexedFile = object : EntityDeleteOrUpdateAdapter<IndexedFile>() {
      protected override fun createQuery(): String = "DELETE FROM `indexed_files` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: IndexedFile) {
        statement.bindLong(1, entity.id.toLong())
      }
    }
    this.__updateAdapterOfIndexedFile = object : EntityDeleteOrUpdateAdapter<IndexedFile>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `indexed_files` SET `id` = ?,`name` = ?,`path` = ?,`mimeType` = ?,`size` = ?,`timestamp` = ?,`isVault` = ?,`classification` = ?,`hash` = ?,`contentSummary` = ?,`summaryKeywords` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: IndexedFile) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.path)
        statement.bindText(4, entity.mimeType)
        statement.bindLong(5, entity.size)
        statement.bindLong(6, entity.timestamp)
        val _tmp: Int = if (entity.isVault) 1 else 0
        statement.bindLong(7, _tmp.toLong())
        statement.bindText(8, entity.classification)
        statement.bindText(9, entity.hash)
        val _tmpContentSummary: String? = entity.contentSummary
        if (_tmpContentSummary == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpContentSummary)
        }
        val _tmpSummaryKeywords: String? = entity.summaryKeywords
        if (_tmpSummaryKeywords == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpSummaryKeywords)
        }
        statement.bindLong(12, entity.id.toLong())
      }
    }
  }

  public override suspend fun insertFile(`file`: IndexedFile): Long = performSuspending(__db, false,
      true) { _connection ->
    val _result: Long = __insertAdapterOfIndexedFile.insertAndReturnId(_connection, file)
    _result
  }

  public override suspend fun insertFiles(files: List<IndexedFile>): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfIndexedFile.insert(_connection, files)
  }

  public override suspend fun deleteFile(`file`: IndexedFile): Unit = performSuspending(__db, false,
      true) { _connection ->
    __deleteAdapterOfIndexedFile.handle(_connection, file)
  }

  public override suspend fun updateFile(`file`: IndexedFile): Unit = performSuspending(__db, false,
      true) { _connection ->
    __updateAdapterOfIndexedFile.handle(_connection, file)
  }

  public override fun getAllFiles(): Flow<List<IndexedFile>> {
    val _sql: String = "SELECT * FROM indexed_files ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("indexed_files")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfPath: Int = getColumnIndexOrThrow(_stmt, "path")
        val _columnIndexOfMimeType: Int = getColumnIndexOrThrow(_stmt, "mimeType")
        val _columnIndexOfSize: Int = getColumnIndexOrThrow(_stmt, "size")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfIsVault: Int = getColumnIndexOrThrow(_stmt, "isVault")
        val _columnIndexOfClassification: Int = getColumnIndexOrThrow(_stmt, "classification")
        val _columnIndexOfHash: Int = getColumnIndexOrThrow(_stmt, "hash")
        val _columnIndexOfContentSummary: Int = getColumnIndexOrThrow(_stmt, "contentSummary")
        val _columnIndexOfSummaryKeywords: Int = getColumnIndexOrThrow(_stmt, "summaryKeywords")
        val _result: MutableList<IndexedFile> = mutableListOf()
        while (_stmt.step()) {
          val _item: IndexedFile
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpPath: String
          _tmpPath = _stmt.getText(_columnIndexOfPath)
          val _tmpMimeType: String
          _tmpMimeType = _stmt.getText(_columnIndexOfMimeType)
          val _tmpSize: Long
          _tmpSize = _stmt.getLong(_columnIndexOfSize)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpIsVault: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsVault).toInt()
          _tmpIsVault = _tmp != 0
          val _tmpClassification: String
          _tmpClassification = _stmt.getText(_columnIndexOfClassification)
          val _tmpHash: String
          _tmpHash = _stmt.getText(_columnIndexOfHash)
          val _tmpContentSummary: String?
          if (_stmt.isNull(_columnIndexOfContentSummary)) {
            _tmpContentSummary = null
          } else {
            _tmpContentSummary = _stmt.getText(_columnIndexOfContentSummary)
          }
          val _tmpSummaryKeywords: String?
          if (_stmt.isNull(_columnIndexOfSummaryKeywords)) {
            _tmpSummaryKeywords = null
          } else {
            _tmpSummaryKeywords = _stmt.getText(_columnIndexOfSummaryKeywords)
          }
          _item =
              IndexedFile(_tmpId,_tmpName,_tmpPath,_tmpMimeType,_tmpSize,_tmpTimestamp,_tmpIsVault,_tmpClassification,_tmpHash,_tmpContentSummary,_tmpSummaryKeywords)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getNonVaultFiles(): Flow<List<IndexedFile>> {
    val _sql: String = "SELECT * FROM indexed_files WHERE isVault = 0 ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("indexed_files")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfPath: Int = getColumnIndexOrThrow(_stmt, "path")
        val _columnIndexOfMimeType: Int = getColumnIndexOrThrow(_stmt, "mimeType")
        val _columnIndexOfSize: Int = getColumnIndexOrThrow(_stmt, "size")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfIsVault: Int = getColumnIndexOrThrow(_stmt, "isVault")
        val _columnIndexOfClassification: Int = getColumnIndexOrThrow(_stmt, "classification")
        val _columnIndexOfHash: Int = getColumnIndexOrThrow(_stmt, "hash")
        val _columnIndexOfContentSummary: Int = getColumnIndexOrThrow(_stmt, "contentSummary")
        val _columnIndexOfSummaryKeywords: Int = getColumnIndexOrThrow(_stmt, "summaryKeywords")
        val _result: MutableList<IndexedFile> = mutableListOf()
        while (_stmt.step()) {
          val _item: IndexedFile
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpPath: String
          _tmpPath = _stmt.getText(_columnIndexOfPath)
          val _tmpMimeType: String
          _tmpMimeType = _stmt.getText(_columnIndexOfMimeType)
          val _tmpSize: Long
          _tmpSize = _stmt.getLong(_columnIndexOfSize)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpIsVault: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsVault).toInt()
          _tmpIsVault = _tmp != 0
          val _tmpClassification: String
          _tmpClassification = _stmt.getText(_columnIndexOfClassification)
          val _tmpHash: String
          _tmpHash = _stmt.getText(_columnIndexOfHash)
          val _tmpContentSummary: String?
          if (_stmt.isNull(_columnIndexOfContentSummary)) {
            _tmpContentSummary = null
          } else {
            _tmpContentSummary = _stmt.getText(_columnIndexOfContentSummary)
          }
          val _tmpSummaryKeywords: String?
          if (_stmt.isNull(_columnIndexOfSummaryKeywords)) {
            _tmpSummaryKeywords = null
          } else {
            _tmpSummaryKeywords = _stmt.getText(_columnIndexOfSummaryKeywords)
          }
          _item =
              IndexedFile(_tmpId,_tmpName,_tmpPath,_tmpMimeType,_tmpSize,_tmpTimestamp,_tmpIsVault,_tmpClassification,_tmpHash,_tmpContentSummary,_tmpSummaryKeywords)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getVaultFiles(): Flow<List<IndexedFile>> {
    val _sql: String = "SELECT * FROM indexed_files WHERE isVault = 1 ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("indexed_files")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfPath: Int = getColumnIndexOrThrow(_stmt, "path")
        val _columnIndexOfMimeType: Int = getColumnIndexOrThrow(_stmt, "mimeType")
        val _columnIndexOfSize: Int = getColumnIndexOrThrow(_stmt, "size")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfIsVault: Int = getColumnIndexOrThrow(_stmt, "isVault")
        val _columnIndexOfClassification: Int = getColumnIndexOrThrow(_stmt, "classification")
        val _columnIndexOfHash: Int = getColumnIndexOrThrow(_stmt, "hash")
        val _columnIndexOfContentSummary: Int = getColumnIndexOrThrow(_stmt, "contentSummary")
        val _columnIndexOfSummaryKeywords: Int = getColumnIndexOrThrow(_stmt, "summaryKeywords")
        val _result: MutableList<IndexedFile> = mutableListOf()
        while (_stmt.step()) {
          val _item: IndexedFile
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpPath: String
          _tmpPath = _stmt.getText(_columnIndexOfPath)
          val _tmpMimeType: String
          _tmpMimeType = _stmt.getText(_columnIndexOfMimeType)
          val _tmpSize: Long
          _tmpSize = _stmt.getLong(_columnIndexOfSize)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpIsVault: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsVault).toInt()
          _tmpIsVault = _tmp != 0
          val _tmpClassification: String
          _tmpClassification = _stmt.getText(_columnIndexOfClassification)
          val _tmpHash: String
          _tmpHash = _stmt.getText(_columnIndexOfHash)
          val _tmpContentSummary: String?
          if (_stmt.isNull(_columnIndexOfContentSummary)) {
            _tmpContentSummary = null
          } else {
            _tmpContentSummary = _stmt.getText(_columnIndexOfContentSummary)
          }
          val _tmpSummaryKeywords: String?
          if (_stmt.isNull(_columnIndexOfSummaryKeywords)) {
            _tmpSummaryKeywords = null
          } else {
            _tmpSummaryKeywords = _stmt.getText(_columnIndexOfSummaryKeywords)
          }
          _item =
              IndexedFile(_tmpId,_tmpName,_tmpPath,_tmpMimeType,_tmpSize,_tmpTimestamp,_tmpIsVault,_tmpClassification,_tmpHash,_tmpContentSummary,_tmpSummaryKeywords)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getFileByPath(path: String): IndexedFile? {
    val _sql: String = "SELECT * FROM indexed_files WHERE path = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, path)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfPath: Int = getColumnIndexOrThrow(_stmt, "path")
        val _columnIndexOfMimeType: Int = getColumnIndexOrThrow(_stmt, "mimeType")
        val _columnIndexOfSize: Int = getColumnIndexOrThrow(_stmt, "size")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfIsVault: Int = getColumnIndexOrThrow(_stmt, "isVault")
        val _columnIndexOfClassification: Int = getColumnIndexOrThrow(_stmt, "classification")
        val _columnIndexOfHash: Int = getColumnIndexOrThrow(_stmt, "hash")
        val _columnIndexOfContentSummary: Int = getColumnIndexOrThrow(_stmt, "contentSummary")
        val _columnIndexOfSummaryKeywords: Int = getColumnIndexOrThrow(_stmt, "summaryKeywords")
        val _result: IndexedFile?
        if (_stmt.step()) {
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpPath: String
          _tmpPath = _stmt.getText(_columnIndexOfPath)
          val _tmpMimeType: String
          _tmpMimeType = _stmt.getText(_columnIndexOfMimeType)
          val _tmpSize: Long
          _tmpSize = _stmt.getLong(_columnIndexOfSize)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpIsVault: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsVault).toInt()
          _tmpIsVault = _tmp != 0
          val _tmpClassification: String
          _tmpClassification = _stmt.getText(_columnIndexOfClassification)
          val _tmpHash: String
          _tmpHash = _stmt.getText(_columnIndexOfHash)
          val _tmpContentSummary: String?
          if (_stmt.isNull(_columnIndexOfContentSummary)) {
            _tmpContentSummary = null
          } else {
            _tmpContentSummary = _stmt.getText(_columnIndexOfContentSummary)
          }
          val _tmpSummaryKeywords: String?
          if (_stmt.isNull(_columnIndexOfSummaryKeywords)) {
            _tmpSummaryKeywords = null
          } else {
            _tmpSummaryKeywords = _stmt.getText(_columnIndexOfSummaryKeywords)
          }
          _result =
              IndexedFile(_tmpId,_tmpName,_tmpPath,_tmpMimeType,_tmpSize,_tmpTimestamp,_tmpIsVault,_tmpClassification,_tmpHash,_tmpContentSummary,_tmpSummaryKeywords)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteFileById(id: Int) {
    val _sql: String = "DELETE FROM indexed_files WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id.toLong())
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearDatabase() {
    val _sql: String = "DELETE FROM indexed_files"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
