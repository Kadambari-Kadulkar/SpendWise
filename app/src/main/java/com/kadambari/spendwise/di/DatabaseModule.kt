package com.kadambari.spendwise.di

import android.content.Context
import androidx.room.Room
import com.kadambari.spendwise.data.local.dao.TransactionDao
import com.kadambari.spendwise.data.local.database.SpendWiseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSpendWiseDatabase(
        @ApplicationContext applicationContext: Context,
    ): SpendWiseDatabase = Room.databaseBuilder(
        applicationContext,
        SpendWiseDatabase::class.java,
        "spendwise.db",
    ).build()

    @Provides
    fun provideTransactionDao(database: SpendWiseDatabase): TransactionDao =
        database.transactionDao()
}
