package com.shourov.apps.pacedream.core.database

import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.room.RoomOpenHelper
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.shourov.apps.pacedream.core.database.dao.BookingDao
import com.shourov.apps.pacedream.core.database.dao.BookingDao_Impl
import com.shourov.apps.pacedream.core.database.dao.CategoryDao
import com.shourov.apps.pacedream.core.database.dao.CategoryDao_Impl
import com.shourov.apps.pacedream.core.database.dao.ChatDao
import com.shourov.apps.pacedream.core.database.dao.ChatDao_Impl
import com.shourov.apps.pacedream.core.database.dao.MessageDao
import com.shourov.apps.pacedream.core.database.dao.MessageDao_Impl
import com.shourov.apps.pacedream.core.database.dao.PropertyDao
import com.shourov.apps.pacedream.core.database.dao.PropertyDao_Impl
import com.shourov.apps.pacedream.core.database.dao.UserDao
import com.shourov.apps.pacedream.core.database.dao.UserDao_Impl
import java.lang.Class
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import javax.`annotation`.processing.Generated
import kotlin.Any
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.Set

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION"])
public class PaceDreamDatabase_Impl : PaceDreamDatabase() {
  private val _userDao: Lazy<UserDao> = lazy {
    UserDao_Impl(this)
  }


  private val _propertyDao: Lazy<PropertyDao> = lazy {
    PropertyDao_Impl(this)
  }


  private val _bookingDao: Lazy<BookingDao> = lazy {
    BookingDao_Impl(this)
  }


  private val _messageDao: Lazy<MessageDao> = lazy {
    MessageDao_Impl(this)
  }


  private val _categoryDao: Lazy<CategoryDao> = lazy {
    CategoryDao_Impl(this)
  }


  private val _chatDao: Lazy<ChatDao> = lazy {
    ChatDao_Impl(this)
  }


  protected override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper {
    val _openCallback: SupportSQLiteOpenHelper.Callback = RoomOpenHelper(config, object :
        RoomOpenHelper.Delegate(1) {
      public override fun createAllTables(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` TEXT NOT NULL, `firstName` TEXT NOT NULL, `lastName` TEXT NOT NULL, `email` TEXT NOT NULL, `phone` TEXT, `profilePic` TEXT, `dateOfBirth` TEXT, `gender` TEXT, `isVerified` INTEGER NOT NULL, `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, `preferences` TEXT, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `properties` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `propertyType` TEXT NOT NULL, `location` TEXT NOT NULL, `images` TEXT NOT NULL, `amenities` TEXT NOT NULL, `rating` REAL, `reviewCount` INTEGER, `basePrice` REAL NOT NULL, `currency` TEXT NOT NULL, `isAvailable` INTEGER NOT NULL, `hostId` TEXT NOT NULL, `hostName` TEXT NOT NULL, `hostAvatar` TEXT, `additionalDetails` TEXT, `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `bookings` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `propertyId` TEXT NOT NULL, `propertyName` TEXT NOT NULL, `propertyImage` TEXT, `startDate` TEXT NOT NULL, `endDate` TEXT NOT NULL, `startTime` TEXT, `endTime` TEXT, `totalPrice` REAL NOT NULL, `currency` TEXT NOT NULL, `status` TEXT NOT NULL, `paymentStatus` TEXT NOT NULL, `specialRequests` TEXT, `hostId` TEXT NOT NULL, `hostName` TEXT NOT NULL, `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `messages` (`id` TEXT NOT NULL, `chatId` TEXT NOT NULL, `senderId` TEXT NOT NULL, `receiverId` TEXT NOT NULL, `content` TEXT NOT NULL, `messageType` TEXT NOT NULL, `attachmentUrl` TEXT, `isRead` INTEGER NOT NULL, `timestamp` TEXT NOT NULL, `createdAt` TEXT NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, `color` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `chats` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `participants` TEXT NOT NULL, `lastMessage` TEXT, `lastMessageTime` TEXT, `unreadCount` INTEGER NOT NULL, `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f5257152308461eb40f319df5ae48a72')")
      }

      public override fun dropAllTables(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `users`")
        db.execSQL("DROP TABLE IF EXISTS `properties`")
        db.execSQL("DROP TABLE IF EXISTS `bookings`")
        db.execSQL("DROP TABLE IF EXISTS `messages`")
        db.execSQL("DROP TABLE IF EXISTS `categories`")
        db.execSQL("DROP TABLE IF EXISTS `chats`")
        val _callbacks: List<RoomDatabase.Callback>? = mCallbacks
        if (_callbacks != null) {
          for (_callback: RoomDatabase.Callback in _callbacks) {
            _callback.onDestructiveMigration(db)
          }
        }
      }

      public override fun onCreate(db: SupportSQLiteDatabase) {
        val _callbacks: List<RoomDatabase.Callback>? = mCallbacks
        if (_callbacks != null) {
          for (_callback: RoomDatabase.Callback in _callbacks) {
            _callback.onCreate(db)
          }
        }
      }

      public override fun onOpen(db: SupportSQLiteDatabase) {
        mDatabase = db
        internalInitInvalidationTracker(db)
        val _callbacks: List<RoomDatabase.Callback>? = mCallbacks
        if (_callbacks != null) {
          for (_callback: RoomDatabase.Callback in _callbacks) {
            _callback.onOpen(db)
          }
        }
      }

      public override fun onPreMigrate(db: SupportSQLiteDatabase) {
        dropFtsSyncTriggers(db)
      }

      public override fun onPostMigrate(db: SupportSQLiteDatabase) {
      }

      public override fun onValidateSchema(db: SupportSQLiteDatabase):
          RoomOpenHelper.ValidationResult {
        val _columnsUsers: HashMap<String, TableInfo.Column> = HashMap<String, TableInfo.Column>(12)
        _columnsUsers.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("firstName", TableInfo.Column("firstName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("lastName", TableInfo.Column("lastName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("email", TableInfo.Column("email", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("phone", TableInfo.Column("phone", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("profilePic", TableInfo.Column("profilePic", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("dateOfBirth", TableInfo.Column("dateOfBirth", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("gender", TableInfo.Column("gender", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("isVerified", TableInfo.Column("isVerified", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("createdAt", TableInfo.Column("createdAt", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("updatedAt", TableInfo.Column("updatedAt", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("preferences", TableInfo.Column("preferences", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysUsers: HashSet<TableInfo.ForeignKey> = HashSet<TableInfo.ForeignKey>(0)
        val _indicesUsers: HashSet<TableInfo.Index> = HashSet<TableInfo.Index>(0)
        val _infoUsers: TableInfo = TableInfo("users", _columnsUsers, _foreignKeysUsers,
            _indicesUsers)
        val _existingUsers: TableInfo = read(db, "users")
        if (!_infoUsers.equals(_existingUsers)) {
          return RoomOpenHelper.ValidationResult(false, """
              |users(com.shourov.apps.pacedream.core.database.entity.UserEntity).
              | Expected:
              |""".trimMargin() + _infoUsers + """
              |
              | Found:
              |""".trimMargin() + _existingUsers)
        }
        val _columnsProperties: HashMap<String, TableInfo.Column> =
            HashMap<String, TableInfo.Column>(18)
        _columnsProperties.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsProperties.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsProperties.put("description", TableInfo.Column("description", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProperties.put("propertyType", TableInfo.Column("propertyType", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProperties.put("location", TableInfo.Column("location", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsProperties.put("images", TableInfo.Column("images", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsProperties.put("amenities", TableInfo.Column("amenities", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsProperties.put("rating", TableInfo.Column("rating", "REAL", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsProperties.put("reviewCount", TableInfo.Column("reviewCount", "INTEGER", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProperties.put("basePrice", TableInfo.Column("basePrice", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsProperties.put("currency", TableInfo.Column("currency", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsProperties.put("isAvailable", TableInfo.Column("isAvailable", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProperties.put("hostId", TableInfo.Column("hostId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsProperties.put("hostName", TableInfo.Column("hostName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsProperties.put("hostAvatar", TableInfo.Column("hostAvatar", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsProperties.put("additionalDetails", TableInfo.Column("additionalDetails", "TEXT",
            false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProperties.put("createdAt", TableInfo.Column("createdAt", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsProperties.put("updatedAt", TableInfo.Column("updatedAt", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysProperties: HashSet<TableInfo.ForeignKey> = HashSet<TableInfo.ForeignKey>(0)
        val _indicesProperties: HashSet<TableInfo.Index> = HashSet<TableInfo.Index>(0)
        val _infoProperties: TableInfo = TableInfo("properties", _columnsProperties,
            _foreignKeysProperties, _indicesProperties)
        val _existingProperties: TableInfo = read(db, "properties")
        if (!_infoProperties.equals(_existingProperties)) {
          return RoomOpenHelper.ValidationResult(false, """
              |properties(com.shourov.apps.pacedream.core.database.entity.PropertyEntity).
              | Expected:
              |""".trimMargin() + _infoProperties + """
              |
              | Found:
              |""".trimMargin() + _existingProperties)
        }
        val _columnsBookings: HashMap<String, TableInfo.Column> =
            HashMap<String, TableInfo.Column>(18)
        _columnsBookings.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBookings.put("userId", TableInfo.Column("userId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBookings.put("propertyId", TableInfo.Column("propertyId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBookings.put("propertyName", TableInfo.Column("propertyName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBookings.put("propertyImage", TableInfo.Column("propertyImage", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBookings.put("startDate", TableInfo.Column("startDate", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBookings.put("endDate", TableInfo.Column("endDate", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBookings.put("startTime", TableInfo.Column("startTime", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBookings.put("endTime", TableInfo.Column("endTime", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBookings.put("totalPrice", TableInfo.Column("totalPrice", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBookings.put("currency", TableInfo.Column("currency", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBookings.put("status", TableInfo.Column("status", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBookings.put("paymentStatus", TableInfo.Column("paymentStatus", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBookings.put("specialRequests", TableInfo.Column("specialRequests", "TEXT", false,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBookings.put("hostId", TableInfo.Column("hostId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBookings.put("hostName", TableInfo.Column("hostName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBookings.put("createdAt", TableInfo.Column("createdAt", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBookings.put("updatedAt", TableInfo.Column("updatedAt", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysBookings: HashSet<TableInfo.ForeignKey> = HashSet<TableInfo.ForeignKey>(0)
        val _indicesBookings: HashSet<TableInfo.Index> = HashSet<TableInfo.Index>(0)
        val _infoBookings: TableInfo = TableInfo("bookings", _columnsBookings, _foreignKeysBookings,
            _indicesBookings)
        val _existingBookings: TableInfo = read(db, "bookings")
        if (!_infoBookings.equals(_existingBookings)) {
          return RoomOpenHelper.ValidationResult(false, """
              |bookings(com.shourov.apps.pacedream.core.database.entity.BookingEntity).
              | Expected:
              |""".trimMargin() + _infoBookings + """
              |
              | Found:
              |""".trimMargin() + _existingBookings)
        }
        val _columnsMessages: HashMap<String, TableInfo.Column> =
            HashMap<String, TableInfo.Column>(10)
        _columnsMessages.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("chatId", TableInfo.Column("chatId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("senderId", TableInfo.Column("senderId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("receiverId", TableInfo.Column("receiverId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("content", TableInfo.Column("content", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("messageType", TableInfo.Column("messageType", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("attachmentUrl", TableInfo.Column("attachmentUrl", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("isRead", TableInfo.Column("isRead", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("timestamp", TableInfo.Column("timestamp", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("createdAt", TableInfo.Column("createdAt", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysMessages: HashSet<TableInfo.ForeignKey> = HashSet<TableInfo.ForeignKey>(0)
        val _indicesMessages: HashSet<TableInfo.Index> = HashSet<TableInfo.Index>(0)
        val _infoMessages: TableInfo = TableInfo("messages", _columnsMessages, _foreignKeysMessages,
            _indicesMessages)
        val _existingMessages: TableInfo = read(db, "messages")
        if (!_infoMessages.equals(_existingMessages)) {
          return RoomOpenHelper.ValidationResult(false, """
              |messages(com.shourov.apps.pacedream.core.database.entity.MessageEntity).
              | Expected:
              |""".trimMargin() + _infoMessages + """
              |
              | Found:
              |""".trimMargin() + _existingMessages)
        }
        val _columnsCategories: HashMap<String, TableInfo.Column> =
            HashMap<String, TableInfo.Column>(8)
        _columnsCategories.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("icon", TableInfo.Column("icon", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("color", TableInfo.Column("color", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("isActive", TableInfo.Column("isActive", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("sortOrder", TableInfo.Column("sortOrder", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("createdAt", TableInfo.Column("createdAt", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("updatedAt", TableInfo.Column("updatedAt", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCategories: HashSet<TableInfo.ForeignKey> = HashSet<TableInfo.ForeignKey>(0)
        val _indicesCategories: HashSet<TableInfo.Index> = HashSet<TableInfo.Index>(0)
        val _infoCategories: TableInfo = TableInfo("categories", _columnsCategories,
            _foreignKeysCategories, _indicesCategories)
        val _existingCategories: TableInfo = read(db, "categories")
        if (!_infoCategories.equals(_existingCategories)) {
          return RoomOpenHelper.ValidationResult(false, """
              |categories(com.shourov.apps.pacedream.core.database.entity.CategoryEntity).
              | Expected:
              |""".trimMargin() + _infoCategories + """
              |
              | Found:
              |""".trimMargin() + _existingCategories)
        }
        val _columnsChats: HashMap<String, TableInfo.Column> = HashMap<String, TableInfo.Column>(8)
        _columnsChats.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("participants", TableInfo.Column("participants", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("lastMessage", TableInfo.Column("lastMessage", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("lastMessageTime", TableInfo.Column("lastMessageTime", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("unreadCount", TableInfo.Column("unreadCount", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("createdAt", TableInfo.Column("createdAt", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("updatedAt", TableInfo.Column("updatedAt", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysChats: HashSet<TableInfo.ForeignKey> = HashSet<TableInfo.ForeignKey>(0)
        val _indicesChats: HashSet<TableInfo.Index> = HashSet<TableInfo.Index>(0)
        val _infoChats: TableInfo = TableInfo("chats", _columnsChats, _foreignKeysChats,
            _indicesChats)
        val _existingChats: TableInfo = read(db, "chats")
        if (!_infoChats.equals(_existingChats)) {
          return RoomOpenHelper.ValidationResult(false, """
              |chats(com.shourov.apps.pacedream.core.database.entity.ChatEntity).
              | Expected:
              |""".trimMargin() + _infoChats + """
              |
              | Found:
              |""".trimMargin() + _existingChats)
        }
        return RoomOpenHelper.ValidationResult(true, null)
      }
    }, "f5257152308461eb40f319df5ae48a72", "a1cb5e98957e55979a365e21e4e2357e")
    val _sqliteConfig: SupportSQLiteOpenHelper.Configuration =
        SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build()
    val _helper: SupportSQLiteOpenHelper = config.sqliteOpenHelperFactory.create(_sqliteConfig)
    return _helper
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: HashMap<String, String> = HashMap<String, String>(0)
    val _viewTables: HashMap<String, Set<String>> = HashMap<String, Set<String>>(0)
    return InvalidationTracker(this, _shadowTablesMap, _viewTables,
        "users","properties","bookings","messages","categories","chats")
  }

  public override fun clearAllTables() {
    super.assertNotMainThread()
    val _db: SupportSQLiteDatabase = super.openHelper.writableDatabase
    try {
      super.beginTransaction()
      _db.execSQL("DELETE FROM `users`")
      _db.execSQL("DELETE FROM `properties`")
      _db.execSQL("DELETE FROM `bookings`")
      _db.execSQL("DELETE FROM `messages`")
      _db.execSQL("DELETE FROM `categories`")
      _db.execSQL("DELETE FROM `chats`")
      super.setTransactionSuccessful()
    } finally {
      super.endTransaction()
      _db.query("PRAGMA wal_checkpoint(FULL)").close()
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM")
      }
    }
  }

  protected override fun getRequiredTypeConverters(): Map<Class<out Any>, List<Class<out Any>>> {
    val _typeConvertersMap: HashMap<Class<out Any>, List<Class<out Any>>> =
        HashMap<Class<out Any>, List<Class<out Any>>>()
    _typeConvertersMap.put(UserDao::class.java, UserDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(PropertyDao::class.java, PropertyDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(BookingDao::class.java, BookingDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(MessageDao::class.java, MessageDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(CategoryDao::class.java, CategoryDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ChatDao::class.java, ChatDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecs(): Set<Class<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: HashSet<Class<out AutoMigrationSpec>> =
        HashSet<Class<out AutoMigrationSpec>>()
    return _autoMigrationSpecsSet
  }

  public override
      fun getAutoMigrations(autoMigrationSpecs: Map<Class<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = ArrayList<Migration>()
    return _autoMigrations
  }

  public override fun userDao(): UserDao = _userDao.value

  public override fun propertyDao(): PropertyDao = _propertyDao.value

  public override fun bookingDao(): BookingDao = _bookingDao.value

  public override fun messageDao(): MessageDao = _messageDao.value

  public override fun categoryDao(): CategoryDao = _categoryDao.value

  public override fun chatDao(): ChatDao = _chatDao.value
}
