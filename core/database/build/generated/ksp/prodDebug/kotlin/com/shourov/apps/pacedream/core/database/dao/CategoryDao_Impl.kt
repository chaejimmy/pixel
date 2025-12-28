package com.shourov.apps.pacedream.core.database.dao

import android.database.Cursor
import android.os.CancellationSignal
import androidx.room.CoroutinesRoom
import androidx.room.CoroutinesRoom.Companion.execute
import androidx.room.EntityDeletionOrUpdateAdapter
import androidx.room.EntityInsertionAdapter
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.SharedSQLiteStatement
import androidx.room.util.createCancellationSignal
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.query
import androidx.sqlite.db.SupportSQLiteStatement
import com.shourov.apps.pacedream.core.database.entity.CategoryEntity
import java.lang.Class
import java.util.ArrayList
import java.util.concurrent.Callable
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.jvm.JvmStatic
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION"])
public class CategoryDao_Impl(
  __db: RoomDatabase,
) : CategoryDao {
  private val __db: RoomDatabase

  private val __insertionAdapterOfCategoryEntity: EntityInsertionAdapter<CategoryEntity>

  private val __deletionAdapterOfCategoryEntity: EntityDeletionOrUpdateAdapter<CategoryEntity>

  private val __updateAdapterOfCategoryEntity: EntityDeletionOrUpdateAdapter<CategoryEntity>

  private val __preparedStmtOfDeleteAllCategories: SharedSQLiteStatement
  init {
    this.__db = __db
    this.__insertionAdapterOfCategoryEntity = object : EntityInsertionAdapter<CategoryEntity>(__db)
        {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `categories` (`id`,`name`,`icon`,`color`,`isActive`,`sortOrder`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: CategoryEntity) {
        statement.bindString(1, entity.id)
        statement.bindString(2, entity.name)
        statement.bindString(3, entity.icon)
        statement.bindString(4, entity.color)
        val _tmp: Int = if (entity.isActive) 1 else 0
        statement.bindLong(5, _tmp.toLong())
        statement.bindLong(6, entity.sortOrder.toLong())
        statement.bindString(7, entity.createdAt)
        statement.bindString(8, entity.updatedAt)
      }
    }
    this.__deletionAdapterOfCategoryEntity = object :
        EntityDeletionOrUpdateAdapter<CategoryEntity>(__db) {
      protected override fun createQuery(): String = "DELETE FROM `categories` WHERE `id` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: CategoryEntity) {
        statement.bindString(1, entity.id)
      }
    }
    this.__updateAdapterOfCategoryEntity = object :
        EntityDeletionOrUpdateAdapter<CategoryEntity>(__db) {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `categories` SET `id` = ?,`name` = ?,`icon` = ?,`color` = ?,`isActive` = ?,`sortOrder` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: CategoryEntity) {
        statement.bindString(1, entity.id)
        statement.bindString(2, entity.name)
        statement.bindString(3, entity.icon)
        statement.bindString(4, entity.color)
        val _tmp: Int = if (entity.isActive) 1 else 0
        statement.bindLong(5, _tmp.toLong())
        statement.bindLong(6, entity.sortOrder.toLong())
        statement.bindString(7, entity.createdAt)
        statement.bindString(8, entity.updatedAt)
        statement.bindString(9, entity.id)
      }
    }
    this.__preparedStmtOfDeleteAllCategories = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "DELETE FROM categories"
        return _query
      }
    }
  }

  public override suspend fun insertCategory(category: CategoryEntity): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __insertionAdapterOfCategoryEntity.insert(category)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun insertCategories(categories: List<CategoryEntity>): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __insertionAdapterOfCategoryEntity.insert(categories)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun deleteCategory(category: CategoryEntity): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __deletionAdapterOfCategoryEntity.handle(category)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun updateCategory(category: CategoryEntity): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __updateAdapterOfCategoryEntity.handle(category)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun deleteAllCategories(): Unit = CoroutinesRoom.execute(__db, true,
      object : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfDeleteAllCategories.acquire()
      try {
        __db.beginTransaction()
        try {
          _stmt.executeUpdateDelete()
          __db.setTransactionSuccessful()
        } finally {
          __db.endTransaction()
        }
      } finally {
        __preparedStmtOfDeleteAllCategories.release(_stmt)
      }
    }
  })

  public override fun getAllCategories(): Flow<List<CategoryEntity>> {
    val _sql: String = "SELECT * FROM categories WHERE isActive = 1 ORDER BY sortOrder ASC"
    val _statement: RoomSQLiteQuery = acquire(_sql, 0)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("categories"), object :
        Callable<List<CategoryEntity>> {
      public override fun call(): List<CategoryEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfName: Int = getColumnIndexOrThrow(_cursor, "name")
          val _cursorIndexOfIcon: Int = getColumnIndexOrThrow(_cursor, "icon")
          val _cursorIndexOfColor: Int = getColumnIndexOrThrow(_cursor, "color")
          val _cursorIndexOfIsActive: Int = getColumnIndexOrThrow(_cursor, "isActive")
          val _cursorIndexOfSortOrder: Int = getColumnIndexOrThrow(_cursor, "sortOrder")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: MutableList<CategoryEntity> = ArrayList<CategoryEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: CategoryEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpName: String
            _tmpName = _cursor.getString(_cursorIndexOfName)
            val _tmpIcon: String
            _tmpIcon = _cursor.getString(_cursorIndexOfIcon)
            val _tmpColor: String
            _tmpColor = _cursor.getString(_cursorIndexOfColor)
            val _tmpIsActive: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsActive)
            _tmpIsActive = _tmp != 0
            val _tmpSortOrder: Int
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder)
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            _item =
                CategoryEntity(_tmpId,_tmpName,_tmpIcon,_tmpColor,_tmpIsActive,_tmpSortOrder,_tmpCreatedAt,_tmpUpdatedAt)
            _result.add(_item)
          }
          return _result
        } finally {
          _cursor.close()
        }
      }

      protected fun finalize() {
        _statement.release()
      }
    })
  }

  public override suspend fun getCategoryById(categoryId: String): CategoryEntity? {
    val _sql: String = "SELECT * FROM categories WHERE id = ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, categoryId)
    val _cancellationSignal: CancellationSignal? = createCancellationSignal()
    return execute(__db, false, _cancellationSignal, object : Callable<CategoryEntity?> {
      public override fun call(): CategoryEntity? {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfName: Int = getColumnIndexOrThrow(_cursor, "name")
          val _cursorIndexOfIcon: Int = getColumnIndexOrThrow(_cursor, "icon")
          val _cursorIndexOfColor: Int = getColumnIndexOrThrow(_cursor, "color")
          val _cursorIndexOfIsActive: Int = getColumnIndexOrThrow(_cursor, "isActive")
          val _cursorIndexOfSortOrder: Int = getColumnIndexOrThrow(_cursor, "sortOrder")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: CategoryEntity?
          if (_cursor.moveToFirst()) {
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpName: String
            _tmpName = _cursor.getString(_cursorIndexOfName)
            val _tmpIcon: String
            _tmpIcon = _cursor.getString(_cursorIndexOfIcon)
            val _tmpColor: String
            _tmpColor = _cursor.getString(_cursorIndexOfColor)
            val _tmpIsActive: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsActive)
            _tmpIsActive = _tmp != 0
            val _tmpSortOrder: Int
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder)
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            _result =
                CategoryEntity(_tmpId,_tmpName,_tmpIcon,_tmpColor,_tmpIsActive,_tmpSortOrder,_tmpCreatedAt,_tmpUpdatedAt)
          } else {
            _result = null
          }
          return _result
        } finally {
          _cursor.close()
          _statement.release()
        }
      }
    })
  }

  public companion object {
    @JvmStatic
    public fun getRequiredConverters(): List<Class<*>> = emptyList()
  }
}
