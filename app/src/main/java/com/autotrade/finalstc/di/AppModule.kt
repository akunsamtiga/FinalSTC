package com.autotrade.finalstc.di

import com.autotrade.finalstc.data.api.BalanceApiService
import com.autotrade.finalstc.data.api.LoginApiService
import com.autotrade.finalstc.data.api.UserProfileApiService
import com.autotrade.finalstc.data.api.CurrencyApiService
import com.autotrade.finalstc.data.local.SessionManager
import com.autotrade.finalstc.data.repository.TradingHistoryRepository
import com.autotrade.finalstc.data.repository.CurrencyRepository
import com.autotrade.finalstc.data.repository.FirebaseRepository
import com.autotrade.finalstc.data.repository.ProfileRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val BASE_URL = "https://api.stockity.id/"

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return Firebase.firestore
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(
                okhttp3.ConnectionPool(
                    maxIdleConnections = 5,
                    keepAliveDuration = 5,
                    timeUnit = TimeUnit.MINUTES
                )
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideLoginApiService(retrofit: Retrofit): LoginApiService {
        return retrofit.create(LoginApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideUserProfileApiService(retrofit: Retrofit): UserProfileApiService {
        return retrofit.create(UserProfileApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideCurrencyApiService(retrofit: Retrofit): CurrencyApiService {
        return retrofit.create(CurrencyApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideBalanceApiService(retrofit: Retrofit): BalanceApiService {
        return retrofit.create(BalanceApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTradingHistoryRepository(
        sessionManager: SessionManager
    ): TradingHistoryRepository {
        return TradingHistoryRepository(sessionManager)
    }

    @Provides
    @Singleton
    fun provideCurrencyRepository(
        currencyApiService: CurrencyApiService,
        sessionManager: SessionManager
    ): CurrencyRepository {
        return CurrencyRepository(currencyApiService, sessionManager)
    }

    @Provides
    @Singleton
    fun provideFirebaseRepository(
        firestore: FirebaseFirestore
    ): FirebaseRepository {
        return FirebaseRepository(firestore)
    }

    @Provides
    @Singleton
    fun provideProfileRepository(
        apiService: UserProfileApiService,
        sessionManager: SessionManager
    ): ProfileRepository {
        return ProfileRepository(apiService, sessionManager)
    }
}