package com.sensorberg.aliolihttp.storage.entity

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

internal class AlioliHttpHeaderTypeConverter {

	private val gson = Gson()
	private val type: Type = object : TypeToken<ArrayList<AlioliHttpHeader>>() {}.type

	@TypeConverter
	fun fromHeaderEntity(alioliHttpHeaderList: ArrayList<AlioliHttpHeader>): String {
		return gson.toJson(alioliHttpHeaderList, type)
	}

	@TypeConverter
	fun fromString(string: String): ArrayList<AlioliHttpHeader> {
		if (string.isBlank()) {
			return arrayListOf()
		}

		return gson.fromJson(string, type)
	}
}