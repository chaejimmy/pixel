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

    // Property Management APIs
    @GET("properties/{propertyId}")
    suspend fun getPropertyById(@Path("propertyId") propertyId: String): Response<Any>

    @GET("properties/search")
    suspend fun searchProperties(
        @Query("query") query: String,
        @Query("propertyType") propertyType: String?,
        @Query("minPrice") minPrice: Double?,
        @Query("maxPrice") maxPrice: Double?,
        @Query("location") location: String?,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<Any>

    @GET("properties/categories")
    suspend fun getPropertyCategories(): Response<Any>

    @GET("properties/destinations")
    suspend fun getPopularDestinations(): Response<Any>

    // Booking Management APIs
    @POST("bookings")
    suspend fun createBooking(@Body booking: BookingModel): Response<Any>

    @GET("bookings/user/{userId}")
    suspend fun getUserBookings(@Path("userId") userId: String): Response<Any>

    @GET("bookings/{bookingId}")
    suspend fun getBookingById(@Path("bookingId") bookingId: String): Response<Any>

    @PUT("bookings/{bookingId}")
    suspend fun updateBooking(
        @Path("bookingId") bookingId: String,
        @Body booking: BookingModel
    ): Response<Any>

    @DELETE("bookings/{bookingId}")
    suspend fun cancelBooking(@Path("bookingId") bookingId: String): Response<Any>

    @POST("bookings/{bookingId}/confirm")
    suspend fun confirmBooking(@Path("bookingId") bookingId: String): Response<Any>

    // Messaging APIs
    @GET("chats/user/{userId}")
    suspend fun getUserChats(@Path("userId") userId: String): Response<Any>

    @GET("chats/{chatId}/messages")
    suspend fun getChatMessages(
        @Path("chatId") chatId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<Any>

    @POST("chats")
    suspend fun createChat(@Body chatData: Map<String, String>): Response<Any>

    @POST("chats/{chatId}/messages")
    suspend fun sendMessage(
        @Path("chatId") chatId: String,
        @Body message: MessageModel
    ): Response<Any>

    @PUT("chats/{chatId}/messages/{messageId}/read")
    suspend fun markMessageAsRead(
        @Path("chatId") chatId: String,
        @Path("messageId") messageId: String
    ): Response<Any>

    // Notification APIs
    @GET("notifications/user/{userId}")
    suspend fun getUserNotifications(@Path("userId") userId: String): Response<Any>

    @PUT("notifications/{notificationId}/read")
    suspend fun markNotificationAsRead(@Path("notificationId") notificationId: String): Response<Any>

    @PUT("notifications/user/{userId}/read-all")
    suspend fun markAllNotificationsAsRead(@Path("userId") userId: String): Response<Any>

    // Payment APIs
    @POST("payments/create-intent")
    suspend fun createPaymentIntent(@Body paymentData: Map<String, Any>): Response<Any>

    @POST("payments/confirm")
    suspend fun confirmPayment(@Body paymentData: Map<String, Any>): Response<Any>

    @GET("payments/user/{userId}/history")
    suspend fun getPaymentHistory(@Path("userId") userId: String): Response<Any>

    // Review and Rating APIs
    @POST("reviews")
    suspend fun createReview(@Body reviewData: Map<String, Any>): Response<Any>

    @GET("reviews/property/{propertyId}")
    suspend fun getPropertyReviews(@Path("propertyId") propertyId: String): Response<Any>

    @GET("reviews/user/{userId}")
    suspend fun getUserReviewsById(@Path("userId") userId: String): Response<Any>

    // Analytics and Tracking APIs
    @POST("analytics/event")
    suspend fun trackEvent(@Body eventData: Map<String, Any>): Response<Any>

    @POST("analytics/property-view")
    suspend fun trackPropertyView(@Body viewData: Map<String, Any>): Response<Any>
}