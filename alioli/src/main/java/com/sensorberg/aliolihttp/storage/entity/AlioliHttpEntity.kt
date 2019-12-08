package com.sensorberg.aliolihttp.storage.entity

import androidx.room.*

internal const val ALIOLI_HTTP_ENTITY_TABLE_NAME = "alioli_http_request"

@Entity(tableName = ALIOLI_HTTP_ENTITY_TABLE_NAME)
@TypeConverters(AlioliHttpHeaderTypeConverter::class)
internal data class AlioliHttpEntity(
	val method: String,
	val url: String,
	@Embedded val alioliHttpBody: AlioliHttpBody?,
	val alioliHttpHeaderList: ArrayList<AlioliHttpHeader>,
	val validUntilMs: Long, // point in time in future until this request should be retried
	@PrimaryKey(autoGenerate = true) val id: Long = 0) {

	fun isValid(): Boolean {
		return System.currentTimeMillis() < validUntilMs
	}
}