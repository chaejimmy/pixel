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
import com.shourov.apps.pacedream.core.database.entity.BookingEntity
import java.lang.Class
import java.util.ArrayList
import java.util.concurrent.Callable
import javax.`annotation`.processing.Generated
import kotlin.Double
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
public class BookingDao_Impl(
  __db: RoomDatabase,
) : BookingDao {
  private val __db: RoomDatabase

  private val __insertionAdapterOfBookingEntity: EntityInsertionAdapter<BookingEntity>

  private val __deletionAdapterOfBookingEntity: EntityDeletionOrUpdateAdapter<BookingEntity>

  private val __updateAdapterOfBookingEntity: EntityDeletionOrUpdateAdapter<BookingEntity>

  private val __preparedStmtOfDeleteBookingById: SharedSQLiteStatement

  private val __preparedStmtOfDeleteBookingsByUser: SharedSQLiteStatement

  private val __preparedStmtOfDeleteAllBookings: SharedSQLiteStatement
  init {
    this.__db = __db
    this.__insertionAdapterOfBookingEntity = object : EntityInsertionAdapter<BookingEntity>(__db) {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `bookings` (`id`,`userId`,`propertyId`,`propertyName`,`propertyImage`,`startDate`,`endDate`,`startTime`,`endTime`,`totalPrice`,`currency`,`status`,`paymentStatus`,`specialRequests`,`hostId`,`hostName`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: BookingEntity) {
        statement.bindString(1, entity.id)
        statement.bindString(2, entity.userId)
        statement.bindString(3, entity.propertyId)
        statement.bindString(4, entity.propertyName)
        val _tmpPropertyImage: String? = entity.propertyImage
        if (_tmpPropertyImage == null) {
          statement.bindNull(5)
        } else {
          statement.bindString(5, _tmpPropertyImage)
        }
        statement.bindString(6, entity.startDate)
        statement.bindString(7, entity.endDate)
        val _tmpStartTime: String? = entity.startTime
        if (_tmpStartTime == null) {
          statement.bindNull(8)
        } else {
          statement.bindString(8, _tmpStartTime)
        }
        val _tmpEndTime: String? = entity.endTime
        if (_tmpEndTime == null) {
          statement.bindNull(9)
        } else {
          statement.bindString(9, _tmpEndTime)
        }
        statement.bindDouble(10, entity.totalPrice)
        statement.bindString(11, entity.currency)
        statement.bindString(12, entity.status)
        statement.bindString(13, entity.paymentStatus)
        val _tmpSpecialRequests: String? = entity.specialRequests
        if (_tmpSpecialRequests == null) {
          statement.bindNull(14)
        } else {
          statement.bindString(14, _tmpSpecialRequests)
        }
        statement.bindString(15, entity.hostId)
        statement.bindString(16, entity.hostName)
        statement.bindString(17, entity.createdAt)
        statement.bindString(18, entity.updatedAt)
      }
    }
    this.__deletionAdapterOfBookingEntity = object :
        EntityDeletionOrUpdateAdapter<BookingEntity>(__db) {
      protected override fun createQuery(): String = "DELETE FROM `bookings` WHERE `id` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: BookingEntity) {
        statement.bindString(1, entity.id)
      }
    }
    this.__updateAdapterOfBookingEntity = object :
        EntityDeletionOrUpdateAdapter<BookingEntity>(__db) {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `bookings` SET `id` = ?,`userId` = ?,`propertyId` = ?,`propertyName` = ?,`propertyImage` = ?,`startDate` = ?,`endDate` = ?,`startTime` = ?,`endTime` = ?,`totalPrice` = ?,`currency` = ?,`status` = ?,`paymentStatus` = ?,`specialRequests` = ?,`hostId` = ?,`hostName` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: BookingEntity) {
        statement.bindString(1, entity.id)
        statement.bindString(2, entity.userId)
        statement.bindString(3, entity.propertyId)
        statement.bindString(4, entity.propertyName)
        val _tmpPropertyImage: String? = entity.propertyImage
        if (_tmpPropertyImage == null) {
          statement.bindNull(5)
        } else {
          statement.bindString(5, _tmpPropertyImage)
        }
        statement.bindString(6, entity.startDate)
        statement.bindString(7, entity.endDate)
        val _tmpStartTime: String? = entity.startTime
        if (_tmpStartTime == null) {
          statement.bindNull(8)
        } else {
          statement.bindString(8, _tmpStartTime)
        }
        val _tmpEndTime: String? = entity.endTime
        if (_tmpEndTime == null) {
          statement.bindNull(9)
        } else {
          statement.bindString(9, _tmpEndTime)
        }
        statement.bindDouble(10, entity.totalPrice)
        statement.bindString(11, entity.currency)
        statement.bindString(12, entity.status)
        statement.bindString(13, entity.paymentStatus)
        val _tmpSpecialRequests: String? = entity.specialRequests
        if (_tmpSpecialRequests == null) {
          statement.bindNull(14)
        } else {
          statement.bindString(14, _tmpSpecialRequests)
        }
        statement.bindString(15, entity.hostId)
        statement.bindString(16, entity.hostName)
        statement.bindString(17, entity.createdAt)
        statement.bindString(18, entity.updatedAt)
        statement.bindString(19, entity.id)
      }
    }
    this.__preparedStmtOfDeleteBookingById = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "DELETE FROM bookings WHERE id = ?"
        return _query
      }
    }
    this.__preparedStmtOfDeleteBookingsByUser = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "DELETE FROM bookings WHERE userId = ?"
        return _query
      }
    }
    this.__preparedStmtOfDeleteAllBookings = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "DELETE FROM bookings"
        return _query
      }
    }
  }

  public override suspend fun insertBooking(booking: BookingEntity): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __insertionAdapterOfBookingEntity.insert(booking)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun insertBookings(bookings: List<BookingEntity>): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __insertionAdapterOfBookingEntity.insert(bookings)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun deleteBooking(booking: BookingEntity): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __deletionAdapterOfBookingEntity.handle(booking)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun updateBooking(booking: BookingEntity): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __updateAdapterOfBookingEntity.handle(booking)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun deleteBookingById(bookingId: String): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfDeleteBookingById.acquire()
      var _argIndex: Int = 1
      _stmt.bindString(_argIndex, bookingId)
      try {
        __db.beginTransaction()
        try {
          _stmt.executeUpdateDelete()
          __db.setTransactionSuccessful()
        } finally {
          __db.endTransaction()
        }
      } finally {
        __preparedStmtOfDeleteBookingById.release(_stmt)
      }
    }
  })

  public override suspend fun deleteBookingsByUser(userId: String): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfDeleteBookingsByUser.acquire()
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
        __preparedStmtOfDeleteBookingsByUser.release(_stmt)
      }
    }
  })

  public override suspend fun deleteAllBookings(): Unit = CoroutinesRoom.execute(__db, true, object
      : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfDeleteAllBookings.acquire()
      try {
        __db.beginTransaction()
        try {
          _stmt.executeUpdateDelete()
          __db.setTransactionSuccessful()
        } finally {
          __db.endTransaction()
        }
      } finally {
        __preparedStmtOfDeleteAllBookings.release(_stmt)
      }
    }
  })

  public override fun getBookingById(bookingId: String): Flow<BookingEntity?> {
    val _sql: String = "SELECT * FROM bookings WHERE id = ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, bookingId)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("bookings"), object :
        Callable<BookingEntity?> {
      public override fun call(): BookingEntity? {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfUserId: Int = getColumnIndexOrThrow(_cursor, "userId")
          val _cursorIndexOfPropertyId: Int = getColumnIndexOrThrow(_cursor, "propertyId")
          val _cursorIndexOfPropertyName: Int = getColumnIndexOrThrow(_cursor, "propertyName")
          val _cursorIndexOfPropertyImage: Int = getColumnIndexOrThrow(_cursor, "propertyImage")
          val _cursorIndexOfStartDate: Int = getColumnIndexOrThrow(_cursor, "startDate")
          val _cursorIndexOfEndDate: Int = getColumnIndexOrThrow(_cursor, "endDate")
          val _cursorIndexOfStartTime: Int = getColumnIndexOrThrow(_cursor, "startTime")
          val _cursorIndexOfEndTime: Int = getColumnIndexOrThrow(_cursor, "endTime")
          val _cursorIndexOfTotalPrice: Int = getColumnIndexOrThrow(_cursor, "totalPrice")
          val _cursorIndexOfCurrency: Int = getColumnIndexOrThrow(_cursor, "currency")
          val _cursorIndexOfStatus: Int = getColumnIndexOrThrow(_cursor, "status")
          val _cursorIndexOfPaymentStatus: Int = getColumnIndexOrThrow(_cursor, "paymentStatus")
          val _cursorIndexOfSpecialRequests: Int = getColumnIndexOrThrow(_cursor, "specialRequests")
          val _cursorIndexOfHostId: Int = getColumnIndexOrThrow(_cursor, "hostId")
          val _cursorIndexOfHostName: Int = getColumnIndexOrThrow(_cursor, "hostName")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: BookingEntity?
          if (_cursor.moveToFirst()) {
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpUserId: String
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId)
            val _tmpPropertyId: String
            _tmpPropertyId = _cursor.getString(_cursorIndexOfPropertyId)
            val _tmpPropertyName: String
            _tmpPropertyName = _cursor.getString(_cursorIndexOfPropertyName)
            val _tmpPropertyImage: String?
            if (_cursor.isNull(_cursorIndexOfPropertyImage)) {
              _tmpPropertyImage = null
            } else {
              _tmpPropertyImage = _cursor.getString(_cursorIndexOfPropertyImage)
            }
            val _tmpStartDate: String
            _tmpStartDate = _cursor.getString(_cursorIndexOfStartDate)
            val _tmpEndDate: String
            _tmpEndDate = _cursor.getString(_cursorIndexOfEndDate)
            val _tmpStartTime: String?
            if (_cursor.isNull(_cursorIndexOfStartTime)) {
              _tmpStartTime = null
            } else {
              _tmpStartTime = _cursor.getString(_cursorIndexOfStartTime)
            }
            val _tmpEndTime: String?
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null
            } else {
              _tmpEndTime = _cursor.getString(_cursorIndexOfEndTime)
            }
            val _tmpTotalPrice: Double
            _tmpTotalPrice = _cursor.getDouble(_cursorIndexOfTotalPrice)
            val _tmpCurrency: String
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency)
            val _tmpStatus: String
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus)
            val _tmpPaymentStatus: String
            _tmpPaymentStatus = _cursor.getString(_cursorIndexOfPaymentStatus)
            val _tmpSpecialRequests: String?
            if (_cursor.isNull(_cursorIndexOfSpecialRequests)) {
              _tmpSpecialRequests = null
            } else {
              _tmpSpecialRequests = _cursor.getString(_cursorIndexOfSpecialRequests)
            }
            val _tmpHostId: String
            _tmpHostId = _cursor.getString(_cursorIndexOfHostId)
            val _tmpHostName: String
            _tmpHostName = _cursor.getString(_cursorIndexOfHostName)
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            _result =
                BookingEntity(_tmpId,_tmpUserId,_tmpPropertyId,_tmpPropertyName,_tmpPropertyImage,_tmpStartDate,_tmpEndDate,_tmpStartTime,_tmpEndTime,_tmpTotalPrice,_tmpCurrency,_tmpStatus,_tmpPaymentStatus,_tmpSpecialRequests,_tmpHostId,_tmpHostName,_tmpCreatedAt,_tmpUpdatedAt)
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

  public override fun getBookingsByUser(userId: String): Flow<List<BookingEntity>> {
    val _sql: String = "SELECT * FROM bookings WHERE userId = ? ORDER BY createdAt DESC"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, userId)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("bookings"), object :
        Callable<List<BookingEntity>> {
      public override fun call(): List<BookingEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfUserId: Int = getColumnIndexOrThrow(_cursor, "userId")
          val _cursorIndexOfPropertyId: Int = getColumnIndexOrThrow(_cursor, "propertyId")
          val _cursorIndexOfPropertyName: Int = getColumnIndexOrThrow(_cursor, "propertyName")
          val _cursorIndexOfPropertyImage: Int = getColumnIndexOrThrow(_cursor, "propertyImage")
          val _cursorIndexOfStartDate: Int = getColumnIndexOrThrow(_cursor, "startDate")
          val _cursorIndexOfEndDate: Int = getColumnIndexOrThrow(_cursor, "endDate")
          val _cursorIndexOfStartTime: Int = getColumnIndexOrThrow(_cursor, "startTime")
          val _cursorIndexOfEndTime: Int = getColumnIndexOrThrow(_cursor, "endTime")
          val _cursorIndexOfTotalPrice: Int = getColumnIndexOrThrow(_cursor, "totalPrice")
          val _cursorIndexOfCurrency: Int = getColumnIndexOrThrow(_cursor, "currency")
          val _cursorIndexOfStatus: Int = getColumnIndexOrThrow(_cursor, "status")
          val _cursorIndexOfPaymentStatus: Int = getColumnIndexOrThrow(_cursor, "paymentStatus")
          val _cursorIndexOfSpecialRequests: Int = getColumnIndexOrThrow(_cursor, "specialRequests")
          val _cursorIndexOfHostId: Int = getColumnIndexOrThrow(_cursor, "hostId")
          val _cursorIndexOfHostName: Int = getColumnIndexOrThrow(_cursor, "hostName")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: MutableList<BookingEntity> = ArrayList<BookingEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: BookingEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpUserId: String
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId)
            val _tmpPropertyId: String
            _tmpPropertyId = _cursor.getString(_cursorIndexOfPropertyId)
            val _tmpPropertyName: String
            _tmpPropertyName = _cursor.getString(_cursorIndexOfPropertyName)
            val _tmpPropertyImage: String?
            if (_cursor.isNull(_cursorIndexOfPropertyImage)) {
              _tmpPropertyImage = null
            } else {
              _tmpPropertyImage = _cursor.getString(_cursorIndexOfPropertyImage)
            }
            val _tmpStartDate: String
            _tmpStartDate = _cursor.getString(_cursorIndexOfStartDate)
            val _tmpEndDate: String
            _tmpEndDate = _cursor.getString(_cursorIndexOfEndDate)
            val _tmpStartTime: String?
            if (_cursor.isNull(_cursorIndexOfStartTime)) {
              _tmpStartTime = null
            } else {
              _tmpStartTime = _cursor.getString(_cursorIndexOfStartTime)
            }
            val _tmpEndTime: String?
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null
            } else {
              _tmpEndTime = _cursor.getString(_cursorIndexOfEndTime)
            }
            val _tmpTotalPrice: Double
            _tmpTotalPrice = _cursor.getDouble(_cursorIndexOfTotalPrice)
            val _tmpCurrency: String
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency)
            val _tmpStatus: String
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus)
            val _tmpPaymentStatus: String
            _tmpPaymentStatus = _cursor.getString(_cursorIndexOfPaymentStatus)
            val _tmpSpecialRequests: String?
            if (_cursor.isNull(_cursorIndexOfSpecialRequests)) {
              _tmpSpecialRequests = null
            } else {
              _tmpSpecialRequests = _cursor.getString(_cursorIndexOfSpecialRequests)
            }
            val _tmpHostId: String
            _tmpHostId = _cursor.getString(_cursorIndexOfHostId)
            val _tmpHostName: String
            _tmpHostName = _cursor.getString(_cursorIndexOfHostName)
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            _item =
                BookingEntity(_tmpId,_tmpUserId,_tmpPropertyId,_tmpPropertyName,_tmpPropertyImage,_tmpStartDate,_tmpEndDate,_tmpStartTime,_tmpEndTime,_tmpTotalPrice,_tmpCurrency,_tmpStatus,_tmpPaymentStatus,_tmpSpecialRequests,_tmpHostId,_tmpHostName,_tmpCreatedAt,_tmpUpdatedAt)
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

  public override fun getBookingsByHost(hostId: String): Flow<List<BookingEntity>> {
    val _sql: String = "SELECT * FROM bookings WHERE hostId = ? ORDER BY createdAt DESC"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, hostId)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("bookings"), object :
        Callable<List<BookingEntity>> {
      public override fun call(): List<BookingEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfUserId: Int = getColumnIndexOrThrow(_cursor, "userId")
          val _cursorIndexOfPropertyId: Int = getColumnIndexOrThrow(_cursor, "propertyId")
          val _cursorIndexOfPropertyName: Int = getColumnIndexOrThrow(_cursor, "propertyName")
          val _cursorIndexOfPropertyImage: Int = getColumnIndexOrThrow(_cursor, "propertyImage")
          val _cursorIndexOfStartDate: Int = getColumnIndexOrThrow(_cursor, "startDate")
          val _cursorIndexOfEndDate: Int = getColumnIndexOrThrow(_cursor, "endDate")
          val _cursorIndexOfStartTime: Int = getColumnIndexOrThrow(_cursor, "startTime")
          val _cursorIndexOfEndTime: Int = getColumnIndexOrThrow(_cursor, "endTime")
          val _cursorIndexOfTotalPrice: Int = getColumnIndexOrThrow(_cursor, "totalPrice")
          val _cursorIndexOfCurrency: Int = getColumnIndexOrThrow(_cursor, "currency")
          val _cursorIndexOfStatus: Int = getColumnIndexOrThrow(_cursor, "status")
          val _cursorIndexOfPaymentStatus: Int = getColumnIndexOrThrow(_cursor, "paymentStatus")
          val _cursorIndexOfSpecialRequests: Int = getColumnIndexOrThrow(_cursor, "specialRequests")
          val _cursorIndexOfHostId: Int = getColumnIndexOrThrow(_cursor, "hostId")
          val _cursorIndexOfHostName: Int = getColumnIndexOrThrow(_cursor, "hostName")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: MutableList<BookingEntity> = ArrayList<BookingEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: BookingEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpUserId: String
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId)
            val _tmpPropertyId: String
            _tmpPropertyId = _cursor.getString(_cursorIndexOfPropertyId)
            val _tmpPropertyName: String
            _tmpPropertyName = _cursor.getString(_cursorIndexOfPropertyName)
            val _tmpPropertyImage: String?
            if (_cursor.isNull(_cursorIndexOfPropertyImage)) {
              _tmpPropertyImage = null
            } else {
              _tmpPropertyImage = _cursor.getString(_cursorIndexOfPropertyImage)
            }
            val _tmpStartDate: String
            _tmpStartDate = _cursor.getString(_cursorIndexOfStartDate)
            val _tmpEndDate: String
            _tmpEndDate = _cursor.getString(_cursorIndexOfEndDate)
            val _tmpStartTime: String?
            if (_cursor.isNull(_cursorIndexOfStartTime)) {
              _tmpStartTime = null
            } else {
              _tmpStartTime = _cursor.getString(_cursorIndexOfStartTime)
            }
            val _tmpEndTime: String?
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null
            } else {
              _tmpEndTime = _cursor.getString(_cursorIndexOfEndTime)
            }
            val _tmpTotalPrice: Double
            _tmpTotalPrice = _cursor.getDouble(_cursorIndexOfTotalPrice)
            val _tmpCurrency: String
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency)
            val _tmpStatus: String
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus)
            val _tmpPaymentStatus: String
            _tmpPaymentStatus = _cursor.getString(_cursorIndexOfPaymentStatus)
            val _tmpSpecialRequests: String?
            if (_cursor.isNull(_cursorIndexOfSpecialRequests)) {
              _tmpSpecialRequests = null
            } else {
              _tmpSpecialRequests = _cursor.getString(_cursorIndexOfSpecialRequests)
            }
            val _tmpHostId: String
            _tmpHostId = _cursor.getString(_cursorIndexOfHostId)
            val _tmpHostName: String
            _tmpHostName = _cursor.getString(_cursorIndexOfHostName)
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            _item =
                BookingEntity(_tmpId,_tmpUserId,_tmpPropertyId,_tmpPropertyName,_tmpPropertyImage,_tmpStartDate,_tmpEndDate,_tmpStartTime,_tmpEndTime,_tmpTotalPrice,_tmpCurrency,_tmpStatus,_tmpPaymentStatus,_tmpSpecialRequests,_tmpHostId,_tmpHostName,_tmpCreatedAt,_tmpUpdatedAt)
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

  public override fun getBookingsByProperty(propertyId: String): Flow<List<BookingEntity>> {
    val _sql: String = "SELECT * FROM bookings WHERE propertyId = ? ORDER BY createdAt DESC"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, propertyId)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("bookings"), object :
        Callable<List<BookingEntity>> {
      public override fun call(): List<BookingEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfUserId: Int = getColumnIndexOrThrow(_cursor, "userId")
          val _cursorIndexOfPropertyId: Int = getColumnIndexOrThrow(_cursor, "propertyId")
          val _cursorIndexOfPropertyName: Int = getColumnIndexOrThrow(_cursor, "propertyName")
          val _cursorIndexOfPropertyImage: Int = getColumnIndexOrThrow(_cursor, "propertyImage")
          val _cursorIndexOfStartDate: Int = getColumnIndexOrThrow(_cursor, "startDate")
          val _cursorIndexOfEndDate: Int = getColumnIndexOrThrow(_cursor, "endDate")
          val _cursorIndexOfStartTime: Int = getColumnIndexOrThrow(_cursor, "startTime")
          val _cursorIndexOfEndTime: Int = getColumnIndexOrThrow(_cursor, "endTime")
          val _cursorIndexOfTotalPrice: Int = getColumnIndexOrThrow(_cursor, "totalPrice")
          val _cursorIndexOfCurrency: Int = getColumnIndexOrThrow(_cursor, "currency")
          val _cursorIndexOfStatus: Int = getColumnIndexOrThrow(_cursor, "status")
          val _cursorIndexOfPaymentStatus: Int = getColumnIndexOrThrow(_cursor, "paymentStatus")
          val _cursorIndexOfSpecialRequests: Int = getColumnIndexOrThrow(_cursor, "specialRequests")
          val _cursorIndexOfHostId: Int = getColumnIndexOrThrow(_cursor, "hostId")
          val _cursorIndexOfHostName: Int = getColumnIndexOrThrow(_cursor, "hostName")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: MutableList<BookingEntity> = ArrayList<BookingEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: BookingEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpUserId: String
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId)
            val _tmpPropertyId: String
            _tmpPropertyId = _cursor.getString(_cursorIndexOfPropertyId)
            val _tmpPropertyName: String
            _tmpPropertyName = _cursor.getString(_cursorIndexOfPropertyName)
            val _tmpPropertyImage: String?
            if (_cursor.isNull(_cursorIndexOfPropertyImage)) {
              _tmpPropertyImage = null
            } else {
              _tmpPropertyImage = _cursor.getString(_cursorIndexOfPropertyImage)
            }
            val _tmpStartDate: String
            _tmpStartDate = _cursor.getString(_cursorIndexOfStartDate)
            val _tmpEndDate: String
            _tmpEndDate = _cursor.getString(_cursorIndexOfEndDate)
            val _tmpStartTime: String?
            if (_cursor.isNull(_cursorIndexOfStartTime)) {
              _tmpStartTime = null
            } else {
              _tmpStartTime = _cursor.getString(_cursorIndexOfStartTime)
            }
            val _tmpEndTime: String?
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null
            } else {
              _tmpEndTime = _cursor.getString(_cursorIndexOfEndTime)
            }
            val _tmpTotalPrice: Double
            _tmpTotalPrice = _cursor.getDouble(_cursorIndexOfTotalPrice)
            val _tmpCurrency: String
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency)
            val _tmpStatus: String
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus)
            val _tmpPaymentStatus: String
            _tmpPaymentStatus = _cursor.getString(_cursorIndexOfPaymentStatus)
            val _tmpSpecialRequests: String?
            if (_cursor.isNull(_cursorIndexOfSpecialRequests)) {
              _tmpSpecialRequests = null
            } else {
              _tmpSpecialRequests = _cursor.getString(_cursorIndexOfSpecialRequests)
            }
            val _tmpHostId: String
            _tmpHostId = _cursor.getString(_cursorIndexOfHostId)
            val _tmpHostName: String
            _tmpHostName = _cursor.getString(_cursorIndexOfHostName)
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            _item =
                BookingEntity(_tmpId,_tmpUserId,_tmpPropertyId,_tmpPropertyName,_tmpPropertyImage,_tmpStartDate,_tmpEndDate,_tmpStartTime,_tmpEndTime,_tmpTotalPrice,_tmpCurrency,_tmpStatus,_tmpPaymentStatus,_tmpSpecialRequests,_tmpHostId,_tmpHostName,_tmpCreatedAt,_tmpUpdatedAt)
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

  public override fun getBookingsByStatus(status: String): Flow<List<BookingEntity>> {
    val _sql: String = "SELECT * FROM bookings WHERE status = ? ORDER BY createdAt DESC"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, status)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("bookings"), object :
        Callable<List<BookingEntity>> {
      public override fun call(): List<BookingEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfUserId: Int = getColumnIndexOrThrow(_cursor, "userId")
          val _cursorIndexOfPropertyId: Int = getColumnIndexOrThrow(_cursor, "propertyId")
          val _cursorIndexOfPropertyName: Int = getColumnIndexOrThrow(_cursor, "propertyName")
          val _cursorIndexOfPropertyImage: Int = getColumnIndexOrThrow(_cursor, "propertyImage")
          val _cursorIndexOfStartDate: Int = getColumnIndexOrThrow(_cursor, "startDate")
          val _cursorIndexOfEndDate: Int = getColumnIndexOrThrow(_cursor, "endDate")
          val _cursorIndexOfStartTime: Int = getColumnIndexOrThrow(_cursor, "startTime")
          val _cursorIndexOfEndTime: Int = getColumnIndexOrThrow(_cursor, "endTime")
          val _cursorIndexOfTotalPrice: Int = getColumnIndexOrThrow(_cursor, "totalPrice")
          val _cursorIndexOfCurrency: Int = getColumnIndexOrThrow(_cursor, "currency")
          val _cursorIndexOfStatus: Int = getColumnIndexOrThrow(_cursor, "status")
          val _cursorIndexOfPaymentStatus: Int = getColumnIndexOrThrow(_cursor, "paymentStatus")
          val _cursorIndexOfSpecialRequests: Int = getColumnIndexOrThrow(_cursor, "specialRequests")
          val _cursorIndexOfHostId: Int = getColumnIndexOrThrow(_cursor, "hostId")
          val _cursorIndexOfHostName: Int = getColumnIndexOrThrow(_cursor, "hostName")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: MutableList<BookingEntity> = ArrayList<BookingEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: BookingEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpUserId: String
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId)
            val _tmpPropertyId: String
            _tmpPropertyId = _cursor.getString(_cursorIndexOfPropertyId)
            val _tmpPropertyName: String
            _tmpPropertyName = _cursor.getString(_cursorIndexOfPropertyName)
            val _tmpPropertyImage: String?
            if (_cursor.isNull(_cursorIndexOfPropertyImage)) {
              _tmpPropertyImage = null
            } else {
              _tmpPropertyImage = _cursor.getString(_cursorIndexOfPropertyImage)
            }
            val _tmpStartDate: String
            _tmpStartDate = _cursor.getString(_cursorIndexOfStartDate)
            val _tmpEndDate: String
            _tmpEndDate = _cursor.getString(_cursorIndexOfEndDate)
            val _tmpStartTime: String?
            if (_cursor.isNull(_cursorIndexOfStartTime)) {
              _tmpStartTime = null
            } else {
              _tmpStartTime = _cursor.getString(_cursorIndexOfStartTime)
            }
            val _tmpEndTime: String?
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null
            } else {
              _tmpEndTime = _cursor.getString(_cursorIndexOfEndTime)
            }
            val _tmpTotalPrice: Double
            _tmpTotalPrice = _cursor.getDouble(_cursorIndexOfTotalPrice)
            val _tmpCurrency: String
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency)
            val _tmpStatus: String
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus)
            val _tmpPaymentStatus: String
            _tmpPaymentStatus = _cursor.getString(_cursorIndexOfPaymentStatus)
            val _tmpSpecialRequests: String?
            if (_cursor.isNull(_cursorIndexOfSpecialRequests)) {
              _tmpSpecialRequests = null
            } else {
              _tmpSpecialRequests = _cursor.getString(_cursorIndexOfSpecialRequests)
            }
            val _tmpHostId: String
            _tmpHostId = _cursor.getString(_cursorIndexOfHostId)
            val _tmpHostName: String
            _tmpHostName = _cursor.getString(_cursorIndexOfHostName)
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            _item =
                BookingEntity(_tmpId,_tmpUserId,_tmpPropertyId,_tmpPropertyName,_tmpPropertyImage,_tmpStartDate,_tmpEndDate,_tmpStartTime,_tmpEndTime,_tmpTotalPrice,_tmpCurrency,_tmpStatus,_tmpPaymentStatus,_tmpSpecialRequests,_tmpHostId,_tmpHostName,_tmpCreatedAt,_tmpUpdatedAt)
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

  public override fun getBookingsByUserAndStatus(userId: String, status: String):
      Flow<List<BookingEntity>> {
    val _sql: String =
        "SELECT * FROM bookings WHERE userId = ? AND status = ? ORDER BY createdAt DESC"
    val _statement: RoomSQLiteQuery = acquire(_sql, 2)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, userId)
    _argIndex = 2
    _statement.bindString(_argIndex, status)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("bookings"), object :
        Callable<List<BookingEntity>> {
      public override fun call(): List<BookingEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfUserId: Int = getColumnIndexOrThrow(_cursor, "userId")
          val _cursorIndexOfPropertyId: Int = getColumnIndexOrThrow(_cursor, "propertyId")
          val _cursorIndexOfPropertyName: Int = getColumnIndexOrThrow(_cursor, "propertyName")
          val _cursorIndexOfPropertyImage: Int = getColumnIndexOrThrow(_cursor, "propertyImage")
          val _cursorIndexOfStartDate: Int = getColumnIndexOrThrow(_cursor, "startDate")
          val _cursorIndexOfEndDate: Int = getColumnIndexOrThrow(_cursor, "endDate")
          val _cursorIndexOfStartTime: Int = getColumnIndexOrThrow(_cursor, "startTime")
          val _cursorIndexOfEndTime: Int = getColumnIndexOrThrow(_cursor, "endTime")
          val _cursorIndexOfTotalPrice: Int = getColumnIndexOrThrow(_cursor, "totalPrice")
          val _cursorIndexOfCurrency: Int = getColumnIndexOrThrow(_cursor, "currency")
          val _cursorIndexOfStatus: Int = getColumnIndexOrThrow(_cursor, "status")
          val _cursorIndexOfPaymentStatus: Int = getColumnIndexOrThrow(_cursor, "paymentStatus")
          val _cursorIndexOfSpecialRequests: Int = getColumnIndexOrThrow(_cursor, "specialRequests")
          val _cursorIndexOfHostId: Int = getColumnIndexOrThrow(_cursor, "hostId")
          val _cursorIndexOfHostName: Int = getColumnIndexOrThrow(_cursor, "hostName")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: MutableList<BookingEntity> = ArrayList<BookingEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: BookingEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpUserId: String
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId)
            val _tmpPropertyId: String
            _tmpPropertyId = _cursor.getString(_cursorIndexOfPropertyId)
            val _tmpPropertyName: String
            _tmpPropertyName = _cursor.getString(_cursorIndexOfPropertyName)
            val _tmpPropertyImage: String?
            if (_cursor.isNull(_cursorIndexOfPropertyImage)) {
              _tmpPropertyImage = null
            } else {
              _tmpPropertyImage = _cursor.getString(_cursorIndexOfPropertyImage)
            }
            val _tmpStartDate: String
            _tmpStartDate = _cursor.getString(_cursorIndexOfStartDate)
            val _tmpEndDate: String
            _tmpEndDate = _cursor.getString(_cursorIndexOfEndDate)
            val _tmpStartTime: String?
            if (_cursor.isNull(_cursorIndexOfStartTime)) {
              _tmpStartTime = null
            } else {
              _tmpStartTime = _cursor.getString(_cursorIndexOfStartTime)
            }
            val _tmpEndTime: String?
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null
            } else {
              _tmpEndTime = _cursor.getString(_cursorIndexOfEndTime)
            }
            val _tmpTotalPrice: Double
            _tmpTotalPrice = _cursor.getDouble(_cursorIndexOfTotalPrice)
            val _tmpCurrency: String
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency)
            val _tmpStatus: String
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus)
            val _tmpPaymentStatus: String
            _tmpPaymentStatus = _cursor.getString(_cursorIndexOfPaymentStatus)
            val _tmpSpecialRequests: String?
            if (_cursor.isNull(_cursorIndexOfSpecialRequests)) {
              _tmpSpecialRequests = null
            } else {
              _tmpSpecialRequests = _cursor.getString(_cursorIndexOfSpecialRequests)
            }
            val _tmpHostId: String
            _tmpHostId = _cursor.getString(_cursorIndexOfHostId)
            val _tmpHostName: String
            _tmpHostName = _cursor.getString(_cursorIndexOfHostName)
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            _item =
                BookingEntity(_tmpId,_tmpUserId,_tmpPropertyId,_tmpPropertyName,_tmpPropertyImage,_tmpStartDate,_tmpEndDate,_tmpStartTime,_tmpEndTime,_tmpTotalPrice,_tmpCurrency,_tmpStatus,_tmpPaymentStatus,_tmpSpecialRequests,_tmpHostId,_tmpHostName,_tmpCreatedAt,_tmpUpdatedAt)
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

  public override fun getBookingsInDateRange(startDate: String, endDate: String):
      Flow<List<BookingEntity>> {
    val _sql: String = "SELECT * FROM bookings WHERE startDate >= ? AND endDate <= ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 2)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, startDate)
    _argIndex = 2
    _statement.bindString(_argIndex, endDate)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("bookings"), object :
        Callable<List<BookingEntity>> {
      public override fun call(): List<BookingEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfUserId: Int = getColumnIndexOrThrow(_cursor, "userId")
          val _cursorIndexOfPropertyId: Int = getColumnIndexOrThrow(_cursor, "propertyId")
          val _cursorIndexOfPropertyName: Int = getColumnIndexOrThrow(_cursor, "propertyName")
          val _cursorIndexOfPropertyImage: Int = getColumnIndexOrThrow(_cursor, "propertyImage")
          val _cursorIndexOfStartDate: Int = getColumnIndexOrThrow(_cursor, "startDate")
          val _cursorIndexOfEndDate: Int = getColumnIndexOrThrow(_cursor, "endDate")
          val _cursorIndexOfStartTime: Int = getColumnIndexOrThrow(_cursor, "startTime")
          val _cursorIndexOfEndTime: Int = getColumnIndexOrThrow(_cursor, "endTime")
          val _cursorIndexOfTotalPrice: Int = getColumnIndexOrThrow(_cursor, "totalPrice")
          val _cursorIndexOfCurrency: Int = getColumnIndexOrThrow(_cursor, "currency")
          val _cursorIndexOfStatus: Int = getColumnIndexOrThrow(_cursor, "status")
          val _cursorIndexOfPaymentStatus: Int = getColumnIndexOrThrow(_cursor, "paymentStatus")
          val _cursorIndexOfSpecialRequests: Int = getColumnIndexOrThrow(_cursor, "specialRequests")
          val _cursorIndexOfHostId: Int = getColumnIndexOrThrow(_cursor, "hostId")
          val _cursorIndexOfHostName: Int = getColumnIndexOrThrow(_cursor, "hostName")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: MutableList<BookingEntity> = ArrayList<BookingEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: BookingEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpUserId: String
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId)
            val _tmpPropertyId: String
            _tmpPropertyId = _cursor.getString(_cursorIndexOfPropertyId)
            val _tmpPropertyName: String
            _tmpPropertyName = _cursor.getString(_cursorIndexOfPropertyName)
            val _tmpPropertyImage: String?
            if (_cursor.isNull(_cursorIndexOfPropertyImage)) {
              _tmpPropertyImage = null
            } else {
              _tmpPropertyImage = _cursor.getString(_cursorIndexOfPropertyImage)
            }
            val _tmpStartDate: String
            _tmpStartDate = _cursor.getString(_cursorIndexOfStartDate)
            val _tmpEndDate: String
            _tmpEndDate = _cursor.getString(_cursorIndexOfEndDate)
            val _tmpStartTime: String?
            if (_cursor.isNull(_cursorIndexOfStartTime)) {
              _tmpStartTime = null
            } else {
              _tmpStartTime = _cursor.getString(_cursorIndexOfStartTime)
            }
            val _tmpEndTime: String?
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null
            } else {
              _tmpEndTime = _cursor.getString(_cursorIndexOfEndTime)
            }
            val _tmpTotalPrice: Double
            _tmpTotalPrice = _cursor.getDouble(_cursorIndexOfTotalPrice)
            val _tmpCurrency: String
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency)
            val _tmpStatus: String
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus)
            val _tmpPaymentStatus: String
            _tmpPaymentStatus = _cursor.getString(_cursorIndexOfPaymentStatus)
            val _tmpSpecialRequests: String?
            if (_cursor.isNull(_cursorIndexOfSpecialRequests)) {
              _tmpSpecialRequests = null
            } else {
              _tmpSpecialRequests = _cursor.getString(_cursorIndexOfSpecialRequests)
            }
            val _tmpHostId: String
            _tmpHostId = _cursor.getString(_cursorIndexOfHostId)
            val _tmpHostName: String
            _tmpHostName = _cursor.getString(_cursorIndexOfHostName)
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            _item =
                BookingEntity(_tmpId,_tmpUserId,_tmpPropertyId,_tmpPropertyName,_tmpPropertyImage,_tmpStartDate,_tmpEndDate,_tmpStartTime,_tmpEndTime,_tmpTotalPrice,_tmpCurrency,_tmpStatus,_tmpPaymentStatus,_tmpSpecialRequests,_tmpHostId,_tmpHostName,_tmpCreatedAt,_tmpUpdatedAt)
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

  public override fun getAllBookings(): Flow<List<BookingEntity>> {
    val _sql: String = "SELECT * FROM bookings ORDER BY createdAt DESC"
    val _statement: RoomSQLiteQuery = acquire(_sql, 0)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("bookings"), object :
        Callable<List<BookingEntity>> {
      public override fun call(): List<BookingEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfUserId: Int = getColumnIndexOrThrow(_cursor, "userId")
          val _cursorIndexOfPropertyId: Int = getColumnIndexOrThrow(_cursor, "propertyId")
          val _cursorIndexOfPropertyName: Int = getColumnIndexOrThrow(_cursor, "propertyName")
          val _cursorIndexOfPropertyImage: Int = getColumnIndexOrThrow(_cursor, "propertyImage")
          val _cursorIndexOfStartDate: Int = getColumnIndexOrThrow(_cursor, "startDate")
          val _cursorIndexOfEndDate: Int = getColumnIndexOrThrow(_cursor, "endDate")
          val _cursorIndexOfStartTime: Int = getColumnIndexOrThrow(_cursor, "startTime")
          val _cursorIndexOfEndTime: Int = getColumnIndexOrThrow(_cursor, "endTime")
          val _cursorIndexOfTotalPrice: Int = getColumnIndexOrThrow(_cursor, "totalPrice")
          val _cursorIndexOfCurrency: Int = getColumnIndexOrThrow(_cursor, "currency")
          val _cursorIndexOfStatus: Int = getColumnIndexOrThrow(_cursor, "status")
          val _cursorIndexOfPaymentStatus: Int = getColumnIndexOrThrow(_cursor, "paymentStatus")
          val _cursorIndexOfSpecialRequests: Int = getColumnIndexOrThrow(_cursor, "specialRequests")
          val _cursorIndexOfHostId: Int = getColumnIndexOrThrow(_cursor, "hostId")
          val _cursorIndexOfHostName: Int = getColumnIndexOrThrow(_cursor, "hostName")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: MutableList<BookingEntity> = ArrayList<BookingEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: BookingEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpUserId: String
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId)
            val _tmpPropertyId: String
            _tmpPropertyId = _cursor.getString(_cursorIndexOfPropertyId)
            val _tmpPropertyName: String
            _tmpPropertyName = _cursor.getString(_cursorIndexOfPropertyName)
            val _tmpPropertyImage: String?
            if (_cursor.isNull(_cursorIndexOfPropertyImage)) {
              _tmpPropertyImage = null
            } else {
              _tmpPropertyImage = _cursor.getString(_cursorIndexOfPropertyImage)
            }
            val _tmpStartDate: String
            _tmpStartDate = _cursor.getString(_cursorIndexOfStartDate)
            val _tmpEndDate: String
            _tmpEndDate = _cursor.getString(_cursorIndexOfEndDate)
            val _tmpStartTime: String?
            if (_cursor.isNull(_cursorIndexOfStartTime)) {
              _tmpStartTime = null
            } else {
              _tmpStartTime = _cursor.getString(_cursorIndexOfStartTime)
            }
            val _tmpEndTime: String?
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null
            } else {
              _tmpEndTime = _cursor.getString(_cursorIndexOfEndTime)
            }
            val _tmpTotalPrice: Double
            _tmpTotalPrice = _cursor.getDouble(_cursorIndexOfTotalPrice)
            val _tmpCurrency: String
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency)
            val _tmpStatus: String
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus)
            val _tmpPaymentStatus: String
            _tmpPaymentStatus = _cursor.getString(_cursorIndexOfPaymentStatus)
            val _tmpSpecialRequests: String?
            if (_cursor.isNull(_cursorIndexOfSpecialRequests)) {
              _tmpSpecialRequests = null
            } else {
              _tmpSpecialRequests = _cursor.getString(_cursorIndexOfSpecialRequests)
            }
            val _tmpHostId: String
            _tmpHostId = _cursor.getString(_cursorIndexOfHostId)
            val _tmpHostName: String
            _tmpHostName = _cursor.getString(_cursorIndexOfHostName)
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            _item =
                BookingEntity(_tmpId,_tmpUserId,_tmpPropertyId,_tmpPropertyName,_tmpPropertyImage,_tmpStartDate,_tmpEndDate,_tmpStartTime,_tmpEndTime,_tmpTotalPrice,_tmpCurrency,_tmpStatus,_tmpPaymentStatus,_tmpSpecialRequests,_tmpHostId,_tmpHostName,_tmpCreatedAt,_tmpUpdatedAt)
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
