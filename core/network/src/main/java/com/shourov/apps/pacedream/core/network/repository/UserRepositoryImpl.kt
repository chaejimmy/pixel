package com.shourov.apps.pacedream.core.network.repository

import com.shourov.apps.pacedream.core.network.model.ApiResult
import com.shourov.apps.pacedream.core.network.model.wrapIntoApiResult
import com.shourov.apps.pacedream.core.network.services.PaceDreamApiService
import com.shourov.apps.pacedream.model.request.ForgotPasswordRequest
import com.shourov.apps.pacedream.model.request.GetOTPRequest
import com.shourov.apps.pacedream.model.request.LoginRequest
import com.shourov.apps.pacedream.model.request.LoginWithEmailRequest
import com.shourov.apps.pacedream.model.request.SendEmailCodeRequest
import com.shourov.apps.pacedream.model.request.SignUpRequest
import com.shourov.apps.pacedream.model.request.SignUpRequestEmail
import com.shourov.apps.pacedream.model.request.VerifyEmailRequest
import com.shourov.apps.pacedream.model.response.auth.ForgotPasswordResponse
import com.shourov.apps.pacedream.model.response.auth.LoginResponse
import com.shourov.apps.pacedream.model.response.auth.MobileOTPResponse
import com.shourov.apps.pacedream.model.response.auth.UserProfileResponse
import com.shourov.apps.pacedream.model.response.home.rooms.RoomsResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class UserRepositoryImpl(
    private val apiService: PaceDreamApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : UserRepository {
    override suspend fun signInUser(request: LoginRequest): ApiResult<LoginResponse> {
        val result = wrapIntoApiResult(dispatcher) {
            apiService.loginUser(mobile = request.mobile, otp = request.otp)
        }
        if (result is ApiResult.Success) {
            val user = result.value.user
//            storage.authorization = user.token
//            storage.user = user
        }
        return result
    }

    override suspend fun signInUserEmail(request: LoginWithEmailRequest): ApiResult<LoginResponse> {
        val result = wrapIntoApiResult(dispatcher) {
            apiService.loginUserEmail(email = request.email, password = request.password)
        }
        if (result is ApiResult.Success) {
            val user = result.value.user
//            storage.authorization = user.token
//            storage.user = user
        }
        return result
    }

    override suspend fun forgotPassword(request: ForgotPasswordRequest): ApiResult<ForgotPasswordResponse> {
        val result = wrapIntoApiResult(dispatcher) {
            apiService.forgotPassword(
                request,
            )
        }

        return result
    }

    override suspend fun sendOTP(request: GetOTPRequest): ApiResult<MobileOTPResponse> {
        val result = wrapIntoApiResult(dispatcher) {
            apiService.sendOtp(mobile = request.mobile)
        }
        return result
    }

    override suspend fun signUpUser(request: SignUpRequest): ApiResult<MobileOTPResponse> {
        val result = wrapIntoApiResult(dispatcher) {
            apiService.registerUser(
                dob = request.dob,
                firstName = request.firstName,
                lastName = request.lastName,
                gender = request.gender,
                password = request.password,
                phoneNumber = request.phoneNumber,
            )
        }
//        if (result is ApiResult.Success) {
//            val user = result.value.user
//            storage.authorization = user?.token
//            storage.user = user
//        }
        return result
    }

    override suspend fun signUpUserEmail(request: SignUpRequestEmail): ApiResult<LoginResponse> {
        val result = wrapIntoApiResult(dispatcher) {
            apiService.registerUserEmail(
                dob = request.dob,
                firstName = request.firstName,
                lastName = request.lastName,
                gender = request.gender,
                password = request.password,
                email = request.email,
            )
        }

        if (result is ApiResult.Success) {
            val user = result.value.user
//            storage.authorization = user.token
//            storage.user = user
        }
        return result
    }

    override suspend fun sendVerificationCodeEmail(request: SendEmailCodeRequest): ApiResult<LoginResponse> {
        val result = wrapIntoApiResult(dispatcher) {
            apiService.sendEmailCode(
                email = request.email,
            )
        }

        return result
    }

    override suspend fun verifyEmail(request: VerifyEmailRequest): ApiResult<LoginResponse> {
        val result = wrapIntoApiResult(dispatcher) {
            apiService.verifyEmailCode(
                email = request.email,
                code = request.code,
            )
        }

        return result
    }

    override suspend fun getUserInfo(): ApiResult<UserProfileResponse> {
        val result = wrapIntoApiResult(dispatcher) {
            apiService.getUserInformation()
        }
        return result
    }

    override suspend fun getRoomStayAll(): ApiResult<RoomsResponse> {
        val result = wrapIntoApiResult(dispatcher) {
            apiService.getRoomStayAll()
        }
        return result
    }
}