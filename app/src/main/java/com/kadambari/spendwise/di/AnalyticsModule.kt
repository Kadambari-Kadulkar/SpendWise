package com.kadambari.spendwise.di

import com.kadambari.spendwise.domain.repository.TransactionRepository
import com.kadambari.spendwise.domain.usecase.GetAnalyticsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.ZoneId

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    fun provideGetAnalyticsUseCase(
        repository: TransactionRepository,
        clock: Clock,
        zoneId: ZoneId,
    ): GetAnalyticsUseCase = GetAnalyticsUseCase(repository, clock, zoneId)
}
