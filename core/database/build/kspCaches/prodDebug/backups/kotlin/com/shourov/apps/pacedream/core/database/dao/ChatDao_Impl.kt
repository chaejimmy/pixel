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
import com.shourov.apps.pacedream.core.database.converter.Converters
import com.shourov.apps.pacedream.core.database.entity.ChatEntity
import java.lang.Class
import java.util.ArrayList
import java.util.concurrent.Callable
import javax.`annotation`.processing.Generated
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
public class ChatDao_Impl(
  __db: RoomDatabase,
) : ChatDao {
  private val __db: RoomDatabase

  private val __insertionAdapterOfChatEntity: EntityInsertionAdapter<ChatEntity>

  private val __converters: Converters = Converters()

  private val __deletionAdapterOfChatEntity: EntityDeletionOrUpdateAdapter<ChatEntity>

  private val __updateAdapterOfChatEntity: EntityDeletionOrUpdateAdapter<ChatEntity>

  private val __preparedStmtOfDeleteAllChats: SharedSQLiteStatement
  init {
    this.__db = __db
    this.__insertionAdapterOfChatEntity = object : EntityInsertionAdapter<ChatEntity>(__db) {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `chats` (`id`,`name`,`participants`,`lastMessage`,`lastMessageTime`,`unreadCount`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: ChatEntity) {
        statement.bindString(1, entity.id)
        statement.bindString(2, entity.name)
        val _tmp: String? = __converters.fromStringList(entity.participants)
        if (_tmp == null) {
          statement.bindNull(3)
        } else {
          statement.bindString(3, _tmp)
        }
        val _tmpLastMessage: String? = entity.lastMessage
        if (_tmpLastMessage == null) {
          statement.bindNull(4)
        } else {
          statement.bindString(4, _tmpLastMessage)
        }
        val _tmpLastMessageTime: String? = entity.lastMessageTime
        if (_tmpLastMessageTime == null) {
          statement.bindNull(5)
        } else {
          statement.bindString(5, _tmpLastMessageTime)
        }
        statement.bindLong(6, entity.unreadCount.toLong())
        statement.bindString(7, entity.createdAt)
        statement.bindString(8, entity.updatedAt)
      }
    }
    this.__deletionAdapterOfChatEntity = object : EntityDeletionOrUpdateAdapter<ChatEntity>(__db) {
      protected override fun createQuery(): String = "DELETE FROM `chats` WHERE `id` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: ChatEntity) {
        statement.bindString(1, entity.id)
      }
    }
    this.__updateAdapterOfChatEntity = object : EntityDeletionOrUpdateAdapter<ChatEntity>(__db) {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `chats` SET `id` = ?,`name` = ?,`participants` = ?,`lastMessage` = ?,`lastMessageTime` = ?,`unreadCount` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: ChatEntity) {
        statement.bindString(1, entity.id)
        statement.bindString(2, entity.name)
        val _tmp: String? = __converters.fromStringList(entity.participants)
        if (_tmp == null) {
          statement.bindNull(3)
        } else {
          statement.bindString(3, _tmp)
        }
        val _tmpLastMessage: String? = entity.lastMessage
        if (_tmpLastMessage == null) {
          statement.bindNull(4)
        } else {
          statement.bindString(4, _tmpLastMessage)
        }
        val _tmpLastMessageTime: String? = entity.lastMessageTime
        if (_tmpLastMessageTime == null) {
          statement.bindNull(5)
        } else {
          statement.bindString(5, _tmpLastMessageTime)
        }
        statement.bindLong(6, entity.unreadCount.toLong())
        statement.bindString(7, entity.createdAt)
        statement.bindString(8, entity.updatedAt)
        statement.bindString(9, entity.id)
      }
    }
    this.__preparedStmtOfDeleteAllChats = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "DELETE FROM chats"
        return _query
      }
    }
  }

  public override suspend fun insertChat(chat: ChatEntity): Unit = CoroutinesRoom.execute(__db,
      true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __insertionAdapterOfChatEntity.insert(chat)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun insertChats(chats: List<ChatEntity>): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __insertionAdapterOfChatEntity.insert(chats)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun deleteChat(chat: ChatEntity): Unit = CoroutinesRoom.execute(__db,
      true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __deletionAdapterOfChatEntity.handle(chat)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun updateChat(chat: ChatEntity): Unit = CoroutinesRoom.execute(__db,
      true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __updateAdapterOfChatEntity.handle(chat)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun deleteAllChats(): Unit = CoroutinesRoom.execute(__db, true, object :
      Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfDeleteAllChats.acquire()
      try {
        __db.beginTransaction()
        try {
          _stmt.executeUpdateDelete()
          __db.setTransactionSuccessful()
        } finally {
          __db.endTransaction()
        }
      } finally {
        __preparedStmtOfDeleteAllChats.release(_stmt)
      }
    }
  })

  public override fun getAllChats(): Flow<List<ChatEntity>> {
    val _sql: String = "SELECT * FROM chats ORDER BY lastMessageTime DESC"
    val _statement: RoomSQLiteQuery = acquire(_sql, 0)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("chats"), object :
        Callable<List<ChatEntity>> {
      public override fun call(): List<ChatEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfName: Int = getColumnIndexOrThrow(_cursor, "name")
          val _cursorIndexOfParticipants: Int = getColumnIndexOrThrow(_cursor, "participants")
          val _cursorIndexOfLastMessage: Int = getColumnIndexOrThrow(_cursor, "lastMessage")
          val _cursorIndexOfLastMessageTime: Int = getColumnIndexOrThrow(_cursor, "lastMessageTime")
          val _cursorIndexOfUnreadCount: Int = getColumnIndexOrThrow(_cursor, "unreadCount")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: MutableList<ChatEntity> = ArrayList<ChatEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: ChatEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpName: String
            _tmpName = _cursor.getString(_cursorIndexOfName)
            val _tmpParticipants: List<String>
            val _tmp: String?
            if (_cursor.isNull(_cursorIndexOfParticipants)) {
              _tmp = null
            } else {
              _tmp = _cursor.getString(_cursorIndexOfParticipants)
            }
            val _tmp_1: List<String>? = __converters.toStringList(_tmp)
            if (_tmp_1 == null) {
              error("Expected NON-NULL 'kotlin.collections.List<kotlin.String>', but it was NULL.")
            } else {
              _tmpParticipants = _tmp_1
            }
            val _tmpLastMessage: String?
            if (_cursor.isNull(_cursorIndexOfLastMessage)) {
              _tmpLastMessage = null
            } else {
              _tmpLastMessage = _cursor.getString(_cursorIndexOfLastMessage)
            }
            val _tmpLastMessageTime: String?
            if (_cursor.isNull(_cursorIndexOfLastMessageTime)) {
              _tmpLastMessageTime = null
            } else {
              _tmpLastMessageTime = _cursor.getString(_cursorIndexOfLastMessageTime)
            }
            val _tmpUnreadCount: Int
            _tmpUnreadCount = _cursor.getInt(_cursorIndexOfUnreadCount)
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            _item =
                ChatEntity(_tmpId,_tmpName,_tmpParticipants,_tmpLastMessage,_tmpLastMessageTime,_tmpUnreadCount,_tmpCreatedAt,_tmpUpdatedAt)
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

  public override suspend fun getChatById(chatId: String): ChatEntity? {
    val _sql: String = "SELECT * FROM chats WHERE id = ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, chatId)
    val _cancellationSignal: CancellationSignal? = createCancellationSignal()
    return execute(__db, false, _cancellationSignal, object : Callable<ChatEntity?> {
      public override fun call(): ChatEntity? {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfName: Int = getColumnIndexOrThrow(_cursor, "name")
          val _cursorIndexOfParticipants: Int = getColumnIndexOrThrow(_cursor, "participants")
          val _cursorIndexOfLastMessage: Int = getColumnIndexOrThrow(_cursor, "lastMessage")
          val _cursorIndexOfLastMessageTime: Int = getColumnIndexOrThrow(_cursor, "lastMessageTime")
          val _cursorIndexOfUnreadCount: Int = getColumnIndexOrThrow(_cursor, "unreadCount")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: ChatEntity?
          if (_cursor.moveToFirst()) {
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpName: String
            _tmpName = _cursor.getString(_cursorIndexOfName)
            val _tmpParticipants: List<String>
            val _tmp: String?
            if (_cursor.isNull(_cursorIndexOfParticipants)) {
              _tmp = null
            } else {
              _tmp = _cursor.getString(_cursorIndexOfParticipants)
            }
            val _tmp_1: List<String>? = __converters.toStringList(_tmp)
            if (_tmp_1 == null) {
              error("Expected NON-NULL 'kotlin.collections.List<kotlin.String>', but it was NULL.")
            } else {
              _tmpParticipants = _tmp_1
            }
            val _tmpLastMessage: String?
            if (_cursor.isNull(_cursorIndexOfLastMessage)) {
              _tmpLastMessage = null
            } else {
              _tmpLastMessage = _cursor.getString(_cursorIndexOfLastMessage)
            }
            val _tmpLastMessageTime: String?
            if (_cursor.isNull(_cursorIndexOfLastMessageTime)) {
              _tmpLastMessageTime = null
            } else {
              _tmpLastMessageTime = _cursor.getString(_cursorIndexOfLastMessageTime)
            }
            val _tmpUnreadCount: Int
            _tmpUnreadCount = _cursor.getInt(_cursorIndexOfUnreadCount)
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            _result =
                ChatEntity(_tmpId,_tmpName,_tmpParticipants,_tmpLastMessage,_tmpLastMessageTime,_tmpUnreadCount,_tmpCreatedAt,_tmpUpdatedAt)
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

  public override fun getChatsByUser(userId: String): Flow<List<ChatEntity>> {
    val _sql: String =
        "SELECT * FROM chats WHERE participants LIKE '%' || ? || '%' ORDER BY lastMessageTime DESC"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, userId)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("chats"), object :
        Callable<List<ChatEntity>> {
      public override fun call(): List<ChatEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfName: Int = getColumnIndexOrThrow(_cursor, "name")
          val _cursorIndexOfParticipants: Int = getColumnIndexOrThrow(_cursor, "participants")
          val _cursorIndexOfLastMessage: Int = getColumnIndexOrThrow(_cursor, "lastMessage")
          val _cursorIndexOfLastMessageTime: Int = getColumnIndexOrThrow(_cursor, "lastMessageTime")
          val _cursorIndexOfUnreadCount: Int = getColumnIndexOrThrow(_cursor, "unreadCount")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: MutableList<ChatEntity> = ArrayList<ChatEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: ChatEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpName: String
            _tmpName = _cursor.getString(_cursorIndexOfName)
            val _tmpParticipants: List<String>
            val _tmp: String?
            if (_cursor.isNull(_cursorIndexOfParticipants)) {
              _tmp = null
            } else {
              _tmp = _cursor.getString(_cursorIndexOfParticipants)
            }
            val _tmp_1: List<String>? = __converters.toStringList(_tmp)
            if (_tmp_1 == null) {
              error("Expected NON-NULL 'kotlin.collections.List<kotlin.String>', but it was NULL.")
            } else {
              _tmpParticipants = _tmp_1
            }
            val _tmpLastMessage: String?
            if (_cursor.isNull(_cursorIndexOfLastMessage)) {
              _tmpLastMessage = null
            } else {
              _tmpLastMessage = _cursor.getString(_cursorIndexOfLastMessage)
            }
            val _tmpLastMessageTime: String?
            if (_cursor.isNull(_cursorIndexOfLastMessageTime)) {
              _tmpLastMessageTime = null
            } else {
              _tmpLastMessageTime = _cursor.getString(_cursorIndexOfLastMessageTime)
            }
            val _tmpUnreadCount: Int
            _tmpUnreadCount = _cursor.getInt(_cursorIndexOfUnreadCount)
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            _item =
                ChatEntity(_tmpId,_tmpName,_tmpParticipants,_tmpLastMessage,_tmpLastMessageTime,_tmpUnreadCount,_tmpCreatedAt,_tmpUpdatedAt)
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
