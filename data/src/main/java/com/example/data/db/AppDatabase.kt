package com.example.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.dao.UserDao
import com.example.data.entity.UserEntity

@Database(entities = [UserEntity::class], version = 1,exportSchema = false)
abstract class AppDatabase: RoomDatabase() {
  abstract fun userDao(): UserDao
}