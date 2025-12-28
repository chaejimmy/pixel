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
import com.shourov.apps.pacedream.core.database.entity.MessageEntity
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
public class MessageDao_Impl(
  __db: RoomDatabase,
) : MessageDao {
  private val __db: RoomDatabase

  private val __insertionAdapterOfMessageEntity: EntityInsertionAdapter<MessageEntity>

  private val __deletionAdapterOfMessageEntity: EntityDeletionOrUpdateAdapter<MessageEntity>

  private val __updateAdapterOfMessageEntity: EntityDeletionOrUpdateAdapter<MessageEntity>

  private val __preparedStmtOfDeleteMessageById: SharedSQLiteStatement

  private val __preparedStmtOfDeleteMessagesByChat: SharedSQLiteStatement

  private val __preparedStmtOfMarkMessagesAsRead: SharedSQLiteStatement

  private val __preparedStmtOfDeleteAllMessages: SharedSQLiteStatement
  init {
    this.__db = __db
    this.__insertionAdapterOfMessageEntity = object : EntityInsertionAdapter<MessageEntity>(__db) {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `messages` (`id`,`chatId`,`senderId`,`receiverId`,`content`,`messageType`,`attachmentUrl`,`isRead`,`timestamp`,`createdAt`) VALUES (?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MessageEntity) {
        statement.bindString(1, entity.id)
        statement.bindString(2, entity.chatId)
        statement.bindString(3, entity.senderId)
        statement.bindString(4, entity.receiverId)
        statement.bindString(5, entity.content)
        statement.bindString(6, entity.messageType)
        val _tmpAttachmentUrl: String? = entity.attachmentUrl
        if (_tmpAttachmentUrl == null) {
          statement.bindNull(7)
        } else {
          statement.bindString(7, _tmpAttachmentUrl)
        }
        val _tmp: Int = if (entity.isRead) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        statement.bindString(9, entity.timestamp)
        statement.bindString(10, entity.createdAt)
      }
    }
    this.__deletionAdapterOfMessageEntity = object :
        EntityDeletionOrUpdateAdapter<MessageEntity>(__db) {
      protected override fun createQuery(): String = "DELETE FROM `messages` WHERE `id` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MessageEntity) {
        statement.bindString(1, entity.id)
      }
    }
    this.__updateAdapterOfMessageEntity = object :
        EntityDeletionOrUpdateAdapter<MessageEntity>(__db) {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `messages` SET `id` = ?,`chatId` = ?,`senderId` = ?,`receiverId` = ?,`content` = ?,`messageType` = ?,`attachmentUrl` = ?,`isRead` = ?,`timestamp` = ?,`createdAt` = ? WHERE `id` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MessageEntity) {
        statement.bindString(1, entity.id)
        statement.bindString(2, entity.chatId)
        statement.bindString(3, entity.senderId)
        statement.bindString(4, entity.receiverId)
        statement.bindString(5, entity.content)
        statement.bindString(6, entity.messageType)
        val _tmpAttachmentUrl: String? = entity.attachmentUrl
        if (_tmpAttachmentUrl == null) {
          statement.bindNull(7)
        } else {
          statement.bindString(7, _tmpAttachmentUrl)
        }
        val _tmp: Int = if (entity.isRead) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        statement.bindString(9, entity.timestamp)
        statement.bindString(10, entity.createdAt)
        statement.bindString(11, entity.id)
      }
    }
    this.__preparedStmtOfDeleteMessageById = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "DELETE FROM messages WHERE id = ?"
        return _query
      }
    }
    this.__preparedStmtOfDeleteMessagesByChat = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "DELETE FROM messages WHERE chatId = ?"
        return _query
      }
    }
    this.__preparedStmtOfMarkMessagesAsRead = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "UPDATE messages SET isRead = 1 WHERE chatId = ? AND receiverId = ?"
        return _query
      }
    }
    this.__preparedStmtOfDeleteAllMessages = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "DELETE FROM messages"
        return _query
      }
    }
  }

  public override suspend fun insertMessage(message: MessageEntity): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __insertionAdapterOfMessageEntity.insert(message)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun insertMessages(messages: List<MessageEntity>): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __insertionAdapterOfMessageEntity.insert(messages)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun deleteMessage(message: MessageEntity): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __deletionAdapterOfMessageEntity.handle(message)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun updateMessage(message: MessageEntity): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __updateAdapterOfMessageEntity.handle(message)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun deleteMessageById(messageId: String): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfDeleteMessageById.acquire()
      var _argIndex: Int = 1
      _stmt.bindString(_argIndex, messageId)
      try {
        __db.beginTransaction()
        try {
          _stmt.executeUpdateDelete()
          __db.setTransactionSuccessful()
        } finally {
          __db.endTransaction()
        }
      } finally {
        __preparedStmtOfDeleteMessageById.release(_stmt)
      }
    }
  })

  public override suspend fun deleteMessagesByChat(chatId: String): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfDeleteMessagesByChat.acquire()
      var _argIndex: Int = 1
      _stmt.bindString(_argIndex, chatId)
      try {
        __db.beginTransaction()
        try {
          _stmt.executeUpdateDelete()
          __db.setTransactionSuccessful()
        } finally {
          __db.endTransaction()
        }
      } finally {
        __preparedStmtOfDeleteMessagesByChat.release(_stmt)
      }
    }
  })

  public override suspend fun markMessagesAsRead(chatId: String, userId: String): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfMarkMessagesAsRead.acquire()
      var _argIndex: Int = 1
      _stmt.bindString(_argIndex, chatId)
      _argIndex = 2
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
        __preparedStmtOfMarkMessagesAsRead.release(_stmt)
      }
    }
  })

  public override suspend fun deleteAllMessages(): Unit = CoroutinesRoom.execute(__db, true, object
      : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfDeleteAllMessages.acquire()
      try {
        __db.beginTransaction()
        try {
          _stmt.executeUpdateDelete()
          __db.setTransactionSuccessful()
        } finally {
          __db.endTransaction()
        }
      } finally {
        __preparedStmtOfDeleteAllMessages.release(_stmt)
      }
    }
  })

  public override fun getMessageById(messageId: String): Flow<MessageEntity?> {
    val _sql: String = "SELECT * FROM messages WHERE id = ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, messageId)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("messages"), object :
        Callable<MessageEntity?> {
      public override fun call(): MessageEntity? {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfSenderId: Int = getColumnIndexOrThrow(_cursor, "senderId")
          val _cursorIndexOfReceiverId: Int = getColumnIndexOrThrow(_cursor, "receiverId")
          val _cursorIndexOfContent: Int = getColumnIndexOrThrow(_cursor, "content")
          val _cursorIndexOfMessageType: Int = getColumnIndexOrThrow(_cursor, "messageType")
          val _cursorIndexOfAttachmentUrl: Int = getColumnIndexOrThrow(_cursor, "attachmentUrl")
          val _cursorIndexOfIsRead: Int = getColumnIndexOrThrow(_cursor, "isRead")
          val _cursorIndexOfTimestamp: Int = getColumnIndexOrThrow(_cursor, "timestamp")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _result: MessageEntity?
          if (_cursor.moveToFirst()) {
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpSenderId: String
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId)
            val _tmpReceiverId: String
            _tmpReceiverId = _cursor.getString(_cursorIndexOfReceiverId)
            val _tmpContent: String
            _tmpContent = _cursor.getString(_cursorIndexOfContent)
            val _tmpMessageType: String
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType)
            val _tmpAttachmentUrl: String?
            if (_cursor.isNull(_cursorIndexOfAttachmentUrl)) {
              _tmpAttachmentUrl = null
            } else {
              _tmpAttachmentUrl = _cursor.getString(_cursorIndexOfAttachmentUrl)
            }
            val _tmpIsRead: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsRead)
            _tmpIsRead = _tmp != 0
            val _tmpTimestamp: String
            _tmpTimestamp = _cursor.getString(_cursorIndexOfTimestamp)
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            _result =
                MessageEntity(_tmpId,_tmpChatId,_tmpSenderId,_tmpReceiverId,_tmpContent,_tmpMessageType,_tmpAttachmentUrl,_tmpIsRead,_tmpTimestamp,_tmpCreatedAt)
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

  public override fun getMessagesByChat(chatId: String): Flow<List<MessageEntity>> {
    val _sql: String = "SELECT * FROM messages WHERE chatId = ? ORDER BY timestamp ASC"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, chatId)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("messages"), object :
        Callable<List<MessageEntity>> {
      public override fun call(): List<MessageEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfSenderId: Int = getColumnIndexOrThrow(_cursor, "senderId")
          val _cursorIndexOfReceiverId: Int = getColumnIndexOrThrow(_cursor, "receiverId")
          val _cursorIndexOfContent: Int = getColumnIndexOrThrow(_cursor, "content")
          val _cursorIndexOfMessageType: Int = getColumnIndexOrThrow(_cursor, "messageType")
          val _cursorIndexOfAttachmentUrl: Int = getColumnIndexOrThrow(_cursor, "attachmentUrl")
          val _cursorIndexOfIsRead: Int = getColumnIndexOrThrow(_cursor, "isRead")
          val _cursorIndexOfTimestamp: Int = getColumnIndexOrThrow(_cursor, "timestamp")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _result: MutableList<MessageEntity> = ArrayList<MessageEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: MessageEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpSenderId: String
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId)
            val _tmpReceiverId: String
            _tmpReceiverId = _cursor.getString(_cursorIndexOfReceiverId)
            val _tmpContent: String
            _tmpContent = _cursor.getString(_cursorIndexOfContent)
            val _tmpMessageType: String
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType)
            val _tmpAttachmentUrl: String?
            if (_cursor.isNull(_cursorIndexOfAttachmentUrl)) {
              _tmpAttachmentUrl = null
            } else {
              _tmpAttachmentUrl = _cursor.getString(_cursorIndexOfAttachmentUrl)
            }
            val _tmpIsRead: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsRead)
            _tmpIsRead = _tmp != 0
            val _tmpTimestamp: String
            _tmpTimestamp = _cursor.getString(_cursorIndexOfTimestamp)
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            _item =
                MessageEntity(_tmpId,_tmpChatId,_tmpSenderId,_tmpReceiverId,_tmpContent,_tmpMessageType,_tmpAttachmentUrl,_tmpIsRead,_tmpTimestamp,_tmpCreatedAt)
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

  public override fun getMessagesByUser(userId: String): Flow<List<MessageEntity>> {
    val _sql: String =
        "SELECT * FROM messages WHERE senderId = ? OR receiverId = ? ORDER BY timestamp DESC"
    val _statement: RoomSQLiteQuery = acquire(_sql, 2)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, userId)
    _argIndex = 2
    _statement.bindString(_argIndex, userId)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("messages"), object :
        Callable<List<MessageEntity>> {
      public override fun call(): List<MessageEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfSenderId: Int = getColumnIndexOrThrow(_cursor, "senderId")
          val _cursorIndexOfReceiverId: Int = getColumnIndexOrThrow(_cursor, "receiverId")
          val _cursorIndexOfContent: Int = getColumnIndexOrThrow(_cursor, "content")
          val _cursorIndexOfMessageType: Int = getColumnIndexOrThrow(_cursor, "messageType")
          val _cursorIndexOfAttachmentUrl: Int = getColumnIndexOrThrow(_cursor, "attachmentUrl")
          val _cursorIndexOfIsRead: Int = getColumnIndexOrThrow(_cursor, "isRead")
          val _cursorIndexOfTimestamp: Int = getColumnIndexOrThrow(_cursor, "timestamp")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _result: MutableList<MessageEntity> = ArrayList<MessageEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: MessageEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpSenderId: String
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId)
            val _tmpReceiverId: String
            _tmpReceiverId = _cursor.getString(_cursorIndexOfReceiverId)
            val _tmpContent: String
            _tmpContent = _cursor.getString(_cursorIndexOfContent)
            val _tmpMessageType: String
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType)
            val _tmpAttachmentUrl: String?
            if (_cursor.isNull(_cursorIndexOfAttachmentUrl)) {
              _tmpAttachmentUrl = null
            } else {
              _tmpAttachmentUrl = _cursor.getString(_cursorIndexOfAttachmentUrl)
            }
            val _tmpIsRead: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsRead)
            _tmpIsRead = _tmp != 0
            val _tmpTimestamp: String
            _tmpTimestamp = _cursor.getString(_cursorIndexOfTimestamp)
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            _item =
                MessageEntity(_tmpId,_tmpChatId,_tmpSenderId,_tmpReceiverId,_tmpContent,_tmpMessageType,_tmpAttachmentUrl,_tmpIsRead,_tmpTimestamp,_tmpCreatedAt)
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

  public override fun getUnreadMessagesByChat(chatId: String): Flow<List<MessageEntity>> {
    val _sql: String =
        "SELECT * FROM messages WHERE chatId = ? AND isRead = 0 ORDER BY timestamp ASC"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, chatId)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("messages"), object :
        Callable<List<MessageEntity>> {
      public override fun call(): List<MessageEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfSenderId: Int = getColumnIndexOrThrow(_cursor, "senderId")
          val _cursorIndexOfReceiverId: Int = getColumnIndexOrThrow(_cursor, "receiverId")
          val _cursorIndexOfContent: Int = getColumnIndexOrThrow(_cursor, "content")
          val _cursorIndexOfMessageType: Int = getColumnIndexOrThrow(_cursor, "messageType")
          val _cursorIndexOfAttachmentUrl: Int = getColumnIndexOrThrow(_cursor, "attachmentUrl")
          val _cursorIndexOfIsRead: Int = getColumnIndexOrThrow(_cursor, "isRead")
          val _cursorIndexOfTimestamp: Int = getColumnIndexOrThrow(_cursor, "timestamp")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _result: MutableList<MessageEntity> = ArrayList<MessageEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: MessageEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpSenderId: String
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId)
            val _tmpReceiverId: String
            _tmpReceiverId = _cursor.getString(_cursorIndexOfReceiverId)
            val _tmpContent: String
            _tmpContent = _cursor.getString(_cursorIndexOfContent)
            val _tmpMessageType: String
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType)
            val _tmpAttachmentUrl: String?
            if (_cursor.isNull(_cursorIndexOfAttachmentUrl)) {
              _tmpAttachmentUrl = null
            } else {
              _tmpAttachmentUrl = _cursor.getString(_cursorIndexOfAttachmentUrl)
            }
            val _tmpIsRead: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsRead)
            _tmpIsRead = _tmp != 0
            val _tmpTimestamp: String
            _tmpTimestamp = _cursor.getString(_cursorIndexOfTimestamp)
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            _item =
                MessageEntity(_tmpId,_tmpChatId,_tmpSenderId,_tmpReceiverId,_tmpContent,_tmpMessageType,_tmpAttachmentUrl,_tmpIsRead,_tmpTimestamp,_tmpCreatedAt)
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

  public override fun getUnreadMessagesByUser(userId: String): Flow<List<MessageEntity>> {
    val _sql: String =
        "SELECT * FROM messages WHERE receiverId = ? AND isRead = 0 ORDER BY timestamp DESC"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, userId)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("messages"), object :
        Callable<List<MessageEntity>> {
      public override fun call(): List<MessageEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfSenderId: Int = getColumnIndexOrThrow(_cursor, "senderId")
          val _cursorIndexOfReceiverId: Int = getColumnIndexOrThrow(_cursor, "receiverId")
          val _cursorIndexOfContent: Int = getColumnIndexOrThrow(_cursor, "content")
          val _cursorIndexOfMessageType: Int = getColumnIndexOrThrow(_cursor, "messageType")
          val _cursorIndexOfAttachmentUrl: Int = getColumnIndexOrThrow(_cursor, "attachmentUrl")
          val _cursorIndexOfIsRead: Int = getColumnIndexOrThrow(_cursor, "isRead")
          val _cursorIndexOfTimestamp: Int = getColumnIndexOrThrow(_cursor, "timestamp")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _result: MutableList<MessageEntity> = ArrayList<MessageEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: MessageEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpSenderId: String
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId)
            val _tmpReceiverId: String
            _tmpReceiverId = _cursor.getString(_cursorIndexOfReceiverId)
            val _tmpContent: String
            _tmpContent = _cursor.getString(_cursorIndexOfContent)
            val _tmpMessageType: String
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType)
            val _tmpAttachmentUrl: String?
            if (_cursor.isNull(_cursorIndexOfAttachmentUrl)) {
              _tmpAttachmentUrl = null
            } else {
              _tmpAttachmentUrl = _cursor.getString(_cursorIndexOfAttachmentUrl)
            }
            val _tmpIsRead: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsRead)
            _tmpIsRead = _tmp != 0
            val _tmpTimestamp: String
            _tmpTimestamp = _cursor.getString(_cursorIndexOfTimestamp)
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            _item =
                MessageEntity(_tmpId,_tmpChatId,_tmpSenderId,_tmpReceiverId,_tmpContent,_tmpMessageType,_tmpAttachmentUrl,_tmpIsRead,_tmpTimestamp,_tmpCreatedAt)
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

  public override fun getLastMessageByChat(chatId: String): Flow<MessageEntity?> {
    val _sql: String = "SELECT * FROM messages WHERE chatId = ? ORDER BY timestamp DESC LIMIT 1"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, chatId)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("messages"), object :
        Callable<MessageEntity?> {
      public override fun call(): MessageEntity? {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfSenderId: Int = getColumnIndexOrThrow(_cursor, "senderId")
          val _cursorIndexOfReceiverId: Int = getColumnIndexOrThrow(_cursor, "receiverId")
          val _cursorIndexOfContent: Int = getColumnIndexOrThrow(_cursor, "content")
          val _cursorIndexOfMessageType: Int = getColumnIndexOrThrow(_cursor, "messageType")
          val _cursorIndexOfAttachmentUrl: Int = getColumnIndexOrThrow(_cursor, "attachmentUrl")
          val _cursorIndexOfIsRead: Int = getColumnIndexOrThrow(_cursor, "isRead")
          val _cursorIndexOfTimestamp: Int = getColumnIndexOrThrow(_cursor, "timestamp")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _result: MessageEntity?
          if (_cursor.moveToFirst()) {
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpSenderId: String
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId)
            val _tmpReceiverId: String
            _tmpReceiverId = _cursor.getString(_cursorIndexOfReceiverId)
            val _tmpContent: String
            _tmpContent = _cursor.getString(_cursorIndexOfContent)
            val _tmpMessageType: String
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType)
            val _tmpAttachmentUrl: String?
            if (_cursor.isNull(_cursorIndexOfAttachmentUrl)) {
              _tmpAttachmentUrl = null
            } else {
              _tmpAttachmentUrl = _cursor.getString(_cursorIndexOfAttachmentUrl)
            }
            val _tmpIsRead: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsRead)
            _tmpIsRead = _tmp != 0
            val _tmpTimestamp: String
            _tmpTimestamp = _cursor.getString(_cursorIndexOfTimestamp)
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            _result =
                MessageEntity(_tmpId,_tmpChatId,_tmpSenderId,_tmpReceiverId,_tmpContent,_tmpMessageType,_tmpAttachmentUrl,_tmpIsRead,_tmpTimestamp,_tmpCreatedAt)
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

  public override fun getAllMessages(): Flow<List<MessageEntity>> {
    val _sql: String = "SELECT * FROM messages ORDER BY timestamp DESC"
    val _statement: RoomSQLiteQuery = acquire(_sql, 0)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("messages"), object :
        Callable<List<MessageEntity>> {
      public override fun call(): List<MessageEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfSenderId: Int = getColumnIndexOrThrow(_cursor, "senderId")
          val _cursorIndexOfReceiverId: Int = getColumnIndexOrThrow(_cursor, "receiverId")
          val _cursorIndexOfContent: Int = getColumnIndexOrThrow(_cursor, "content")
          val _cursorIndexOfMessageType: Int = getColumnIndexOrThrow(_cursor, "messageType")
          val _cursorIndexOfAttachmentUrl: Int = getColumnIndexOrThrow(_cursor, "attachmentUrl")
          val _cursorIndexOfIsRead: Int = getColumnIndexOrThrow(_cursor, "isRead")
          val _cursorIndexOfTimestamp: Int = getColumnIndexOrThrow(_cursor, "timestamp")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _result: MutableList<MessageEntity> = ArrayList<MessageEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: MessageEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpSenderId: String
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId)
            val _tmpReceiverId: String
            _tmpReceiverId = _cursor.getString(_cursorIndexOfReceiverId)
            val _tmpContent: String
            _tmpContent = _cursor.getString(_cursorIndexOfContent)
            val _tmpMessageType: String
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType)
            val _tmpAttachmentUrl: String?
            if (_cursor.isNull(_cursorIndexOfAttachmentUrl)) {
              _tmpAttachmentUrl = null
            } else {
              _tmpAttachmentUrl = _cursor.getString(_cursorIndexOfAttachmentUrl)
            }
            val _tmpIsRead: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsRead)
            _tmpIsRead = _tmp != 0
            val _tmpTimestamp: String
            _tmpTimestamp = _cursor.getString(_cursorIndexOfTimestamp)
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            _item =
                MessageEntity(_tmpId,_tmpChatId,_tmpSenderId,_tmpReceiverId,_tmpContent,_tmpMessageType,_tmpAttachmentUrl,_tmpIsRead,_tmpTimestamp,_tmpCreatedAt)
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
