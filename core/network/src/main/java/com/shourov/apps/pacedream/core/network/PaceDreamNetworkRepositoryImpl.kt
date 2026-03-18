package com.shourov.apps.pacedream.core.network

import com.shourov.apps.pacedream.core.network.model.UpdatedProfileData
import com.shourov.apps.pacedream.core.network.model.auth.SignInRequestModel
import com.shourov.apps.pacedream.core.network.model.auth.SignupRequestModel
import com.shourov.apps.pacedream.model.SendOtpRequestModel
import javax.inject.Inject

class PaceDreamNetworkRepositoryImpl @Inject constructor(
    private val retrofitPaceDreamApiService: RetrofitPaceDreamApiService,
) : PaceDreamNetworkRepository {
    override suspend fun getAllProperties() {
        retrofitPaceDreamApiService.getAllProperties()
    }

    override suspend fun sendOtp(sendOtpRequestModel: SendOtpRequestModel) {
        retrofitPaceDreamApiService.sendOtp(sendOtpRequestModel)
    }

    override suspend fun signInRequest(signInRequestModel: SignInRequestModel) {
        retrofitPaceDreamApiService.signInRequest(signInRequestModel)
    }

    override suspend fun sendSignUpRequest(description: SignupRequestModel) {
        retrofitPaceDreamApiService.sendSignUpRequest(description)
    }

    override suspend fun getUserInformation(authorization: String) {
        retrofitPaceDreamApiService.getUserInformation(authorization)
    }

    override suspend fun updateUserInformation(authorization: String, aData: UpdatedProfileData) {
        retrofitPaceDreamApiService.updateUserInformation(authorization, aData)
    }

    override suspend fun getUserReviews(authorization: String) {
        retrofitPaceDreamApiService.getUserReviews(authorization)
    }

    override suspend fun getProfileAlreadyBookedList(authorization: String) {
        retrofitPaceDreamApiService.getProfileAlreadyBookedList(authorization)
    }
}

interface PaceDreamNetworkRepository {
    suspend fun getAllProperties()
    suspend fun sendOtp(sendOtpRequestModel: SendOtpRequestModel)
    suspend fun signInRequest(signInRequestModel: SignInRequestModel)
    suspend fun sendSignUpRequest(description: SignupRequestModel)
    suspend fun getUserInformation(authorization: String)
    suspend fun updateUserInformation(authorization: String, aData: UpdatedProfileData)
    suspend fun getUserReviews(authorization: String)
    suspend fun getProfileAlreadyBookedList(authorization: String)
}