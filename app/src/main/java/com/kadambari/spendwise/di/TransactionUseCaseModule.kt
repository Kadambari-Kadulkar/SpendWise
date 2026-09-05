package com.kadambari.spendwise.di

import com.kadambari.spendwise.domain.repository.TransactionRepository
import com.kadambari.spendwise.domain.usecase.AddTransactionUseCase
import com.kadambari.spendwise.domain.usecase.DeleteTransactionUseCase
import com.kadambari.spendwise.domain.usecase.GetTransactionUseCase
import com.kadambari.spendwise.domain.usecase.GetTransactionsUseCase
import com.kadambari.spendwise.domain.usecase.UpdateTransactionUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.ZoneId
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TransactionUseCaseModule {

    /*
     * Clock represents the current instant; ZoneId determines the user's
     * calendar date when a date is derived for the transaction form.
     */
    @Provides
    @Singleton
    fun provideTransactionClock(): Clock = Clock.systemUTC()

    @Provides
    @Singleton
    fun provideTransactionZoneId(): ZoneId = ZoneId.systemDefault()

    @Provides
    fun provideGetTransactionsUseCase(
        repository: TransactionRepository,
    ): GetTransactionsUseCase = GetTransactionsUseCase(repository)

    @Provides
    fun provideGetTransactionUseCase(
        repository: TransactionRepository,
    ): GetTransactionUseCase = GetTransactionUseCase(repository)

    @Provides
    fun provideAddTransactionUseCase(
        repository: TransactionRepository,
        clock: Clock,
        zoneId: ZoneId,
    ): AddTransactionUseCase = AddTransactionUseCase(repository, clock, zoneId)

    @Provides
    fun provideUpdateTransactionUseCase(
        repository: TransactionRepository,
        clock: Clock,
        zoneId: ZoneId,
    ): UpdateTransactionUseCase = UpdateTransactionUseCase(repository, clock, zoneId)

    @Provides
    fun provideDeleteTransactionUseCase(
        repository: TransactionRepository,
    ): DeleteTransactionUseCase = DeleteTransactionUseCase(repository)
}
