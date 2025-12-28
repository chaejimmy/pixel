package com.shourov.apps.pacedream.core.database.dao

import android.database.Cursor
import androidx.room.CoroutinesRoom
import androidx.room.EntityDeletionOrUpdateAdapter
import androidx.room.EntityInsertionAdapter
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.SharedSQLiteStatement
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.query
import androidx.sqlite.db.SupportSQLiteStatement
import com.shourov.apps.pacedream.core.database.entity.UserEntity
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
public class UserDao_Impl(
  __db: RoomDatabase,
) : UserDao {
  private val __db: RoomDatabase

  private val __insertionAdapterOfUserEntity: EntityInsertionAdapter<UserEntity>

  private val __deletionAdapterOfUserEntity: EntityDeletionOrUpdateAdapter<UserEntity>

  private val __updateAdapterOfUserEntity: EntityDeletionOrUpdateAdapter<UserEntity>

  private val __preparedStmtOfDeleteUserById: SharedSQLiteStatement

  private val __preparedStmtOfDeleteAllUsers: SharedSQLiteStatement
  init {
    this.__db = __db
    this.__insertionAdapterOfUserEntity = object : EntityInsertionAdapter<UserEntity>(__db) {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `users` (`id`,`firstName`,`lastName`,`email`,`phone`,`profilePic`,`dateOfBirth`,`gender`,`isVerified`,`createdAt`,`updatedAt`,`preferences`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: UserEntity) {
        statement.bindString(1, entity.id)
        statement.bindString(2, entity.firstName)
        statement.bindString(3, entity.lastName)
        statement.bindString(4, entity.email)
        val _tmpPhone: String? = entity.phone
        if (_tmpPhone == null) {
          statement.bindNull(5)
        } else {
          statement.bindString(5, _tmpPhone)
        }
        val _tmpProfilePic: String? = entity.profilePic
        if (_tmpProfilePic == null) {
          statement.bindNull(6)
        } else {
          statement.bindString(6, _tmpProfilePic)
        }
        val _tmpDateOfBirth: String? = entity.dateOfBirth
        if (_tmpDateOfBirth == null) {
          statement.bindNull(7)
        } else {
          statement.bindString(7, _tmpDateOfBirth)
        }
        val _tmpGender: String? = entity.gender
        if (_tmpGender == null) {
          statement.bindNull(8)
        } else {
          statement.bindString(8, _tmpGender)
        }
        val _tmp: Int = if (entity.isVerified) 1 else 0
        statement.bindLong(9, _tmp.toLong())
        statement.bindString(10, entity.createdAt)
        statement.bindString(11, entity.updatedAt)
        val _tmpPreferences: String? = entity.preferences
        if (_tmpPreferences == null) {
          statement.bindNull(12)
        } else {
          statement.bindString(12, _tmpPreferences)
        }
      }
    }
    this.__deletionAdapterOfUserEntity = object : EntityDeletionOrUpdateAdapter<UserEntity>(__db) {
      protected override fun createQuery(): String = "DELETE FROM `users` WHERE `id` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: UserEntity) {
        statement.bindString(1, entity.id)
      }
    }
    this.__updateAdapterOfUserEntity = object : EntityDeletionOrUpdateAdapter<UserEntity>(__db) {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `users` SET `id` = ?,`firstName` = ?,`lastName` = ?,`email` = ?,`phone` = ?,`profilePic` = ?,`dateOfBirth` = ?,`gender` = ?,`isVerified` = ?,`createdAt` = ?,`updatedAt` = ?,`preferences` = ? WHERE `id` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: UserEntity) {
        statement.bindString(1, entity.id)
        statement.bindString(2, entity.firstName)
        statement.bindString(3, entity.lastName)
        statement.bindString(4, entity.email)
        val _tmpPhone: String? = entity.phone
        if (_tmpPhone == null) {
          statement.bindNull(5)
        } else {
          statement.bindString(5, _tmpPhone)
        }
        val _tmpProfilePic: String? = entity.profilePic
        if (_tmpProfilePic == null) {
          statement.bindNull(6)
        } else {
          statement.bindString(6, _tmpProfilePic)
        }
        val _tmpDateOfBirth: String? = entity.dateOfBirth
        if (_tmpDateOfBirth == null) {
          statement.bindNull(7)
        } else {
          statement.bindString(7, _tmpDateOfBirth)
        }
        val _tmpGender: String? = entity.gender
        if (_tmpGender == null) {
          statement.bindNull(8)
        } else {
          statement.bindString(8, _tmpGender)
        }
        val _tmp: Int = if (entity.isVerified) 1 else 0
        statement.bindLong(9, _tmp.toLong())
        statement.bindString(10, entity.createdAt)
        statement.bindString(11, entity.updatedAt)
        val _tmpPreferences: String? = entity.preferences
        if (_tmpPreferences == null) {
          statement.bindNull(12)
        } else {
          statement.bindString(12, _tmpPreferences)
        }
        statement.bindString(13, entity.id)
      }
    }
    this.__preparedStmtOfDeleteUserById = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "DELETE FROM users WHERE id = ?"
        return _query
      }
    }
    this.__preparedStmtOfDeleteAllUsers = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "DELETE FROM users"
        return _query
      }
    }
  }

  public override suspend fun insertUser(user: UserEntity): Unit = CoroutinesRoom.execute(__db,
      true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __insertionAdapterOfUserEntity.insert(user)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun insertUsers(users: List<UserEntity>): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __insertionAdapterOfUserEntity.insert(users)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun deleteUser(user: UserEntity): Unit = CoroutinesRoom.execute(__db,
      true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __deletionAdapterOfUserEntity.handle(user)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun updateUser(user: UserEntity): Unit = CoroutinesRoom.execute(__db,
      true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __updateAdapterOfUserEntity.handle(user)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun deleteUserById(userId: String): Unit = CoroutinesRoom.execute(__db,
      true, object : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfDeleteUserById.acquire()
      var _argIndex: Int = 1
      _stmt.bindString(_argIndex, userId)
      try {
        __db.beginTransaction()
        try {
          _stmt.executeUpdateDelete()
          __db.setTransactionSuccessful()
        } finally {
          __db.endTransaction()
        }
      } finally {
        __preparedStmtOfDeleteUserById.release(_stmt)
      }
    }
  })

  public override suspend fun deleteAllUsers(): Unit = CoroutinesRoom.execute(__db, true, object :
      Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfDeleteAllUsers.acquire()
      try {
        __db.beginTransaction()
        try {
          _stmt.executeUpdateDelete()
          __db.setTransactionSuccessful()
        } finally {
          __db.endTransaction()
        }
      } finally {
        __preparedStmtOfDeleteAllUsers.release(_stmt)
      }
    }
  })

  public override fun getUserById(userId: String): Flow<UserEntity?> {
    val _sql: String = "SELECT * FROM users WHERE id = ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, userId)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("users"), object : Callable<UserEntity?> {
      public override fun call(): UserEntity? {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfFirstName: Int = getColumnIndexOrThrow(_cursor, "firstName")
          val _cursorIndexOfLastName: Int = getColumnIndexOrThrow(_cursor, "lastName")
          val _cursorIndexOfEmail: Int = getColumnIndexOrThrow(_cursor, "email")
          val _cursorIndexOfPhone: Int = getColumnIndexOrThrow(_cursor, "phone")
          val _cursorIndexOfProfilePic: Int = getColumnIndexOrThrow(_cursor, "profilePic")
          val _cursorIndexOfDateOfBirth: Int = getColumnIndexOrThrow(_cursor, "dateOfBirth")
          val _cursorIndexOfGender: Int = getColumnIndexOrThrow(_cursor, "gender")
          val _cursorIndexOfIsVerified: Int = getColumnIndexOrThrow(_cursor, "isVerified")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _cursorIndexOfPreferences: Int = getColumnIndexOrThrow(_cursor, "preferences")
          val _result: UserEntity?
          if (_cursor.moveToFirst()) {
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpFirstName: String
            _tmpFirstName = _cursor.getString(_cursorIndexOfFirstName)
            val _tmpLastName: String
            _tmpLastName = _cursor.getString(_cursorIndexOfLastName)
            val _tmpEmail: String
            _tmpEmail = _cursor.getString(_cursorIndexOfEmail)
            val _tmpPhone: String?
            if (_cursor.isNull(_cursorIndexOfPhone)) {
              _tmpPhone = null
            } else {
              _tmpPhone = _cursor.getString(_cursorIndexOfPhone)
            }
            val _tmpProfilePic: String?
            if (_cursor.isNull(_cursorIndexOfProfilePic)) {
              _tmpProfilePic = null
            } else {
              _tmpProfilePic = _cursor.getString(_cursorIndexOfProfilePic)
            }
            val _tmpDateOfBirth: String?
            if (_cursor.isNull(_cursorIndexOfDateOfBirth)) {
              _tmpDateOfBirth = null
            } else {
              _tmpDateOfBirth = _cursor.getString(_cursorIndexOfDateOfBirth)
            }
            val _tmpGender: String?
            if (_cursor.isNull(_cursorIndexOfGender)) {
              _tmpGender = null
            } else {
              _tmpGender = _cursor.getString(_cursorIndexOfGender)
            }
            val _tmpIsVerified: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsVerified)
            _tmpIsVerified = _tmp != 0
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            val _tmpPreferences: String?
            if (_cursor.isNull(_cursorIndexOfPreferences)) {
              _tmpPreferences = null
            } else {
              _tmpPreferences = _cursor.getString(_cursorIndexOfPreferences)
            }
            _result =
                UserEntity(_tmpId,_tmpFirstName,_tmpLastName,_tmpEmail,_tmpPhone,_tmpProfilePic,_tmpDateOfBirth,_tmpGender,_tmpIsVerified,_tmpCreatedAt,_tmpUpdatedAt,_tmpPreferences)
          } else {
            _result = null
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

  public override fun getUserByEmail(email: String): Flow<UserEntity?> {
    val _sql: String = "SELECT * FROM users WHERE email = ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, email)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("users"), object : Callable<UserEntity?> {
      public override fun call(): UserEntity? {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfFirstName: Int = getColumnIndexOrThrow(_cursor, "firstName")
          val _cursorIndexOfLastName: Int = getColumnIndexOrThrow(_cursor, "lastName")
          val _cursorIndexOfEmail: Int = getColumnIndexOrThrow(_cursor, "email")
          val _cursorIndexOfPhone: Int = getColumnIndexOrThrow(_cursor, "phone")
          val _cursorIndexOfProfilePic: Int = getColumnIndexOrThrow(_cursor, "profilePic")
          val _cursorIndexOfDateOfBirth: Int = getColumnIndexOrThrow(_cursor, "dateOfBirth")
          val _cursorIndexOfGender: Int = getColumnIndexOrThrow(_cursor, "gender")
          val _cursorIndexOfIsVerified: Int = getColumnIndexOrThrow(_cursor, "isVerified")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _cursorIndexOfPreferences: Int = getColumnIndexOrThrow(_cursor, "preferences")
          val _result: UserEntity?
          if (_cursor.moveToFirst()) {
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpFirstName: String
            _tmpFirstName = _cursor.getString(_cursorIndexOfFirstName)
            val _tmpLastName: String
            _tmpLastName = _cursor.getString(_cursorIndexOfLastName)
            val _tmpEmail: String
            _tmpEmail = _cursor.getString(_cursorIndexOfEmail)
            val _tmpPhone: String?
            if (_cursor.isNull(_cursorIndexOfPhone)) {
              _tmpPhone = null
            } else {
              _tmpPhone = _cursor.getString(_cursorIndexOfPhone)
            }
            val _tmpProfilePic: String?
            if (_cursor.isNull(_cursorIndexOfProfilePic)) {
              _tmpProfilePic = null
            } else {
              _tmpProfilePic = _cursor.getString(_cursorIndexOfProfilePic)
            }
            val _tmpDateOfBirth: String?
            if (_cursor.isNull(_cursorIndexOfDateOfBirth)) {
              _tmpDateOfBirth = null
            } else {
              _tmpDateOfBirth = _cursor.getString(_cursorIndexOfDateOfBirth)
            }
            val _tmpGender: String?
            if (_cursor.isNull(_cursorIndexOfGender)) {
              _tmpGender = null
            } else {
              _tmpGender = _cursor.getString(_cursorIndexOfGender)
            }
            val _tmpIsVerified: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsVerified)
            _tmpIsVerified = _tmp != 0
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            val _tmpPreferences: String?
            if (_cursor.isNull(_cursorIndexOfPreferences)) {
              _tmpPreferences = null
            } else {
              _tmpPreferences = _cursor.getString(_cursorIndexOfPreferences)
            }
            _result =
                UserEntity(_tmpId,_tmpFirstName,_tmpLastName,_tmpEmail,_tmpPhone,_tmpProfilePic,_tmpDateOfBirth,_tmpGender,_tmpIsVerified,_tmpCreatedAt,_tmpUpdatedAt,_tmpPreferences)
          } else {
            _result = null
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

  public override fun getAllUsers(): Flow<List<UserEntity>> {
    val _sql: String = "SELECT * FROM users"
    val _statement: RoomSQLiteQuery = acquire(_sql, 0)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("users"), object :
        Callable<List<UserEntity>> {
      public override fun call(): List<UserEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfFirstName: Int = getColumnIndexOrThrow(_cursor, "firstName")
          val _cursorIndexOfLastName: Int = getColumnIndexOrThrow(_cursor, "lastName")
          val _cursorIndexOfEmail: Int = getColumnIndexOrThrow(_cursor, "email")
          val _cursorIndexOfPhone: Int = getColumnIndexOrThrow(_cursor, "phone")
          val _cursorIndexOfProfilePic: Int = getColumnIndexOrThrow(_cursor, "profilePic")
          val _cursorIndexOfDateOfBirth: Int = getColumnIndexOrThrow(_cursor, "dateOfBirth")
          val _cursorIndexOfGender: Int = getColumnIndexOrThrow(_cursor, "gender")
          val _cursorIndexOfIsVerified: Int = getColumnIndexOrThrow(_cursor, "isVerified")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _cursorIndexOfPreferences: Int = getColumnIndexOrThrow(_cursor, "preferences")
          val _result: MutableList<UserEntity> = ArrayList<UserEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: UserEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpFirstName: String
            _tmpFirstName = _cursor.getString(_cursorIndexOfFirstName)
            val _tmpLastName: String
            _tmpLastName = _cursor.getString(_cursorIndexOfLastName)
            val _tmpEmail: String
            _tmpEmail = _cursor.getString(_cursorIndexOfEmail)
            val _tmpPhone: String?
            if (_cursor.isNull(_cursorIndexOfPhone)) {
              _tmpPhone = null
            } else {
              _tmpPhone = _cursor.getString(_cursorIndexOfPhone)
            }
            val _tmpProfilePic: String?
            if (_cursor.isNull(_cursorIndexOfProfilePic)) {
              _tmpProfilePic = null
            } else {
              _tmpProfilePic = _cursor.getString(_cursorIndexOfProfilePic)
            }
            val _tmpDateOfBirth: String?
            if (_cursor.isNull(_cursorIndexOfDateOfBirth)) {
              _tmpDateOfBirth = null
            } else {
              _tmpDateOfBirth = _cursor.getString(_cursorIndexOfDateOfBirth)
            }
            val _tmpGender: String?
            if (_cursor.isNull(_cursorIndexOfGender)) {
              _tmpGender = null
            } else {
              _tmpGender = _cursor.getString(_cursorIndexOfGender)
            }
            val _tmpIsVerified: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsVerified)
            _tmpIsVerified = _tmp != 0
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            val _tmpPreferences: String?
            if (_cursor.isNull(_cursorIndexOfPreferences)) {
              _tmpPreferences = null
            } else {
              _tmpPreferences = _cursor.getString(_cursorIndexOfPreferences)
            }
            _item =
                UserEntity(_tmpId,_tmpFirstName,_tmpLastName,_tmpEmail,_tmpPhone,_tmpProfilePic,_tmpDateOfBirth,_tmpGender,_tmpIsVerified,_tmpCreatedAt,_tmpUpdatedAt,_tmpPreferences)
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

  public companion object {
    @JvmStatic
    public fun getRequiredConverters(): List<Class<*>> = emptyList()
  }
}
