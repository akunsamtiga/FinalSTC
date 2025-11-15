package com.autotrade.finalstc.di

import com.autotrade.finalstc.data.api.CurrencyApiService
import com.autotrade.finalstc.data.local.SessionManager
import com.autotrade.finalstc.data.repository.TradingHistoryRepository
import com.autotrade.finalstc.data.repository.CurrencyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.autotrade.finalstc.data.repository.FirebaseRepository
import com.google.firebase.firestore.FirebaseFirestore

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

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

}