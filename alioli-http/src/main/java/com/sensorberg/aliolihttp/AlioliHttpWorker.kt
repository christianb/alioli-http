package com.sensorberg.aliolihttp

import android.content.Context
import androidx.work.*
import com.sensorberg.aliolihttp.storage.entity.AlioliHttpEntity
import okhttp3.*
import timber.log.Timber
import com.sensorberg.aliolihttp.storage.AlioliHttpDatabaseProvider
import com.sensorberg.aliolihttp.storage.AlioliHttpDao
import com.sensorberg.aliolihttp.storage.entity.AlioliHttpBody
import com.sensorberg.aliolihttp.storage.entity.AlioliHttpHeader
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class AlioliHttpWorker(private val context: Context,
								workerParameters: WorkerParameters) : Worker(context, workerParameters) {

	private val okHttpClient: OkHttpClient = AlioliHttpClientProvider.createOkHttpClient()

	override fun doWork(): Result {
		val alioliHttpDao: AlioliHttpDao = AlioliHttpDatabaseProvider.getDatabase(context).getAlioliHttpDao()
		val alioliHttpList: List<AlioliHttpEntity> = alioliHttpDao.getAll()

		if (alioliHttpList.isEmpty()) {
			Timber.d("no requests to execute")
			return Result.success()
		}

		alioliHttpList.forEach { alioliHttpEntity ->
			if (!alioliHttpEntity.isValid()) {
				Timber.d("alioliHttpEntity not valid anymore, delete from DB")
				alioliHttpDao.delete(alioliHttpEntity.id)
				return@forEach
			}

			val successful: Boolean = safelyExecute(alioliHttpEntity)
			if (successful) {
				Timber.d("request successful, delete entry in db")
				alioliHttpDao.delete(alioliHttpEntity.id)
			}
		}

		if (alioliHttpDao.size() > 0) {
			Timber.d("db still contains requests, retry later")
			return Result.retry()
		}

		return Result.success()
	}

	@Suppress("SwallowedException")
	private fun safelyExecute(alioliHttpEntity: AlioliHttpEntity): Boolean {
		val request: Request = createRequestFrom(alioliHttpEntity)

		try {
			return okHttpClient.newCall(request).execute().isSuccessful
		} catch (_: IOException) {
			// ignored
		}

		return false
	}

	private fun createRequestFrom(entity: AlioliHttpEntity): Request {
		return Request.Builder()
			.headers(toHeaders(entity.alioliHttpHeaderList))
			.url(entity.url)
			.method(entity.method, createRequestBodyFrom(entity))
			.build()
	}

	private fun toHeaders(alioliHttpHeaderList: List<AlioliHttpHeader>): Headers {
		val builder = Headers.Builder()

		alioliHttpHeaderList.forEach {
			Timber.d("header: $it")
			builder.add(it.key, it.value)
		}

		return builder.build()
	}

	private fun createRequestBodyFrom(entity: AlioliHttpEntity): RequestBody? {
		val alioliHttpBody: AlioliHttpBody = entity.alioliHttpBody ?: return null
		val body: String = alioliHttpBody.body
		val mediaType = MediaType.parse(alioliHttpBody.contentType)

		Timber.d("body: $body")
		Timber.d("contentType: $mediaType")

		return RequestBody.create(mediaType, body)
	}

	companion object {
		private val TAG: String = AlioliHttpWorker::class.java.simpleName

		private fun createWorkRequest(backoffDelay: Long,
									  timeUnit: TimeUnit): OneTimeWorkRequest {
			val constraints: Constraints = Constraints.Builder()
				.setRequiredNetworkType(NetworkType.CONNECTED)
				.build()

			return OneTimeWorkRequestBuilder<AlioliHttpWorker>()
				.setConstraints(constraints)
				.setBackoffCriteria(BackoffPolicy.LINEAR, backoffDelay, timeUnit)
				.build()
		}

		internal fun enqueue(backoffDelay: Long,
							 timeUnit: TimeUnit) {
			Timber.d("enqueue workRequest")
			val oneTimeWorkRequest: OneTimeWorkRequest = createWorkRequest(backoffDelay, timeUnit)

			AlioliHttpWorkManagerProvider.getWorkManager().enqueueUniqueWork(
					TAG,
					ExistingWorkPolicy.REPLACE,
					oneTimeWorkRequest)
		}
	}
}