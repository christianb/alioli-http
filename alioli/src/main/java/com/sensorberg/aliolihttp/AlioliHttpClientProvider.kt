package com.sensorberg.aliolihttp

import okhttp3.OkHttpClient

internal object AlioliHttpClientProvider {

	fun createOkHttpClient(): OkHttpClient = OkHttpClient()
}