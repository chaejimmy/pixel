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
import com.shourov.apps.pacedream.core.database.entity.asExternalModel as bookingAsExternalModel
import com.shourov.apps.pacedream.core.database.entity.asExternalModel as messageAsExternalModel
import com.shourov.apps.pacedream.core.database.entity.asExternalModel as propertyAsExternalModel
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
            entities.map { it.asExternalModel() }
        }
    }
    
    fun getPropertyById(propertyId: String): Flow<Result?> {
        return propertyDao.getPropertyById(propertyId).map { entity ->
            entity?.asExternalModel()
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
            entities.map { it.asExternalModel() }
        }
    }
    
    fun getBookingsByUserName(userName: String): Flow<List<BookingModel>> {
        return bookingDao.getBookingsByUserName(userName).map { entities ->
            entities.map { it.asExternalModel() }
        }
    }
    
    suspend fun createBooking(booking: BookingEntity) {
        try {
            // Call API to create booking
            // val response = apiService.createBooking(booking)
            // if (response.isSuccessful) {
            //     response.body()?.let { createdBooking ->
            //         bookingDao.insertBooking(createdBooking.asEntity())
            //     }
            // }
            // For now, just insert into local DB
            bookingDao.insertBooking(booking)
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    // Message operations
    fun getMessagesByUser(userName: String): Flow<List<MessageModel>> {
        return messageDao.getMessagesByUser(userName).map { entities ->
            entities.map { it.asExternalModel() }
        }
    }
    
    suspend fun sendMessage(message: MessageEntity) {
        try {
            // Call API to send message
            // val response = apiService.sendMessage(chatId, message)
            // if (response.isSuccessful) {
            //     response.body()?.let { sentMessage ->
            //         messageDao.insertMessage(sentMessage.asEntity())
            //     }
            // }
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
}

// Extension functions to resolve ambiguity
private fun BookingEntity.asExternalModel(): BookingModel =
    com.shourov.apps.pacedream.core.database.entity.asExternalModel(this)

private fun MessageEntity.asExternalModel(): MessageModel =
    com.shourov.apps.pacedream.core.database.entity.asExternalModel(this)

private fun PropertyEntity.asExternalModel(): Result =
    com.shourov.apps.pacedream.core.database.entity.asExternalModel(this)
