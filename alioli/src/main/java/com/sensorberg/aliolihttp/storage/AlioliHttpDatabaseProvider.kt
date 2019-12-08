package com.sensorberg.aliolihttp.storage

import android.content.Context
import androidx.room.Room

internal object AlioliHttpDatabaseProvider {

	private var alioliHttpDatabase: AlioliHttpDatabase? = null

	@Synchronized fun getDatabase(context: Context): AlioliHttpDatabase {
		if (alioliHttpDatabase == null) {
			alioliHttpDatabase = Room.databaseBuilder(
					context,
					AlioliHttpDatabase::class.java,
					ALIOLI_DATABASE_NAME).build()
		}

		return alioliHttpDatabase!!
	}
}