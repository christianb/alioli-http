package com.sensorberg.aliolihttp

import android.content.Context
import okhttp3.Interceptor

object AlioliHttpInterceptorFactory {

	/**
	 * The returned interceptor must be set at the end of your OkHttp application interceptor list.
	 */
	fun create(context: Context): Interceptor {
		return AlioliHttpInterceptor(context)
	}
}