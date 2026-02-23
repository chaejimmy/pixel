package com.shourov.apps.pacedream.core.network.services

import com.shourov.apps.pacedream.core.network.ApiEndPoints
import com.shourov.apps.pacedream.core.network.model.AllPropertiesResponseModel
import com.shourov.apps.pacedream.core.network.model.UpdatedProfileData
import com.shourov.apps.pacedream.model.request.ForgotPasswordRequest
import com.shourov.apps.pacedream.model.response.auth.ForgotPasswordResponse
import com.shourov.apps.pacedream.model.response.auth.LoginResponse
import com.shourov.apps.pacedream.model.response.auth.MobileOTPResponse
import com.shourov.apps.pacedream.model.response.auth.UserProfileResponse
import com.shourov.apps.pacedream.model.response.home.rooms.RoomsResponse
import com.shourov.apps.pacedream.model.BookingModel
import com.shourov.apps.pacedream.model.MessageModel
import com.shourov.apps.pacedream.model.NotificationModel
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PaceDreamApiService {

    @GET(ApiEndPoints.GET_ALL_PROPERTIES)
    suspend fun getAllProperties(): AllPropertiesResponseModel

    @POST(ApiEndPoints.SEND_OTP)
    @FormUrlEncoded
    suspend fun sendOtp(@Field("mobile") mobile: String): MobileOTPResponse

    @POST(ApiEndPoints.AUTH_LOGIN_MOBILE)
    @FormUrlEncoded
    suspend fun loginUser(
        @Field("mobile") mobile: String,
        @Field("otp") otp: String,
    ): LoginResponse

    @POST(ApiEndPoints.AUTH_LOGIN_EMAIL)
    @FormUrlEncoded
    suspend fun loginUserEmail(
        @Field("email") email: String,
        @Field("password") password: String,
    ): LoginResponse

    @PUT(ApiEndPoints.AUTH_FORGOT_PASSWORD)
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequest,
    ): ForgotPasswordResponse

    @POST(ApiEndPoints.AUTH_SIGNUP_MOBILE)
    @FormUrlEncoded
    suspend fun registerUser(
        @Field("dob") dob: String,
        @Field("firstName") firstName: String,
        @Field("lastName") lastName: String,
        @Field("gender") gender: String,
        @Field("password") password: String,
        @Field("phoneNumber") phoneNumber: String,
    ): MobileOTPResponse

    @POST(ApiEndPoints.AUTH_SIGNUP_EMAIL)
    @FormUrlEncoded
    suspend fun registerUserEmail(
        @Field("dob") dob: String,
        @Field("firstName") firstName: String,
        @Field("lastName") lastName: String,
        @Field("gender") gender: String,
        @Field("password") password: String,
        @Field("email") email: String,
    ): LoginResponse

    @POST(ApiEndPoints.AUTH_SEND_EMAIL_CODE)
    @FormUrlEncoded
    suspend fun sendEmailCode(
        @Field("email") email: String,
    ): LoginResponse

    @POST(ApiEndPoints.AUTH_VERIFY_EMAIL)
    @FormUrlEncoded
    suspend fun verifyEmailCode(
        @Field("email") email: String,
        @Field("code") code: String,
    ): LoginResponse

    @GET(ApiEndPoints.USER_GET_PROFILE)
    suspend fun getUserInformation(): UserProfileResponse

    @PUT(ApiEndPoints.UPDATE_USER_PROFILE)
    suspend fun updateUserInformation(
        @Header("Authorization") aAuthorization: String,
        @Body data: UpdatedProfileData,
    ): Response<Any>

    @GET(ApiEndPoints.GET_USER_REVIEWS)
    suspend fun getUserReviews(
        @Header("Authorization") aAuthorization: String,
    ): Response<Any>

    @GET(ApiEndPoints.GET_ALREADY_BOOKED)
    suspend fun getProfileAlreadyBookedList(
        @Header("Authorization") aAuthorization: String,
    ): Response<Any>

    @GET(ApiEndPoints.ROOMMATE_ROOM_STAY)
    suspend fun getRoomStayAll(): RoomsResponse

    // ── Property Management APIs ──────────────────────────────
    @GET(ApiEndPoints.GET_PROPERTY_BY_ID)
    suspend fun getPropertyById(@Path("propertyId") propertyId: String): Response<Any>

    @GET(ApiEndPoints.SEARCH_PROPERTIES)
    suspend fun searchProperties(
        @Query("query") query: String,
        @Query("propertyType") propertyType: String?,
        @Query("minPrice") minPrice: Double?,
        @Query("maxPrice") maxPrice: Double?,
        @Query("location") location: String?,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<Any>

    @GET(ApiEndPoints.GET_PROPERTY_CATEGORIES)
    suspend fun getPropertyCategories(): Response<Any>

    @GET(ApiEndPoints.GET_POPULAR_DESTINATIONS)
    suspend fun getPopularDestinations(): Response<Any>

    @GET(ApiEndPoints.GET_FEATURED_PROPERTIES)
    suspend fun getFeaturedProperties(): Response<Any>

    @GET(ApiEndPoints.GET_NEARBY_PROPERTIES)
    suspend fun getNearbyProperties(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("radius") radiusKm: Int = 25
    ): Response<Any>

    // ── Wishlist / Favorites (iOS parity) ──────────────────────
    @GET(ApiEndPoints.GET_WISHLIST)
    suspend fun getWishlist(@Header("Authorization") token: String): Response<Any>

    @POST(ApiEndPoints.ADD_TO_WISHLIST)
    suspend fun addToWishlist(
        @Header("Authorization") token: String,
        @Body data: Map<String, String>
    ): Response<Any>

    @DELETE(ApiEndPoints.REMOVE_FROM_WISHLIST)
    suspend fun removeFromWishlist(
        @Header("Authorization") token: String,
        @Path("propertyId") propertyId: String
    ): Response<Any>

    // ── Booking Management APIs ────────────────────────────────
    @POST(ApiEndPoints.CREATE_BOOKING)
    suspend fun createBooking(@Body booking: BookingModel): Response<Any>

    @GET(ApiEndPoints.GET_USER_BOOKINGS)
    suspend fun getUserBookings(@Path("userId") userId: String): Response<Any>

    @GET(ApiEndPoints.GET_BOOKING_BY_ID)
    suspend fun getBookingById(@Path("bookingId") bookingId: String): Response<Any>

    @PUT(ApiEndPoints.UPDATE_BOOKING)
    suspend fun updateBooking(
        @Path("bookingId") bookingId: String,
        @Body booking: BookingModel
    ): Response<Any>

    @DELETE(ApiEndPoints.CANCEL_BOOKING)
    suspend fun cancelBooking(@Path("bookingId") bookingId: String): Response<Any>

    @POST(ApiEndPoints.CONFIRM_BOOKING)
    suspend fun confirmBooking(@Path("bookingId") bookingId: String): Response<Any>

    @GET(ApiEndPoints.GET_BOOKING_AVAILABILITY)
    suspend fun getBookingAvailability(@Path("propertyId") propertyId: String): Response<Any>

    // ── Messaging APIs ─────────────────────────────────────────
    @GET(ApiEndPoints.GET_USER_CHATS)
    suspend fun getUserChats(@Path("userId") userId: String): Response<Any>

    @GET(ApiEndPoints.GET_CHAT_MESSAGES)
    suspend fun getChatMessages(
        @Path("chatId") chatId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<Any>

    @POST(ApiEndPoints.CREATE_CHAT)
    suspend fun createChat(@Body chatData: Map<String, String>): Response<Any>

    @POST(ApiEndPoints.SEND_MESSAGE)
    suspend fun sendMessage(
        @Path("chatId") chatId: String,
        @Body message: MessageModel
    ): Response<Any>

    @PUT(ApiEndPoints.MARK_MESSAGE_READ)
    suspend fun markMessageAsRead(
        @Path("chatId") chatId: String,
        @Path("messageId") messageId: String
    ): Response<Any>

    // ── Notification APIs ──────────────────────────────────────
    @GET(ApiEndPoints.GET_USER_NOTIFICATIONS)
    suspend fun getUserNotifications(@Path("userId") userId: String): Response<Any>

    @PUT(ApiEndPoints.MARK_NOTIFICATION_READ)
    suspend fun markNotificationAsRead(@Path("notificationId") notificationId: String): Response<Any>

    @PUT(ApiEndPoints.MARK_ALL_NOTIFICATIONS_READ)
    suspend fun markAllNotificationsAsRead(@Path("userId") userId: String): Response<Any>

    @POST(ApiEndPoints.REGISTER_PUSH_TOKEN)
    suspend fun registerPushToken(@Body tokenData: Map<String, String>): Response<Any>

    // ── Payment APIs ───────────────────────────────────────────
    @POST(ApiEndPoints.CREATE_PAYMENT_INTENT)
    suspend fun createPaymentIntent(@Body paymentData: Map<String, Any>): Response<Any>

    @POST(ApiEndPoints.CONFIRM_PAYMENT)
    suspend fun confirmPayment(@Body paymentData: Map<String, Any>): Response<Any>

    @GET(ApiEndPoints.GET_PAYMENT_HISTORY)
    suspend fun getPaymentHistory(@Path("userId") userId: String): Response<Any>

    @GET(ApiEndPoints.GET_PAYMENT_METHODS)
    suspend fun getPaymentMethods(@Header("Authorization") token: String): Response<Any>

    // ── Review and Rating APIs ─────────────────────────────────
    @POST(ApiEndPoints.CREATE_REVIEW)
    suspend fun createReview(@Body reviewData: Map<String, Any>): Response<Any>

    @GET(ApiEndPoints.GET_PROPERTY_REVIEWS)
    suspend fun getPropertyReviews(@Path("propertyId") propertyId: String): Response<Any>

    @GET(ApiEndPoints.GET_USER_REVIEWS_BY_ID)
    suspend fun getUserReviewsById(@Path("userId") userId: String): Response<Any>

    // ── Collections / Lists APIs (Web parity) ──────────────
    @GET(ApiEndPoints.GET_COLLECTIONS)
    suspend fun getCollections(@Header("Authorization") token: String): Response<Any>

    @POST(ApiEndPoints.CREATE_COLLECTION)
    suspend fun createCollection(
        @Header("Authorization") token: String,
        @Body data: Map<String, Any>
    ): Response<Any>

    @GET(ApiEndPoints.GET_COLLECTION_BY_ID)
    suspend fun getCollectionById(
        @Header("Authorization") token: String,
        @Path("collectionId") collectionId: String
    ): Response<Any>

    @DELETE(ApiEndPoints.DELETE_COLLECTION)
    suspend fun deleteCollection(
        @Header("Authorization") token: String,
        @Path("collectionId") collectionId: String
    ): Response<Any>

    @POST(ApiEndPoints.ADD_TO_COLLECTION)
    suspend fun addToCollection(
        @Header("Authorization") token: String,
        @Path("collectionId") collectionId: String,
        @Body data: Map<String, String>
    ): Response<Any>

    @DELETE(ApiEndPoints.REMOVE_FROM_COLLECTION)
    suspend fun removeFromCollection(
        @Header("Authorization") token: String,
        @Path("collectionId") collectionId: String,
        @Path("itemId") itemId: String
    ): Response<Any>

    // ── Analytics and Tracking APIs ────────────────────────────
    @POST(ApiEndPoints.TRACK_EVENT)
    suspend fun trackEvent(@Body eventData: Map<String, Any>): Response<Any>

    @POST(ApiEndPoints.TRACK_PROPERTY_VIEW)
    suspend fun trackPropertyView(@Body viewData: Map<String, Any>): Response<Any>
}