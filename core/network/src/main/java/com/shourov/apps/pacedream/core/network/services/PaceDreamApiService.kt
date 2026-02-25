package com.shourov.apps.pacedream.core.network.services

import com.shourov.apps.pacedream.core.network.ApiEndPoints
import com.shourov.apps.pacedream.core.network.model.AllPropertiesResponseModel
import com.shourov.apps.pacedream.core.network.model.AnalyticsEventResponse
import com.shourov.apps.pacedream.core.network.model.ApiListResponse
import com.shourov.apps.pacedream.core.network.model.ApiResponse
import com.shourov.apps.pacedream.core.network.model.BookingAvailabilityResponse
import com.shourov.apps.pacedream.core.network.model.BookingResponse
import com.shourov.apps.pacedream.core.network.model.CategoryResponse
import com.shourov.apps.pacedream.core.network.model.ChatResponse
import com.shourov.apps.pacedream.core.network.model.CollectionItemResponse
import com.shourov.apps.pacedream.core.network.model.CollectionResponse
import com.shourov.apps.pacedream.core.network.model.DestinationResponse
import com.shourov.apps.pacedream.core.network.model.HostAnalyticsResponse
import com.shourov.apps.pacedream.core.network.model.HostEarningsResponse
import com.shourov.apps.pacedream.core.network.model.HostListingResponse
import com.shourov.apps.pacedream.core.network.model.MessageResponse
import com.shourov.apps.pacedream.core.network.model.NotificationResponse
import com.shourov.apps.pacedream.core.network.model.PaymentHistoryResponse
import com.shourov.apps.pacedream.core.network.model.PaymentIntentResponse
import com.shourov.apps.pacedream.core.network.model.PaymentMethodResponse
import com.shourov.apps.pacedream.core.network.model.ProfileInformationResponse
import com.shourov.apps.pacedream.core.network.model.PropertyResponse
import com.shourov.apps.pacedream.core.network.model.ReviewResponse
import com.shourov.apps.pacedream.core.network.model.UpdatedProfileData
import com.shourov.apps.pacedream.core.network.model.WishlistItemResponse
import com.shourov.apps.pacedream.core.network.model.verification.PhoneConfirmRequest
import com.shourov.apps.pacedream.core.network.model.verification.PhoneConfirmResponse
import com.shourov.apps.pacedream.core.network.model.verification.PhoneVerificationRequest
import com.shourov.apps.pacedream.core.network.model.verification.PhoneVerificationResponse
import com.shourov.apps.pacedream.core.network.model.verification.SubmitVerificationRequest
import com.shourov.apps.pacedream.core.network.model.verification.SubmitVerificationResponse
import com.shourov.apps.pacedream.core.network.model.verification.UploadURLsRequest
import com.shourov.apps.pacedream.core.network.model.verification.UploadURLsResponse
import com.shourov.apps.pacedream.core.network.model.verification.VerificationStatusResponse
import com.shourov.apps.pacedream.model.request.ForgotPasswordRequest
import com.shourov.apps.pacedream.model.response.auth.ForgotPasswordResponse
import com.shourov.apps.pacedream.model.response.auth.LoginResponse
import com.shourov.apps.pacedream.model.response.auth.MobileOTPResponse
import com.shourov.apps.pacedream.model.response.auth.UserProfileResponse
import com.shourov.apps.pacedream.model.response.home.rooms.RoomsResponse
import com.shourov.apps.pacedream.model.BookingModel
import com.shourov.apps.pacedream.model.MessageModel
import com.shourov.apps.pacedream.model.NotificationModel
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
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
    ): Response<ProfileInformationResponse>

    @GET(ApiEndPoints.GET_USER_REVIEWS)
    suspend fun getUserReviews(
        @Header("Authorization") aAuthorization: String,
    ): Response<ApiListResponse<ReviewResponse>>

    @GET(ApiEndPoints.GET_ALREADY_BOOKED)
    suspend fun getProfileAlreadyBookedList(
        @Header("Authorization") aAuthorization: String,
    ): Response<ApiListResponse<BookingResponse>>

    // ── Account / User Management (missing endpoints) ────────────
    @GET(ApiEndPoints.ACCOUNT_ME)
    suspend fun getAccountMe(
        @Header("Authorization") token: String
    ): Response<UserProfileResponse>

    @Multipart
    @POST(ApiEndPoints.USER_UPLOAD_AVATAR)
    suspend fun uploadAvatar(
        @Header("Authorization") token: String,
        @Part avatar: MultipartBody.Part
    ): Response<ApiResponse<String>>

    @DELETE(ApiEndPoints.USER_DELETE_ACCOUNT)
    suspend fun deleteAccount(
        @Header("Authorization") token: String
    ): Response<ApiResponse<Unit>>

    // ── Auth (missing endpoints) ─────────────────────────────────
    @POST(ApiEndPoints.AUTH_REFRESH_TOKEN)
    suspend fun refreshToken(
        @Body refreshData: Map<String, String>
    ): Response<LoginResponse>

    @POST(ApiEndPoints.AUTH_AUTH0_CALLBACK)
    suspend fun auth0Callback(
        @Body callbackData: Map<String, String>
    ): Response<LoginResponse>

    @POST(ApiEndPoints.AUTH_LOGOUT)
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<ApiResponse<Unit>>

    @GET(ApiEndPoints.ROOMMATE_ROOM_STAY)
    suspend fun getRoomStayAll(): RoomsResponse

    // ── Property Management APIs ──────────────────────────────
    @GET(ApiEndPoints.GET_PROPERTY_BY_ID)
    suspend fun getPropertyById(@Path("propertyId") propertyId: String): Response<ApiResponse<PropertyResponse>>

    @GET(ApiEndPoints.SEARCH_PROPERTIES)
    suspend fun searchProperties(
        @Query("query") query: String,
        @Query("propertyType") propertyType: String?,
        @Query("minPrice") minPrice: Double?,
        @Query("maxPrice") maxPrice: Double?,
        @Query("location") location: String?,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ApiListResponse<PropertyResponse>>

    @GET(ApiEndPoints.GET_PROPERTY_CATEGORIES)
    suspend fun getPropertyCategories(): Response<ApiListResponse<CategoryResponse>>

    @GET(ApiEndPoints.GET_POPULAR_DESTINATIONS)
    suspend fun getPopularDestinations(): Response<ApiListResponse<DestinationResponse>>

    @GET(ApiEndPoints.GET_FEATURED_PROPERTIES)
    suspend fun getFeaturedProperties(): Response<ApiListResponse<PropertyResponse>>

    @GET(ApiEndPoints.GET_NEARBY_PROPERTIES)
    suspend fun getNearbyProperties(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("radius") radiusKm: Int = 25
    ): Response<ApiListResponse<PropertyResponse>>

    // ── Wishlist / Favorites (iOS parity) ──────────────────────
    @GET(ApiEndPoints.GET_WISHLIST)
    suspend fun getWishlist(@Header("Authorization") token: String): Response<ApiListResponse<WishlistItemResponse>>

    @POST(ApiEndPoints.ADD_TO_WISHLIST)
    suspend fun addToWishlist(
        @Header("Authorization") token: String,
        @Body data: Map<String, String>
    ): Response<ApiResponse<WishlistItemResponse>>

    @DELETE(ApiEndPoints.REMOVE_FROM_WISHLIST)
    suspend fun removeFromWishlist(
        @Header("Authorization") token: String,
        @Path("propertyId") propertyId: String
    ): Response<ApiResponse<Unit>>

    // ── Booking Management APIs ────────────────────────────────
    @POST(ApiEndPoints.CREATE_BOOKING)
    suspend fun createBooking(@Body booking: BookingModel): Response<ApiResponse<BookingResponse>>

    @GET(ApiEndPoints.GET_USER_BOOKINGS)
    suspend fun getUserBookings(@Path("userId") userId: String): Response<ApiListResponse<BookingResponse>>

    @GET(ApiEndPoints.GET_BOOKING_BY_ID)
    suspend fun getBookingById(@Path("bookingId") bookingId: String): Response<ApiResponse<BookingResponse>>

    @PUT(ApiEndPoints.UPDATE_BOOKING)
    suspend fun updateBooking(
        @Path("bookingId") bookingId: String,
        @Body booking: BookingModel
    ): Response<ApiResponse<BookingResponse>>

    @DELETE(ApiEndPoints.CANCEL_BOOKING)
    suspend fun cancelBooking(@Path("bookingId") bookingId: String): Response<ApiResponse<BookingResponse>>

    @POST(ApiEndPoints.CONFIRM_BOOKING)
    suspend fun confirmBooking(@Path("bookingId") bookingId: String): Response<ApiResponse<BookingResponse>>

    @GET(ApiEndPoints.GET_BOOKING_AVAILABILITY)
    suspend fun getBookingAvailability(@Path("propertyId") propertyId: String): Response<ApiResponse<BookingAvailabilityResponse>>

    // ── Messaging APIs ─────────────────────────────────────────
    @GET(ApiEndPoints.GET_USER_CHATS)
    suspend fun getUserChats(@Path("userId") userId: String): Response<ApiListResponse<ChatResponse>>

    @GET(ApiEndPoints.GET_CHAT_MESSAGES)
    suspend fun getChatMessages(
        @Path("chatId") chatId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<ApiListResponse<MessageResponse>>

    @POST(ApiEndPoints.CREATE_CHAT)
    suspend fun createChat(@Body chatData: Map<String, String>): Response<ApiResponse<ChatResponse>>

    @POST(ApiEndPoints.SEND_MESSAGE)
    suspend fun sendMessage(
        @Path("chatId") chatId: String,
        @Body message: MessageModel
    ): Response<ApiResponse<MessageResponse>>

    @PUT(ApiEndPoints.MARK_MESSAGE_READ)
    suspend fun markMessageAsRead(
        @Path("chatId") chatId: String,
        @Path("messageId") messageId: String
    ): Response<ApiResponse<MessageResponse>>

    // ── Notification APIs ──────────────────────────────────────
    @GET(ApiEndPoints.GET_USER_NOTIFICATIONS)
    suspend fun getUserNotifications(@Path("userId") userId: String): Response<ApiListResponse<NotificationResponse>>

    @PUT(ApiEndPoints.MARK_NOTIFICATION_READ)
    suspend fun markNotificationAsRead(@Path("notificationId") notificationId: String): Response<ApiResponse<NotificationResponse>>

    @PUT(ApiEndPoints.MARK_ALL_NOTIFICATIONS_READ)
    suspend fun markAllNotificationsAsRead(@Path("userId") userId: String): Response<ApiResponse<Unit>>

    @POST(ApiEndPoints.REGISTER_PUSH_TOKEN)
    suspend fun registerPushToken(@Body tokenData: Map<String, String>): Response<ApiResponse<Unit>>

    // ── Payment APIs ───────────────────────────────────────────
    @POST(ApiEndPoints.CREATE_PAYMENT_INTENT)
    suspend fun createPaymentIntent(@Body paymentData: Map<String, Any>): Response<ApiResponse<PaymentIntentResponse>>

    @POST(ApiEndPoints.CONFIRM_PAYMENT)
    suspend fun confirmPayment(@Body paymentData: Map<String, Any>): Response<ApiResponse<PaymentIntentResponse>>

    @GET(ApiEndPoints.GET_PAYMENT_HISTORY)
    suspend fun getPaymentHistory(@Path("userId") userId: String): Response<ApiListResponse<PaymentHistoryResponse>>

    @GET(ApiEndPoints.GET_PAYMENT_METHODS)
    suspend fun getPaymentMethods(@Header("Authorization") token: String): Response<ApiListResponse<PaymentMethodResponse>>

    // ── Review and Rating APIs ─────────────────────────────────
    @POST(ApiEndPoints.CREATE_REVIEW)
    suspend fun createReview(@Body reviewData: Map<String, Any>): Response<ApiResponse<ReviewResponse>>

    @GET(ApiEndPoints.GET_PROPERTY_REVIEWS)
    suspend fun getPropertyReviews(@Path("propertyId") propertyId: String): Response<ApiListResponse<ReviewResponse>>

    @GET(ApiEndPoints.GET_USER_REVIEWS_BY_ID)
    suspend fun getUserReviewsById(@Path("userId") userId: String): Response<ApiListResponse<ReviewResponse>>

    // ── Collections / Lists APIs (Web parity) ──────────────
    @GET(ApiEndPoints.GET_COLLECTIONS)
    suspend fun getCollections(@Header("Authorization") token: String): Response<ApiListResponse<CollectionResponse>>

    @POST(ApiEndPoints.CREATE_COLLECTION)
    suspend fun createCollection(
        @Header("Authorization") token: String,
        @Body data: Map<String, Any>
    ): Response<ApiResponse<CollectionResponse>>

    @GET(ApiEndPoints.GET_COLLECTION_BY_ID)
    suspend fun getCollectionById(
        @Header("Authorization") token: String,
        @Path("collectionId") collectionId: String
    ): Response<ApiResponse<CollectionResponse>>

    @DELETE(ApiEndPoints.DELETE_COLLECTION)
    suspend fun deleteCollection(
        @Header("Authorization") token: String,
        @Path("collectionId") collectionId: String
    ): Response<ApiResponse<Unit>>

    @POST(ApiEndPoints.ADD_TO_COLLECTION)
    suspend fun addToCollection(
        @Header("Authorization") token: String,
        @Path("collectionId") collectionId: String,
        @Body data: Map<String, String>
    ): Response<ApiResponse<CollectionItemResponse>>

    @DELETE(ApiEndPoints.REMOVE_FROM_COLLECTION)
    suspend fun removeFromCollection(
        @Header("Authorization") token: String,
        @Path("collectionId") collectionId: String,
        @Path("itemId") itemId: String
    ): Response<ApiResponse<Unit>>

    // ── Analytics and Tracking APIs ────────────────────────────
    @POST(ApiEndPoints.TRACK_EVENT)
    suspend fun trackEvent(@Body eventData: Map<String, Any>): Response<ApiResponse<AnalyticsEventResponse>>

    @POST(ApiEndPoints.TRACK_PROPERTY_VIEW)
    suspend fun trackPropertyView(@Body viewData: Map<String, Any>): Response<ApiResponse<AnalyticsEventResponse>>

    // ── Host Management APIs (iOS parity) ───────────────────
    @GET(ApiEndPoints.HOST_GET_LISTINGS)
    suspend fun getHostListings(
        @Header("Authorization") token: String
    ): Response<ApiListResponse<HostListingResponse>>

    @POST(ApiEndPoints.HOST_CREATE_LISTING)
    suspend fun createHostListing(
        @Header("Authorization") token: String,
        @Body listingData: Map<String, Any>
    ): Response<ApiResponse<HostListingResponse>>

    @PUT(ApiEndPoints.HOST_UPDATE_LISTING)
    suspend fun updateHostListing(
        @Header("Authorization") token: String,
        @Path("listingId") listingId: String,
        @Body listingData: Map<String, Any>
    ): Response<ApiResponse<HostListingResponse>>

    @DELETE(ApiEndPoints.HOST_DELETE_LISTING)
    suspend fun deleteHostListing(
        @Header("Authorization") token: String,
        @Path("listingId") listingId: String
    ): Response<ApiResponse<Unit>>

    @GET(ApiEndPoints.HOST_GET_EARNINGS)
    suspend fun getHostEarnings(
        @Header("Authorization") token: String
    ): Response<ApiResponse<HostEarningsResponse>>

    @GET(ApiEndPoints.HOST_GET_BOOKINGS)
    suspend fun getHostBookings(
        @Header("Authorization") token: String
    ): Response<ApiListResponse<BookingResponse>>

    @POST(ApiEndPoints.HOST_ACCEPT_BOOKING)
    suspend fun acceptHostBooking(
        @Header("Authorization") token: String,
        @Path("bookingId") bookingId: String
    ): Response<ApiResponse<BookingResponse>>

    @POST(ApiEndPoints.HOST_DECLINE_BOOKING)
    suspend fun declineHostBooking(
        @Header("Authorization") token: String,
        @Path("bookingId") bookingId: String
    ): Response<ApiResponse<BookingResponse>>

    @GET(ApiEndPoints.HOST_GET_ANALYTICS)
    suspend fun getHostAnalytics(
        @Header("Authorization") token: String
    ): Response<ApiResponse<HostAnalyticsResponse>>

    // ── Verification APIs ───────────────────────────────────
    @POST(ApiEndPoints.VERIFICATION_SEND_PHONE_CODE)
    suspend fun sendPhoneVerificationCode(
        @Header("Authorization") token: String,
        @Body request: PhoneVerificationRequest
    ): Response<ApiResponse<PhoneVerificationResponse>>

    @POST(ApiEndPoints.VERIFICATION_CONFIRM_PHONE)
    suspend fun confirmPhoneVerification(
        @Header("Authorization") token: String,
        @Body request: PhoneConfirmRequest
    ): Response<ApiResponse<PhoneConfirmResponse>>

    @GET(ApiEndPoints.VERIFICATION_STATUS)
    suspend fun getVerificationStatus(
        @Header("Authorization") token: String
    ): Response<ApiResponse<VerificationStatusResponse>>

    @POST(ApiEndPoints.VERIFICATION_UPLOAD_URLS)
    suspend fun getVerificationUploadUrls(
        @Header("Authorization") token: String,
        @Body request: UploadURLsRequest
    ): Response<ApiResponse<UploadURLsResponse>>

    @POST(ApiEndPoints.VERIFICATION_SUBMIT)
    suspend fun submitVerification(
        @Header("Authorization") token: String,
        @Body request: SubmitVerificationRequest
    ): Response<ApiResponse<SubmitVerificationResponse>>
}