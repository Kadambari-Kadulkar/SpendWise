package com.kadambari.spendwise.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kadambari.spendwise.data.local.dao.TransactionDao

@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SpendWiseDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
}
