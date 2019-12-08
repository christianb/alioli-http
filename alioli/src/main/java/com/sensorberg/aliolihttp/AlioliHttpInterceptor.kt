package com.sensorberg.aliolihttp

import android.content.Context
import com.sensorberg.aliolihttp.AlioliHttpConstant.HEADER_KEY_VALID_UNTIL
import com.sensorberg.aliolihttp.AlioliHttpConstant.HEADER_VALUE_MAX_1_WEEK
import com.sensorberg.aliolihttp.storage.AlioliHttpDao
import com.sensorberg.aliolihttp.storage.AlioliHttpDatabase
import com.sensorberg.aliolihttp.storage.AlioliHttpDatabaseProvider
import com.sensorberg.aliolihttp.storage.entity.AlioliHttpBody
import com.sensorberg.aliolihttp.storage.entity.AlioliHttpEntity
import com.sensorberg.aliolihttp.storage.entity.AlioliHttpHeader
import okhttp3.*
import okio.Buffer
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class AlioliHttpInterceptor(private val context: Context,
									 private val backoffDelay: Long = 15,
									 private val timeUnit: TimeUnit = TimeUnit.MINUTES) : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val request: Request = chain.request()
		if (!isDeferrable(request)) {
			return chain.proceed(request)
		}

		val database: AlioliHttpDatabase = AlioliHttpDatabaseProvider.getDatabase(context)
		val dao: AlioliHttpDao = database.getAlioliHttpDao()
		val id: Long = saveToDao(request, dao)

		return safelyProceedRequest(chain, request, dao, id)
	}

	private fun safelyProceedRequest(chain: Interceptor.Chain, request: Request, dao: AlioliHttpDao, idToDelete: Long): Response {
		try {
			val response: Response = chain.proceed(request)
			if (response.isSuccessful) {
				Timber.d("Alioli: response success ${request.url()}")
				dao.delete(idToDelete)
			} else {
				Timber.d("Alioli: response not success ${request.url()}. Enqueueing")
				AlioliHttpWorker.enqueue(backoffDelay, timeUnit)
			}
			return response
		} catch (exception: Throwable) {
			Timber.d("Alioli: response fail with ${exception.message} for ${request.url()}. Enqueueing")
			AlioliHttpWorker.enqueue(backoffDelay, timeUnit)
			throw exception
		}
	}

	private fun saveToDao(request: Request, dao: AlioliHttpDao): Long {
		val headers: Headers = request.headers()
		val alioliHttpEntity = AlioliHttpEntity(
				request.method(),
				request.url().toString(),
				toAlioliHttpBody(request.body()),
				toHeaderEntityList(headers),
				headers.getValidUntilTimeStamp())

		return dao.insert(alioliHttpEntity)
	}

	private fun isDeferrable(request: Request): Boolean {
		return request.headers().contains(HEADER_KEY_VALID_UNTIL)
	}

	private fun Headers.contains(headerKey: String): Boolean {
		return get(headerKey) != null
	}

	private fun toAlioliHttpBody(requestBody: RequestBody?): AlioliHttpBody? {
		val bodyString: String = toString(requestBody) ?: return null
		val contentType: String = requestBody?.contentType()?.toString() ?: return null

		return AlioliHttpBody(bodyString, contentType)
	}

	@Suppress("SwallowedException")
	private fun toString(requestBody: RequestBody?): String? {
		requestBody ?: return null

		return try {
			val buffer = Buffer()
			requestBody.writeTo(buffer)
			buffer.readUtf8()
		} catch (_: IOException) {
			null
		}
	}

	private fun toHeaderEntityList(headers: Headers): ArrayList<AlioliHttpHeader> {
		val list: ArrayList<AlioliHttpHeader> = arrayListOf()

		headers.names().forEach { name ->
			if (isCustomHeader(name)) {
				// we do not wanna save our custom header, so skip it
				return@forEach
			}

			headers.values(name).forEach { value ->
				list.add(AlioliHttpHeader(name, value))
			}
		}

		return list
	}

	private fun isCustomHeader(key: String): Boolean {
		return key == HEADER_KEY_VALID_UNTIL
	}

	private fun Headers.getValidUntilTimeStamp(): Long {
		val value: Long? = values(HEADER_KEY_VALID_UNTIL).firstOrNull()?.toLongOrNull()

		val currentMs: Long = System.currentTimeMillis()
		if (value == null) {
			return currentMs + HEADER_VALUE_MAX_1_WEEK
		}

		return currentMs + value
	}
}
