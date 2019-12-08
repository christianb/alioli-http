package com.sensorberg.aliolihttp.storage.entity

internal data class AlioliHttpHeader(
	val key: String,
	val value: String) {

	override fun toString(): String {
		return "$key: $value"
	}
}