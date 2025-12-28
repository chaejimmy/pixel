package com.shourov.apps.pacedream.core.data.repository

import com.shourov.apps.pacedream.core.database.dao.BookingDao
import com.shourov.apps.pacedream.core.database.dao.CategoryDao
import com.shourov.apps.pacedream.core.database.dao.ChatDao
import com.shourov.apps.pacedream.core.database.dao.MessageDao
import com.shourov.apps.pacedream.core.database.dao.PropertyDao
import com.shourov.apps.pacedream.core.database.dao.UserDao
import com.shourov.apps.pacedream.core.database.entity.BookingEntity
import com.shourov.apps.pacedream.core.database.entity.CategoryEntity
import com.shourov.apps.pacedream.core.database.entity.ChatEntity
import com.shourov.apps.pacedream.core.database.entity.MessageEntity
import com.shourov.apps.pacedream.core.database.entity.PropertyEntity
import com.shourov.apps.pacedream.core.database.entity.UserEntity
import com.shourov.apps.pacedream.core.network.services.PaceDreamApiService
import com.shourov.apps.pacedream.model.BookingModel
import com.shourov.apps.pacedream.model.MessageModel
import com.shourov.apps.pacedream.model.response.home.rooms.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PaceDreamRepository @Inject constructor(
    private val userDao: UserDao,
    private val propertyDao: PropertyDao,
    private val bookingDao: BookingDao,
    private val messageDao: MessageDao,
    private val categoryDao: CategoryDao,
    private val chatDao: ChatDao,
    private val apiService: PaceDreamApiService
) {
    
    // User operations
    fun getUserById(userId: String) = userDao.getUserById(userId)
    
    suspend fun insertUser(user: UserEntity) {
        userDao.insertUser(user)
    }
    
    // Property operations
    fun getAllProperties(): Flow<List<Result>> {
        return propertyDao.getAllProperties().map { entities ->
            entities.map { it.toPropertyModel() }
        }
    }
    
    fun getPropertyById(propertyId: String): Flow<Result?> {
        return propertyDao.getPropertyById(propertyId).map { entity ->
            entity?.toPropertyModel()
        }
    }
    
    suspend fun refreshProperties() {
        try {
            val response = apiService.getAllProperties()
            // Convert API response to entities and insert
            // This would need to be implemented based on your API response structure
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    // Booking operations
    fun getAllBookings(): Flow<List<BookingModel>> {
        return bookingDao.getAllBookings().map { entities ->
            entities.map { it.toBookingModel() }
        }
    }
    
    fun getBookingsByUserName(userName: String): Flow<List<BookingModel>> {
        return bookingDao.getBookingsByUserName(userName).map { entities ->
            entities.map { it.toBookingModel() }
        }
    }
    
    suspend fun createBooking(booking: BookingEntity) {
        try {
            // For now, just insert into local DB
            bookingDao.insertBooking(booking)
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    // Message operations
    fun getMessagesByUser(userName: String): Flow<List<MessageModel>> {
        return messageDao.getMessagesByUser(userName).map { entities ->
            entities.map { it.toMessageModel() }
        }
    }
    
    suspend fun sendMessage(message: MessageEntity) {
        try {
            // For now, just insert into local DB
            messageDao.insertMessage(message)
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    // Category operations
    fun getAllCategories() = categoryDao.getAllCategories()
    
    suspend fun insertCategories(categories: List<CategoryEntity>) {
        categoryDao.insertCategories(categories)
    }
    
    // Chat operations
    fun getAllChats() = chatDao.getAllChats()
    
    fun getChatsByUser(userId: String) = chatDao.getChatsByUser(userId)
    
    suspend fun insertChat(chat: ChatEntity) {
        chatDao.insertChat(chat)
    }
    
    // Private conversion functions to avoid extension function ambiguity
    private fun BookingEntity.toBookingModel(): BookingModel {
        return BookingModel(
            id = id,
            userProfilePic = userProfilePic,
            userName = userName,
            checkOutTime = checkOutTime,
            checkInTime = checkInTime,
            bookingStatus = bookingStatus,
            price = price
        )
    }
    
    private fun MessageEntity.toMessageModel(): MessageModel {
        return MessageModel(
            profilePic = profilePic,
            userName = userName,
            messageTime = messageTime,
            message = message,
            newMessageCount = newMessageCount
        )
    }
    
    private fun PropertyEntity.toPropertyModel(): Result {
        return Result(
            __v = null,
            _id = id,
            additional_details = null,
            amenities = null,
            createdAt = createdAt,
            description = description,
            dynamic_price = null,
            facilities = null,
            faq = null,
            guest_details = null,
            host_id = hostId,
            ideal_renters = null,
            images = null,
            isDeleted = null,
            location = null,
            name = name,
            property_type = propertyType,
            rating = rating,
            room_details = null,
            room_type = roomType,
            rules = null,
            status = status,
            updatedAt = updatedAt
        )
    }
}
