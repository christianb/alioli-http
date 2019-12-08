package com.sensorberg.aliolihttp.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sensorberg.aliolihttp.storage.entity.AlioliHttpEntity

internal const val ALIOLI_DATABASE_NAME = "alioli_http_database"

@Database(entities = [AlioliHttpEntity::class], version = 1, exportSchema = false)
internal abstract class AlioliHttpDatabase : RoomDatabase() {
	abstract fun getAlioliHttpDao(): AlioliHttpDao
}