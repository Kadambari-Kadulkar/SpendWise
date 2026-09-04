package com.kadambari.spendwise.di

import com.kadambari.spendwise.data.repository.TransactionRepositoryImpl
import com.kadambari.spendwise.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        implementation: TransactionRepositoryImpl,
    ): TransactionRepository
}
