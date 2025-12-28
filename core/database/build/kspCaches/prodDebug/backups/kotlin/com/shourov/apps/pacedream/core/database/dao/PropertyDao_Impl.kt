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
import com.shourov.apps.pacedream.core.database.entity.PropertyEntity
import java.lang.Class
import java.util.ArrayList
import java.util.concurrent.Callable
import javax.`annotation`.processing.Generated
import kotlin.Boolean
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
public class PropertyDao_Impl(
  __db: RoomDatabase,
) : PropertyDao {
  private val __db: RoomDatabase

  private val __insertionAdapterOfPropertyEntity: EntityInsertionAdapter<PropertyEntity>

  private val __deletionAdapterOfPropertyEntity: EntityDeletionOrUpdateAdapter<PropertyEntity>

  private val __updateAdapterOfPropertyEntity: EntityDeletionOrUpdateAdapter<PropertyEntity>

  private val __preparedStmtOfDeletePropertyById: SharedSQLiteStatement

  private val __preparedStmtOfDeleteAllProperties: SharedSQLiteStatement
  init {
    this.__db = __db
    this.__insertionAdapterOfPropertyEntity = object : EntityInsertionAdapter<PropertyEntity>(__db)
        {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `properties` (`id`,`name`,`description`,`propertyType`,`location`,`images`,`amenities`,`rating`,`reviewCount`,`basePrice`,`currency`,`isAvailable`,`hostId`,`hostName`,`hostAvatar`,`additionalDetails`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: PropertyEntity) {
        statement.bindString(1, entity.id)
        statement.bindString(2, entity.name)
        val _tmpDescription: String? = entity.description
        if (_tmpDescription == null) {
          statement.bindNull(3)
        } else {
          statement.bindString(3, _tmpDescription)
        }
        statement.bindString(4, entity.propertyType)
        statement.bindString(5, entity.location)
        statement.bindString(6, entity.images)
        statement.bindString(7, entity.amenities)
        val _tmpRating: Double? = entity.rating
        if (_tmpRating == null) {
          statement.bindNull(8)
        } else {
          statement.bindDouble(8, _tmpRating)
        }
        val _tmpReviewCount: Int? = entity.reviewCount
        if (_tmpReviewCount == null) {
          statement.bindNull(9)
        } else {
          statement.bindLong(9, _tmpReviewCount.toLong())
        }
        statement.bindDouble(10, entity.basePrice)
        statement.bindString(11, entity.currency)
        val _tmp: Int = if (entity.isAvailable) 1 else 0
        statement.bindLong(12, _tmp.toLong())
        statement.bindString(13, entity.hostId)
        statement.bindString(14, entity.hostName)
        val _tmpHostAvatar: String? = entity.hostAvatar
        if (_tmpHostAvatar == null) {
          statement.bindNull(15)
        } else {
          statement.bindString(15, _tmpHostAvatar)
        }
        val _tmpAdditionalDetails: String? = entity.additionalDetails
        if (_tmpAdditionalDetails == null) {
          statement.bindNull(16)
        } else {
          statement.bindString(16, _tmpAdditionalDetails)
        }
        statement.bindString(17, entity.createdAt)
        statement.bindString(18, entity.updatedAt)
      }
    }
    this.__deletionAdapterOfPropertyEntity = object :
        EntityDeletionOrUpdateAdapter<PropertyEntity>(__db) {
      protected override fun createQuery(): String = "DELETE FROM `properties` WHERE `id` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: PropertyEntity) {
        statement.bindString(1, entity.id)
      }
    }
    this.__updateAdapterOfPropertyEntity = object :
        EntityDeletionOrUpdateAdapter<PropertyEntity>(__db) {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `properties` SET `id` = ?,`name` = ?,`description` = ?,`propertyType` = ?,`location` = ?,`images` = ?,`amenities` = ?,`rating` = ?,`reviewCount` = ?,`basePrice` = ?,`currency` = ?,`isAvailable` = ?,`hostId` = ?,`hostName` = ?,`hostAvatar` = ?,`additionalDetails` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: PropertyEntity) {
        statement.bindString(1, entity.id)
        statement.bindString(2, entity.name)
        val _tmpDescription: String? = entity.description
        if (_tmpDescription == null) {
          statement.bindNull(3)
        } else {
          statement.bindString(3, _tmpDescription)
        }
        statement.bindString(4, entity.propertyType)
        statement.bindString(5, entity.location)
        statement.bindString(6, entity.images)
        statement.bindString(7, entity.amenities)
        val _tmpRating: Double? = entity.rating
        if (_tmpRating == null) {
          statement.bindNull(8)
        } else {
          statement.bindDouble(8, _tmpRating)
        }
        val _tmpReviewCount: Int? = entity.reviewCount
        if (_tmpReviewCount == null) {
          statement.bindNull(9)
        } else {
          statement.bindLong(9, _tmpReviewCount.toLong())
        }
        statement.bindDouble(10, entity.basePrice)
        statement.bindString(11, entity.currency)
        val _tmp: Int = if (entity.isAvailable) 1 else 0
        statement.bindLong(12, _tmp.toLong())
        statement.bindString(13, entity.hostId)
        statement.bindString(14, entity.hostName)
        val _tmpHostAvatar: String? = entity.hostAvatar
        if (_tmpHostAvatar == null) {
          statement.bindNull(15)
        } else {
          statement.bindString(15, _tmpHostAvatar)
        }
        val _tmpAdditionalDetails: String? = entity.additionalDetails
        if (_tmpAdditionalDetails == null) {
          statement.bindNull(16)
        } else {
          statement.bindString(16, _tmpAdditionalDetails)
        }
        statement.bindString(17, entity.createdAt)
        statement.bindString(18, entity.updatedAt)
        statement.bindString(19, entity.id)
      }
    }
    this.__preparedStmtOfDeletePropertyById = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "DELETE FROM properties WHERE id = ?"
        return _query
      }
    }
    this.__preparedStmtOfDeleteAllProperties = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "DELETE FROM properties"
        return _query
      }
    }
  }

  public override suspend fun insertProperty(`property`: PropertyEntity): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __insertionAdapterOfPropertyEntity.insert(property)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun insertProperties(properties: List<PropertyEntity>): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __insertionAdapterOfPropertyEntity.insert(properties)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun deleteProperty(`property`: PropertyEntity): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __deletionAdapterOfPropertyEntity.handle(property)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun updateProperty(`property`: PropertyEntity): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __updateAdapterOfPropertyEntity.handle(property)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun deletePropertyById(propertyId: String): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfDeletePropertyById.acquire()
      var _argIndex: Int = 1
      _stmt.bindString(_argIndex, propertyId)
      try {
        __db.beginTransaction()
        try {
          _stmt.executeUpdateDelete()
          __db.setTransactionSuccessful()
        } finally {
          __db.endTransaction()
        }
      } finally {
        __preparedStmtOfDeletePropertyById.release(_stmt)
      }
    }
  })

  public override suspend fun deleteAllProperties(): Unit = CoroutinesRoom.execute(__db, true,
      object : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfDeleteAllProperties.acquire()
      try {
        __db.beginTransaction()
        try {
          _stmt.executeUpdateDelete()
          __db.setTransactionSuccessful()
        } finally {
          __db.endTransaction()
        }
      } finally {
        __preparedStmtOfDeleteAllProperties.release(_stmt)
      }
    }
  })

  public override fun getPropertyById(propertyId: String): Flow<PropertyEntity?> {
    val _sql: String = "SELECT * FROM properties WHERE id = ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, propertyId)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("properties"), object :
        Callable<PropertyEntity?> {
      public override fun call(): PropertyEntity? {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfName: Int = getColumnIndexOrThrow(_cursor, "name")
          val _cursorIndexOfDescription: Int = getColumnIndexOrThrow(_cursor, "description")
          val _cursorIndexOfPropertyType: Int = getColumnIndexOrThrow(_cursor, "propertyType")
          val _cursorIndexOfLocation: Int = getColumnIndexOrThrow(_cursor, "location")
          val _cursorIndexOfImages: Int = getColumnIndexOrThrow(_cursor, "images")
          val _cursorIndexOfAmenities: Int = getColumnIndexOrThrow(_cursor, "amenities")
          val _cursorIndexOfRating: Int = getColumnIndexOrThrow(_cursor, "rating")
          val _cursorIndexOfReviewCount: Int = getColumnIndexOrThrow(_cursor, "reviewCount")
          val _cursorIndexOfBasePrice: Int = getColumnIndexOrThrow(_cursor, "basePrice")
          val _cursorIndexOfCurrency: Int = getColumnIndexOrThrow(_cursor, "currency")
          val _cursorIndexOfIsAvailable: Int = getColumnIndexOrThrow(_cursor, "isAvailable")
          val _cursorIndexOfHostId: Int = getColumnIndexOrThrow(_cursor, "hostId")
          val _cursorIndexOfHostName: Int = getColumnIndexOrThrow(_cursor, "hostName")
          val _cursorIndexOfHostAvatar: Int = getColumnIndexOrThrow(_cursor, "hostAvatar")
          val _cursorIndexOfAdditionalDetails: Int = getColumnIndexOrThrow(_cursor,
              "additionalDetails")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: PropertyEntity?
          if (_cursor.moveToFirst()) {
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpName: String
            _tmpName = _cursor.getString(_cursorIndexOfName)
            val _tmpDescription: String?
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription)
            }
            val _tmpPropertyType: String
            _tmpPropertyType = _cursor.getString(_cursorIndexOfPropertyType)
            val _tmpLocation: String
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation)
            val _tmpImages: String
            _tmpImages = _cursor.getString(_cursorIndexOfImages)
            val _tmpAmenities: String
            _tmpAmenities = _cursor.getString(_cursorIndexOfAmenities)
            val _tmpRating: Double?
            if (_cursor.isNull(_cursorIndexOfRating)) {
              _tmpRating = null
            } else {
              _tmpRating = _cursor.getDouble(_cursorIndexOfRating)
            }
            val _tmpReviewCount: Int?
            if (_cursor.isNull(_cursorIndexOfReviewCount)) {
              _tmpReviewCount = null
            } else {
              _tmpReviewCount = _cursor.getInt(_cursorIndexOfReviewCount)
            }
            val _tmpBasePrice: Double
            _tmpBasePrice = _cursor.getDouble(_cursorIndexOfBasePrice)
            val _tmpCurrency: String
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency)
            val _tmpIsAvailable: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsAvailable)
            _tmpIsAvailable = _tmp != 0
            val _tmpHostId: String
            _tmpHostId = _cursor.getString(_cursorIndexOfHostId)
            val _tmpHostName: String
            _tmpHostName = _cursor.getString(_cursorIndexOfHostName)
            val _tmpHostAvatar: String?
            if (_cursor.isNull(_cursorIndexOfHostAvatar)) {
              _tmpHostAvatar = null
            } else {
              _tmpHostAvatar = _cursor.getString(_cursorIndexOfHostAvatar)
            }
            val _tmpAdditionalDetails: String?
            if (_cursor.isNull(_cursorIndexOfAdditionalDetails)) {
              _tmpAdditionalDetails = null
            } else {
              _tmpAdditionalDetails = _cursor.getString(_cursorIndexOfAdditionalDetails)
            }
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            _result =
                PropertyEntity(_tmpId,_tmpName,_tmpDescription,_tmpPropertyType,_tmpLocation,_tmpImages,_tmpAmenities,_tmpRating,_tmpReviewCount,_tmpBasePrice,_tmpCurrency,_tmpIsAvailable,_tmpHostId,_tmpHostName,_tmpHostAvatar,_tmpAdditionalDetails,_tmpCreatedAt,_tmpUpdatedAt)
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

  public override fun getPropertiesByType(propertyType: String): Flow<List<PropertyEntity>> {
    val _sql: String = "SELECT * FROM properties WHERE propertyType = ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, propertyType)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("properties"), object :
        Callable<List<PropertyEntity>> {
      public override fun call(): List<PropertyEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfName: Int = getColumnIndexOrThrow(_cursor, "name")
          val _cursorIndexOfDescription: Int = getColumnIndexOrThrow(_cursor, "description")
          val _cursorIndexOfPropertyType: Int = getColumnIndexOrThrow(_cursor, "propertyType")
          val _cursorIndexOfLocation: Int = getColumnIndexOrThrow(_cursor, "location")
          val _cursorIndexOfImages: Int = getColumnIndexOrThrow(_cursor, "images")
          val _cursorIndexOfAmenities: Int = getColumnIndexOrThrow(_cursor, "amenities")
          val _cursorIndexOfRating: Int = getColumnIndexOrThrow(_cursor, "rating")
          val _cursorIndexOfReviewCount: Int = getColumnIndexOrThrow(_cursor, "reviewCount")
          val _cursorIndexOfBasePrice: Int = getColumnIndexOrThrow(_cursor, "basePrice")
          val _cursorIndexOfCurrency: Int = getColumnIndexOrThrow(_cursor, "currency")
          val _cursorIndexOfIsAvailable: Int = getColumnIndexOrThrow(_cursor, "isAvailable")
          val _cursorIndexOfHostId: Int = getColumnIndexOrThrow(_cursor, "hostId")
          val _cursorIndexOfHostName: Int = getColumnIndexOrThrow(_cursor, "hostName")
          val _cursorIndexOfHostAvatar: Int = getColumnIndexOrThrow(_cursor, "hostAvatar")
          val _cursorIndexOfAdditionalDetails: Int = getColumnIndexOrThrow(_cursor,
              "additionalDetails")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: MutableList<PropertyEntity> = ArrayList<PropertyEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: PropertyEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpName: String
            _tmpName = _cursor.getString(_cursorIndexOfName)
            val _tmpDescription: String?
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription)
            }
            val _tmpPropertyType: String
            _tmpPropertyType = _cursor.getString(_cursorIndexOfPropertyType)
            val _tmpLocation: String
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation)
            val _tmpImages: String
            _tmpImages = _cursor.getString(_cursorIndexOfImages)
            val _tmpAmenities: String
            _tmpAmenities = _cursor.getString(_cursorIndexOfAmenities)
            val _tmpRating: Double?
            if (_cursor.isNull(_cursorIndexOfRating)) {
              _tmpRating = null
            } else {
              _tmpRating = _cursor.getDouble(_cursorIndexOfRating)
            }
            val _tmpReviewCount: Int?
            if (_cursor.isNull(_cursorIndexOfReviewCount)) {
              _tmpReviewCount = null
            } else {
              _tmpReviewCount = _cursor.getInt(_cursorIndexOfReviewCount)
            }
            val _tmpBasePrice: Double
            _tmpBasePrice = _cursor.getDouble(_cursorIndexOfBasePrice)
            val _tmpCurrency: String
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency)
            val _tmpIsAvailable: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsAvailable)
            _tmpIsAvailable = _tmp != 0
            val _tmpHostId: String
            _tmpHostId = _cursor.getString(_cursorIndexOfHostId)
            val _tmpHostName: String
            _tmpHostName = _cursor.getString(_cursorIndexOfHostName)
            val _tmpHostAvatar: String?
            if (_cursor.isNull(_cursorIndexOfHostAvatar)) {
              _tmpHostAvatar = null
            } else {
              _tmpHostAvatar = _cursor.getString(_cursorIndexOfHostAvatar)
            }
            val _tmpAdditionalDetails: String?
            if (_cursor.isNull(_cursorIndexOfAdditionalDetails)) {
              _tmpAdditionalDetails = null
            } else {
              _tmpAdditionalDetails = _cursor.getString(_cursorIndexOfAdditionalDetails)
            }
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            _item =
                PropertyEntity(_tmpId,_tmpName,_tmpDescription,_tmpPropertyType,_tmpLocation,_tmpImages,_tmpAmenities,_tmpRating,_tmpReviewCount,_tmpBasePrice,_tmpCurrency,_tmpIsAvailable,_tmpHostId,_tmpHostName,_tmpHostAvatar,_tmpAdditionalDetails,_tmpCreatedAt,_tmpUpdatedAt)
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

  public override fun getAvailableProperties(): Flow<List<PropertyEntity>> {
    val _sql: String = "SELECT * FROM properties WHERE isAvailable = 1"
    val _statement: RoomSQLiteQuery = acquire(_sql, 0)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("properties"), object :
        Callable<List<PropertyEntity>> {
      public override fun call(): List<PropertyEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfName: Int = getColumnIndexOrThrow(_cursor, "name")
          val _cursorIndexOfDescription: Int = getColumnIndexOrThrow(_cursor, "description")
          val _cursorIndexOfPropertyType: Int = getColumnIndexOrThrow(_cursor, "propertyType")
          val _cursorIndexOfLocation: Int = getColumnIndexOrThrow(_cursor, "location")
          val _cursorIndexOfImages: Int = getColumnIndexOrThrow(_cursor, "images")
          val _cursorIndexOfAmenities: Int = getColumnIndexOrThrow(_cursor, "amenities")
          val _cursorIndexOfRating: Int = getColumnIndexOrThrow(_cursor, "rating")
          val _cursorIndexOfReviewCount: Int = getColumnIndexOrThrow(_cursor, "reviewCount")
          val _cursorIndexOfBasePrice: Int = getColumnIndexOrThrow(_cursor, "basePrice")
          val _cursorIndexOfCurrency: Int = getColumnIndexOrThrow(_cursor, "currency")
          val _cursorIndexOfIsAvailable: Int = getColumnIndexOrThrow(_cursor, "isAvailable")
          val _cursorIndexOfHostId: Int = getColumnIndexOrThrow(_cursor, "hostId")
          val _cursorIndexOfHostName: Int = getColumnIndexOrThrow(_cursor, "hostName")
          val _cursorIndexOfHostAvatar: Int = getColumnIndexOrThrow(_cursor, "hostAvatar")
          val _cursorIndexOfAdditionalDetails: Int = getColumnIndexOrThrow(_cursor,
              "additionalDetails")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: MutableList<PropertyEntity> = ArrayList<PropertyEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: PropertyEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpName: String
            _tmpName = _cursor.getString(_cursorIndexOfName)
            val _tmpDescription: String?
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription)
            }
            val _tmpPropertyType: String
            _tmpPropertyType = _cursor.getString(_cursorIndexOfPropertyType)
            val _tmpLocation: String
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation)
            val _tmpImages: String
            _tmpImages = _cursor.getString(_cursorIndexOfImages)
            val _tmpAmenities: String
            _tmpAmenities = _cursor.getString(_cursorIndexOfAmenities)
            val _tmpRating: Double?
            if (_cursor.isNull(_cursorIndexOfRating)) {
              _tmpRating = null
            } else {
              _tmpRating = _cursor.getDouble(_cursorIndexOfRating)
            }
            val _tmpReviewCount: Int?
            if (_cursor.isNull(_cursorIndexOfReviewCount)) {
              _tmpReviewCount = null
            } else {
              _tmpReviewCount = _cursor.getInt(_cursorIndexOfReviewCount)
            }
            val _tmpBasePrice: Double
            _tmpBasePrice = _cursor.getDouble(_cursorIndexOfBasePrice)
            val _tmpCurrency: String
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency)
            val _tmpIsAvailable: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsAvailable)
            _tmpIsAvailable = _tmp != 0
            val _tmpHostId: String
            _tmpHostId = _cursor.getString(_cursorIndexOfHostId)
            val _tmpHostName: String
            _tmpHostName = _cursor.getString(_cursorIndexOfHostName)
            val _tmpHostAvatar: String?
            if (_cursor.isNull(_cursorIndexOfHostAvatar)) {
              _tmpHostAvatar = null
            } else {
              _tmpHostAvatar = _cursor.getString(_cursorIndexOfHostAvatar)
            }
            val _tmpAdditionalDetails: String?
            if (_cursor.isNull(_cursorIndexOfAdditionalDetails)) {
              _tmpAdditionalDetails = null
            } else {
              _tmpAdditionalDetails = _cursor.getString(_cursorIndexOfAdditionalDetails)
            }
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            _item =
                PropertyEntity(_tmpId,_tmpName,_tmpDescription,_tmpPropertyType,_tmpLocation,_tmpImages,_tmpAmenities,_tmpRating,_tmpReviewCount,_tmpBasePrice,_tmpCurrency,_tmpIsAvailable,_tmpHostId,_tmpHostName,_tmpHostAvatar,_tmpAdditionalDetails,_tmpCreatedAt,_tmpUpdatedAt)
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

  public override fun getPropertiesByHost(hostId: String): Flow<List<PropertyEntity>> {
    val _sql: String = "SELECT * FROM properties WHERE hostId = ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, hostId)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("properties"), object :
        Callable<List<PropertyEntity>> {
      public override fun call(): List<PropertyEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfName: Int = getColumnIndexOrThrow(_cursor, "name")
          val _cursorIndexOfDescription: Int = getColumnIndexOrThrow(_cursor, "description")
          val _cursorIndexOfPropertyType: Int = getColumnIndexOrThrow(_cursor, "propertyType")
          val _cursorIndexOfLocation: Int = getColumnIndexOrThrow(_cursor, "location")
          val _cursorIndexOfImages: Int = getColumnIndexOrThrow(_cursor, "images")
          val _cursorIndexOfAmenities: Int = getColumnIndexOrThrow(_cursor, "amenities")
          val _cursorIndexOfRating: Int = getColumnIndexOrThrow(_cursor, "rating")
          val _cursorIndexOfReviewCount: Int = getColumnIndexOrThrow(_cursor, "reviewCount")
          val _cursorIndexOfBasePrice: Int = getColumnIndexOrThrow(_cursor, "basePrice")
          val _cursorIndexOfCurrency: Int = getColumnIndexOrThrow(_cursor, "currency")
          val _cursorIndexOfIsAvailable: Int = getColumnIndexOrThrow(_cursor, "isAvailable")
          val _cursorIndexOfHostId: Int = getColumnIndexOrThrow(_cursor, "hostId")
          val _cursorIndexOfHostName: Int = getColumnIndexOrThrow(_cursor, "hostName")
          val _cursorIndexOfHostAvatar: Int = getColumnIndexOrThrow(_cursor, "hostAvatar")
          val _cursorIndexOfAdditionalDetails: Int = getColumnIndexOrThrow(_cursor,
              "additionalDetails")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: MutableList<PropertyEntity> = ArrayList<PropertyEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: PropertyEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpName: String
            _tmpName = _cursor.getString(_cursorIndexOfName)
            val _tmpDescription: String?
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription)
            }
            val _tmpPropertyType: String
            _tmpPropertyType = _cursor.getString(_cursorIndexOfPropertyType)
            val _tmpLocation: String
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation)
            val _tmpImages: String
            _tmpImages = _cursor.getString(_cursorIndexOfImages)
            val _tmpAmenities: String
            _tmpAmenities = _cursor.getString(_cursorIndexOfAmenities)
            val _tmpRating: Double?
            if (_cursor.isNull(_cursorIndexOfRating)) {
              _tmpRating = null
            } else {
              _tmpRating = _cursor.getDouble(_cursorIndexOfRating)
            }
            val _tmpReviewCount: Int?
            if (_cursor.isNull(_cursorIndexOfReviewCount)) {
              _tmpReviewCount = null
            } else {
              _tmpReviewCount = _cursor.getInt(_cursorIndexOfReviewCount)
            }
            val _tmpBasePrice: Double
            _tmpBasePrice = _cursor.getDouble(_cursorIndexOfBasePrice)
            val _tmpCurrency: String
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency)
            val _tmpIsAvailable: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsAvailable)
            _tmpIsAvailable = _tmp != 0
            val _tmpHostId: String
            _tmpHostId = _cursor.getString(_cursorIndexOfHostId)
            val _tmpHostName: String
            _tmpHostName = _cursor.getString(_cursorIndexOfHostName)
            val _tmpHostAvatar: String?
            if (_cursor.isNull(_cursorIndexOfHostAvatar)) {
              _tmpHostAvatar = null
            } else {
              _tmpHostAvatar = _cursor.getString(_cursorIndexOfHostAvatar)
            }
            val _tmpAdditionalDetails: String?
            if (_cursor.isNull(_cursorIndexOfAdditionalDetails)) {
              _tmpAdditionalDetails = null
            } else {
              _tmpAdditionalDetails = _cursor.getString(_cursorIndexOfAdditionalDetails)
            }
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            _item =
                PropertyEntity(_tmpId,_tmpName,_tmpDescription,_tmpPropertyType,_tmpLocation,_tmpImages,_tmpAmenities,_tmpRating,_tmpReviewCount,_tmpBasePrice,_tmpCurrency,_tmpIsAvailable,_tmpHostId,_tmpHostName,_tmpHostAvatar,_tmpAdditionalDetails,_tmpCreatedAt,_tmpUpdatedAt)
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

  public override fun searchProperties(query: String): Flow<List<PropertyEntity>> {
    val _sql: String =
        "SELECT * FROM properties WHERE name LIKE '%' || ? || '%' OR description LIKE '%' || ? || '%'"
    val _statement: RoomSQLiteQuery = acquire(_sql, 2)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, query)
    _argIndex = 2
    _statement.bindString(_argIndex, query)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("properties"), object :
        Callable<List<PropertyEntity>> {
      public override fun call(): List<PropertyEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfName: Int = getColumnIndexOrThrow(_cursor, "name")
          val _cursorIndexOfDescription: Int = getColumnIndexOrThrow(_cursor, "description")
          val _cursorIndexOfPropertyType: Int = getColumnIndexOrThrow(_cursor, "propertyType")
          val _cursorIndexOfLocation: Int = getColumnIndexOrThrow(_cursor, "location")
          val _cursorIndexOfImages: Int = getColumnIndexOrThrow(_cursor, "images")
          val _cursorIndexOfAmenities: Int = getColumnIndexOrThrow(_cursor, "amenities")
          val _cursorIndexOfRating: Int = getColumnIndexOrThrow(_cursor, "rating")
          val _cursorIndexOfReviewCount: Int = getColumnIndexOrThrow(_cursor, "reviewCount")
          val _cursorIndexOfBasePrice: Int = getColumnIndexOrThrow(_cursor, "basePrice")
          val _cursorIndexOfCurrency: Int = getColumnIndexOrThrow(_cursor, "currency")
          val _cursorIndexOfIsAvailable: Int = getColumnIndexOrThrow(_cursor, "isAvailable")
          val _cursorIndexOfHostId: Int = getColumnIndexOrThrow(_cursor, "hostId")
          val _cursorIndexOfHostName: Int = getColumnIndexOrThrow(_cursor, "hostName")
          val _cursorIndexOfHostAvatar: Int = getColumnIndexOrThrow(_cursor, "hostAvatar")
          val _cursorIndexOfAdditionalDetails: Int = getColumnIndexOrThrow(_cursor,
              "additionalDetails")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: MutableList<PropertyEntity> = ArrayList<PropertyEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: PropertyEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpName: String
            _tmpName = _cursor.getString(_cursorIndexOfName)
            val _tmpDescription: String?
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription)
            }
            val _tmpPropertyType: String
            _tmpPropertyType = _cursor.getString(_cursorIndexOfPropertyType)
            val _tmpLocation: String
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation)
            val _tmpImages: String
            _tmpImages = _cursor.getString(_cursorIndexOfImages)
            val _tmpAmenities: String
            _tmpAmenities = _cursor.getString(_cursorIndexOfAmenities)
            val _tmpRating: Double?
            if (_cursor.isNull(_cursorIndexOfRating)) {
              _tmpRating = null
            } else {
              _tmpRating = _cursor.getDouble(_cursorIndexOfRating)
            }
            val _tmpReviewCount: Int?
            if (_cursor.isNull(_cursorIndexOfReviewCount)) {
              _tmpReviewCount = null
            } else {
              _tmpReviewCount = _cursor.getInt(_cursorIndexOfReviewCount)
            }
            val _tmpBasePrice: Double
            _tmpBasePrice = _cursor.getDouble(_cursorIndexOfBasePrice)
            val _tmpCurrency: String
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency)
            val _tmpIsAvailable: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsAvailable)
            _tmpIsAvailable = _tmp != 0
            val _tmpHostId: String
            _tmpHostId = _cursor.getString(_cursorIndexOfHostId)
            val _tmpHostName: String
            _tmpHostName = _cursor.getString(_cursorIndexOfHostName)
            val _tmpHostAvatar: String?
            if (_cursor.isNull(_cursorIndexOfHostAvatar)) {
              _tmpHostAvatar = null
            } else {
              _tmpHostAvatar = _cursor.getString(_cursorIndexOfHostAvatar)
            }
            val _tmpAdditionalDetails: String?
            if (_cursor.isNull(_cursorIndexOfAdditionalDetails)) {
              _tmpAdditionalDetails = null
            } else {
              _tmpAdditionalDetails = _cursor.getString(_cursorIndexOfAdditionalDetails)
            }
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            _item =
                PropertyEntity(_tmpId,_tmpName,_tmpDescription,_tmpPropertyType,_tmpLocation,_tmpImages,_tmpAmenities,_tmpRating,_tmpReviewCount,_tmpBasePrice,_tmpCurrency,_tmpIsAvailable,_tmpHostId,_tmpHostName,_tmpHostAvatar,_tmpAdditionalDetails,_tmpCreatedAt,_tmpUpdatedAt)
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

  public override fun getTopRatedProperties(limit: Int): Flow<List<PropertyEntity>> {
    val _sql: String = "SELECT * FROM properties ORDER BY rating DESC LIMIT ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindLong(_argIndex, limit.toLong())
    return CoroutinesRoom.createFlow(__db, false, arrayOf("properties"), object :
        Callable<List<PropertyEntity>> {
      public override fun call(): List<PropertyEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfName: Int = getColumnIndexOrThrow(_cursor, "name")
          val _cursorIndexOfDescription: Int = getColumnIndexOrThrow(_cursor, "description")
          val _cursorIndexOfPropertyType: Int = getColumnIndexOrThrow(_cursor, "propertyType")
          val _cursorIndexOfLocation: Int = getColumnIndexOrThrow(_cursor, "location")
          val _cursorIndexOfImages: Int = getColumnIndexOrThrow(_cursor, "images")
          val _cursorIndexOfAmenities: Int = getColumnIndexOrThrow(_cursor, "amenities")
          val _cursorIndexOfRating: Int = getColumnIndexOrThrow(_cursor, "rating")
          val _cursorIndexOfReviewCount: Int = getColumnIndexOrThrow(_cursor, "reviewCount")
          val _cursorIndexOfBasePrice: Int = getColumnIndexOrThrow(_cursor, "basePrice")
          val _cursorIndexOfCurrency: Int = getColumnIndexOrThrow(_cursor, "currency")
          val _cursorIndexOfIsAvailable: Int = getColumnIndexOrThrow(_cursor, "isAvailable")
          val _cursorIndexOfHostId: Int = getColumnIndexOrThrow(_cursor, "hostId")
          val _cursorIndexOfHostName: Int = getColumnIndexOrThrow(_cursor, "hostName")
          val _cursorIndexOfHostAvatar: Int = getColumnIndexOrThrow(_cursor, "hostAvatar")
          val _cursorIndexOfAdditionalDetails: Int = getColumnIndexOrThrow(_cursor,
              "additionalDetails")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: MutableList<PropertyEntity> = ArrayList<PropertyEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: PropertyEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpName: String
            _tmpName = _cursor.getString(_cursorIndexOfName)
            val _tmpDescription: String?
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription)
            }
            val _tmpPropertyType: String
            _tmpPropertyType = _cursor.getString(_cursorIndexOfPropertyType)
            val _tmpLocation: String
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation)
            val _tmpImages: String
            _tmpImages = _cursor.getString(_cursorIndexOfImages)
            val _tmpAmenities: String
            _tmpAmenities = _cursor.getString(_cursorIndexOfAmenities)
            val _tmpRating: Double?
            if (_cursor.isNull(_cursorIndexOfRating)) {
              _tmpRating = null
            } else {
              _tmpRating = _cursor.getDouble(_cursorIndexOfRating)
            }
            val _tmpReviewCount: Int?
            if (_cursor.isNull(_cursorIndexOfReviewCount)) {
              _tmpReviewCount = null
            } else {
              _tmpReviewCount = _cursor.getInt(_cursorIndexOfReviewCount)
            }
            val _tmpBasePrice: Double
            _tmpBasePrice = _cursor.getDouble(_cursorIndexOfBasePrice)
            val _tmpCurrency: String
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency)
            val _tmpIsAvailable: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsAvailable)
            _tmpIsAvailable = _tmp != 0
            val _tmpHostId: String
            _tmpHostId = _cursor.getString(_cursorIndexOfHostId)
            val _tmpHostName: String
            _tmpHostName = _cursor.getString(_cursorIndexOfHostName)
            val _tmpHostAvatar: String?
            if (_cursor.isNull(_cursorIndexOfHostAvatar)) {
              _tmpHostAvatar = null
            } else {
              _tmpHostAvatar = _cursor.getString(_cursorIndexOfHostAvatar)
            }
            val _tmpAdditionalDetails: String?
            if (_cursor.isNull(_cursorIndexOfAdditionalDetails)) {
              _tmpAdditionalDetails = null
            } else {
              _tmpAdditionalDetails = _cursor.getString(_cursorIndexOfAdditionalDetails)
            }
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            _item =
                PropertyEntity(_tmpId,_tmpName,_tmpDescription,_tmpPropertyType,_tmpLocation,_tmpImages,_tmpAmenities,_tmpRating,_tmpReviewCount,_tmpBasePrice,_tmpCurrency,_tmpIsAvailable,_tmpHostId,_tmpHostName,_tmpHostAvatar,_tmpAdditionalDetails,_tmpCreatedAt,_tmpUpdatedAt)
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

  public override fun getRecentProperties(limit: Int): Flow<List<PropertyEntity>> {
    val _sql: String = "SELECT * FROM properties ORDER BY createdAt DESC LIMIT ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindLong(_argIndex, limit.toLong())
    return CoroutinesRoom.createFlow(__db, false, arrayOf("properties"), object :
        Callable<List<PropertyEntity>> {
      public override fun call(): List<PropertyEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfName: Int = getColumnIndexOrThrow(_cursor, "name")
          val _cursorIndexOfDescription: Int = getColumnIndexOrThrow(_cursor, "description")
          val _cursorIndexOfPropertyType: Int = getColumnIndexOrThrow(_cursor, "propertyType")
          val _cursorIndexOfLocation: Int = getColumnIndexOrThrow(_cursor, "location")
          val _cursorIndexOfImages: Int = getColumnIndexOrThrow(_cursor, "images")
          val _cursorIndexOfAmenities: Int = getColumnIndexOrThrow(_cursor, "amenities")
          val _cursorIndexOfRating: Int = getColumnIndexOrThrow(_cursor, "rating")
          val _cursorIndexOfReviewCount: Int = getColumnIndexOrThrow(_cursor, "reviewCount")
          val _cursorIndexOfBasePrice: Int = getColumnIndexOrThrow(_cursor, "basePrice")
          val _cursorIndexOfCurrency: Int = getColumnIndexOrThrow(_cursor, "currency")
          val _cursorIndexOfIsAvailable: Int = getColumnIndexOrThrow(_cursor, "isAvailable")
          val _cursorIndexOfHostId: Int = getColumnIndexOrThrow(_cursor, "hostId")
          val _cursorIndexOfHostName: Int = getColumnIndexOrThrow(_cursor, "hostName")
          val _cursorIndexOfHostAvatar: Int = getColumnIndexOrThrow(_cursor, "hostAvatar")
          val _cursorIndexOfAdditionalDetails: Int = getColumnIndexOrThrow(_cursor,
              "additionalDetails")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: MutableList<PropertyEntity> = ArrayList<PropertyEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: PropertyEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpName: String
            _tmpName = _cursor.getString(_cursorIndexOfName)
            val _tmpDescription: String?
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription)
            }
            val _tmpPropertyType: String
            _tmpPropertyType = _cursor.getString(_cursorIndexOfPropertyType)
            val _tmpLocation: String
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation)
            val _tmpImages: String
            _tmpImages = _cursor.getString(_cursorIndexOfImages)
            val _tmpAmenities: String
            _tmpAmenities = _cursor.getString(_cursorIndexOfAmenities)
            val _tmpRating: Double?
            if (_cursor.isNull(_cursorIndexOfRating)) {
              _tmpRating = null
            } else {
              _tmpRating = _cursor.getDouble(_cursorIndexOfRating)
            }
            val _tmpReviewCount: Int?
            if (_cursor.isNull(_cursorIndexOfReviewCount)) {
              _tmpReviewCount = null
            } else {
              _tmpReviewCount = _cursor.getInt(_cursorIndexOfReviewCount)
            }
            val _tmpBasePrice: Double
            _tmpBasePrice = _cursor.getDouble(_cursorIndexOfBasePrice)
            val _tmpCurrency: String
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency)
            val _tmpIsAvailable: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsAvailable)
            _tmpIsAvailable = _tmp != 0
            val _tmpHostId: String
            _tmpHostId = _cursor.getString(_cursorIndexOfHostId)
            val _tmpHostName: String
            _tmpHostName = _cursor.getString(_cursorIndexOfHostName)
            val _tmpHostAvatar: String?
            if (_cursor.isNull(_cursorIndexOfHostAvatar)) {
              _tmpHostAvatar = null
            } else {
              _tmpHostAvatar = _cursor.getString(_cursorIndexOfHostAvatar)
            }
            val _tmpAdditionalDetails: String?
            if (_cursor.isNull(_cursorIndexOfAdditionalDetails)) {
              _tmpAdditionalDetails = null
            } else {
              _tmpAdditionalDetails = _cursor.getString(_cursorIndexOfAdditionalDetails)
            }
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            _item =
                PropertyEntity(_tmpId,_tmpName,_tmpDescription,_tmpPropertyType,_tmpLocation,_tmpImages,_tmpAmenities,_tmpRating,_tmpReviewCount,_tmpBasePrice,_tmpCurrency,_tmpIsAvailable,_tmpHostId,_tmpHostName,_tmpHostAvatar,_tmpAdditionalDetails,_tmpCreatedAt,_tmpUpdatedAt)
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

  public override fun getAllProperties(): Flow<List<PropertyEntity>> {
    val _sql: String = "SELECT * FROM properties"
    val _statement: RoomSQLiteQuery = acquire(_sql, 0)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("properties"), object :
        Callable<List<PropertyEntity>> {
      public override fun call(): List<PropertyEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfName: Int = getColumnIndexOrThrow(_cursor, "name")
          val _cursorIndexOfDescription: Int = getColumnIndexOrThrow(_cursor, "description")
          val _cursorIndexOfPropertyType: Int = getColumnIndexOrThrow(_cursor, "propertyType")
          val _cursorIndexOfLocation: Int = getColumnIndexOrThrow(_cursor, "location")
          val _cursorIndexOfImages: Int = getColumnIndexOrThrow(_cursor, "images")
          val _cursorIndexOfAmenities: Int = getColumnIndexOrThrow(_cursor, "amenities")
          val _cursorIndexOfRating: Int = getColumnIndexOrThrow(_cursor, "rating")
          val _cursorIndexOfReviewCount: Int = getColumnIndexOrThrow(_cursor, "reviewCount")
          val _cursorIndexOfBasePrice: Int = getColumnIndexOrThrow(_cursor, "basePrice")
          val _cursorIndexOfCurrency: Int = getColumnIndexOrThrow(_cursor, "currency")
          val _cursorIndexOfIsAvailable: Int = getColumnIndexOrThrow(_cursor, "isAvailable")
          val _cursorIndexOfHostId: Int = getColumnIndexOrThrow(_cursor, "hostId")
          val _cursorIndexOfHostName: Int = getColumnIndexOrThrow(_cursor, "hostName")
          val _cursorIndexOfHostAvatar: Int = getColumnIndexOrThrow(_cursor, "hostAvatar")
          val _cursorIndexOfAdditionalDetails: Int = getColumnIndexOrThrow(_cursor,
              "additionalDetails")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: MutableList<PropertyEntity> = ArrayList<PropertyEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: PropertyEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpName: String
            _tmpName = _cursor.getString(_cursorIndexOfName)
            val _tmpDescription: String?
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription)
            }
            val _tmpPropertyType: String
            _tmpPropertyType = _cursor.getString(_cursorIndexOfPropertyType)
            val _tmpLocation: String
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation)
            val _tmpImages: String
            _tmpImages = _cursor.getString(_cursorIndexOfImages)
            val _tmpAmenities: String
            _tmpAmenities = _cursor.getString(_cursorIndexOfAmenities)
            val _tmpRating: Double?
            if (_cursor.isNull(_cursorIndexOfRating)) {
              _tmpRating = null
            } else {
              _tmpRating = _cursor.getDouble(_cursorIndexOfRating)
            }
            val _tmpReviewCount: Int?
            if (_cursor.isNull(_cursorIndexOfReviewCount)) {
              _tmpReviewCount = null
            } else {
              _tmpReviewCount = _cursor.getInt(_cursorIndexOfReviewCount)
            }
            val _tmpBasePrice: Double
            _tmpBasePrice = _cursor.getDouble(_cursorIndexOfBasePrice)
            val _tmpCurrency: String
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency)
            val _tmpIsAvailable: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsAvailable)
            _tmpIsAvailable = _tmp != 0
            val _tmpHostId: String
            _tmpHostId = _cursor.getString(_cursorIndexOfHostId)
            val _tmpHostName: String
            _tmpHostName = _cursor.getString(_cursorIndexOfHostName)
            val _tmpHostAvatar: String?
            if (_cursor.isNull(_cursorIndexOfHostAvatar)) {
              _tmpHostAvatar = null
            } else {
              _tmpHostAvatar = _cursor.getString(_cursorIndexOfHostAvatar)
            }
            val _tmpAdditionalDetails: String?
            if (_cursor.isNull(_cursorIndexOfAdditionalDetails)) {
              _tmpAdditionalDetails = null
            } else {
              _tmpAdditionalDetails = _cursor.getString(_cursorIndexOfAdditionalDetails)
            }
            val _tmpCreatedAt: String
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: String
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt)
            _item =
                PropertyEntity(_tmpId,_tmpName,_tmpDescription,_tmpPropertyType,_tmpLocation,_tmpImages,_tmpAmenities,_tmpRating,_tmpReviewCount,_tmpBasePrice,_tmpCurrency,_tmpIsAvailable,_tmpHostId,_tmpHostName,_tmpHostAvatar,_tmpAdditionalDetails,_tmpCreatedAt,_tmpUpdatedAt)
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
