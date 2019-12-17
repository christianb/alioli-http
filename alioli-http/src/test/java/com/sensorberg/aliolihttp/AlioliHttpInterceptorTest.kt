package com.sensorberg.aliolihttp

import android.content.Context
import com.sensorberg.aliolihttp.AlioliHttpConstant.HEADER_KEY_VALID_UNTIL
import com.sensorberg.aliolihttp.AlioliHttpConstant.HEADER_VALUE_MAX_1_DAY
import com.sensorberg.aliolihttp.AlioliHttpConstant.HEADER_VALUE_MAX_1_MONTH
import com.sensorberg.aliolihttp.AlioliHttpConstant.HEADER_VALUE_MAX_1_WEEK
import com.sensorberg.aliolihttp.AlioliHttpConstant.HEADER_VALUE_MAX_2_WEEKS
import com.sensorberg.aliolihttp.storage.AlioliHttpDatabaseProvider
import com.sensorberg.aliolihttp.storage.AlioliHttpDao
import com.sensorberg.aliolihttp.storage.AlioliHttpDatabase
import com.sensorberg.aliolihttp.storage.entity.*
import io.mockk.*
import okhttp3.*
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class AlioliHttpInterceptorTest {

	private val context: Context = mockk()

	private lateinit var classToTest: AlioliHttpInterceptor

	@Before
	fun setUp() {
		classToTest = AlioliHttpInterceptor(context)

		mockkObject(AlioliHttpWorkManagerProvider)
		every { AlioliHttpWorkManagerProvider.getWorkManager() } returns mockk(relaxed = true)
	}

	@Test
	fun `intercept should return response when request is not deferrable`() {
		val request: Request = mockk {
			every { headers() } returns createHeaders(emptyMap())
		}
		val response: Response = mockResponse(successful = true)
		val chain: Interceptor.Chain = mockChain(request, response)
		val dao: AlioliHttpDao = mockDao()

		val result: Response = classToTest.intercept(chain)

		assertThat(result).isEqualTo(response)
		verify(exactly = 0) { dao.insert(any()) }
		verify(exactly = 0) { dao.delete(any()) }
	}

	@Test
	fun `intercept should add request to DB when its deferrable and response failed`() {
		val request: Request = mockRequest()
		val response: Response = mockResponse(successful = false)
		val chain: Interceptor.Chain = mockChain(request, response)
		val dao: AlioliHttpDao = mockDao(relaxed = true)

		val result: Response = classToTest.intercept(chain)

		assertThat(result).isEqualTo(response)
		verify { dao.insert(any()) }
	}

	@Test
	fun `intercept should add request to DB when its deferrable and exception happened while executing response`() {
		val request: Request = mockRequest()
		val chain: Interceptor.Chain = mockk {
			every { request() } returns request
			every { proceed(request) } throws Exception()
		}
		val dao: AlioliHttpDao = mockDao(relaxed = true)

		try {
			classToTest.intercept(chain)
		} catch (e: java.lang.Exception) {
			// ignore thrown exception
		}

		verify(exactly = 1) { dao.insert(any()) }
	}

	@Test
	fun `intercept should create entity with correct method`() {
		val method = "method"
		val request: Request = mockRequest(requestMethod = method)
		val response: Response = mockResponse(successful = false)
		val chain: Interceptor.Chain = mockChain(request, response)
		val dao: AlioliHttpDao = mockDao(relaxed = true)

		val result: Response = classToTest.intercept(chain)

		assertThat(result).isEqualTo(response)
		verify {
			dao.insert(match { entity ->
				entity.method == method
			})
		}
	}

	@Test
	fun `intercept should create entity with correct url`() {
		val url = "url"
		val request: Request = mockRequest(requestUrl = url)
		val response: Response = mockResponse(successful = false)
		val chain: Interceptor.Chain = mockChain(request, response)
		val dao: AlioliHttpDao = mockDao(relaxed = true)

		val result: Response = classToTest.intercept(chain)

		assertThat(result).isEqualTo(response)
		verify {
			dao.insert(match { entity ->
				entity.url == url
			})
		}
	}

	@Test
	fun `intercept should create entity with correct body`() {
		val body = "body"
		val requestBody: RequestBody = RequestBody.create(MediaType.get("text/plain"), body)
		val request: Request = mockRequest(requestBody = requestBody)
		val response: Response = mockResponse(successful = false)
		val chain: Interceptor.Chain = mockChain(request, response)
		val dao: AlioliHttpDao = mockDao(relaxed = true)

		val result: Response = classToTest.intercept(chain)

		assertThat(result).isEqualTo(response)
		verify {
			dao.insert(match { entity ->
				entity.alioliHttpBody?.body == body
			})
		}
	}

	@Test
	fun `intercept should create entity with correct headers`() {
		val headerMap: Map<String, String> = mapOf(HEADER_KEY_VALID_UNTIL to "valid",
												   "key2" to "value2")
		val request: Request = mockRequest(requestHeaders = headerMap)
		val response: Response = mockResponse(successful = false)
		val chain: Interceptor.Chain = mockChain(request, response)
		val dao: AlioliHttpDao = mockDao(relaxed = true)

		val result: Response = classToTest.intercept(chain)

		assertThat(result).isEqualTo(response)
		verify {
			dao.insert(match { entity ->
				entity.alioliHttpHeaderList.containsAll(headerMap)
			})
		}
	}

	@Test
	fun `intercept should enqueue worker only when response is not successful`() {
		val backoffDelay = 78L
		val timeUnit = TimeUnit.SECONDS
		val chain: Interceptor.Chain = mockChain(mockRequest(), mockResponse(successful = false))
		mockDao(relaxed = true)
		mockkObject(AlioliHttpWorker)

		AlioliHttpInterceptor(context, backoffDelay, timeUnit).intercept(chain)

		verify { AlioliHttpWorker.enqueue(backoffDelay, timeUnit) }
	}

	@Test
	fun `intercept should create entity with almost correct ONE_DAY validUntil timestamp`() {
		verifyValidUntil(headerMap = mapOf(HEADER_KEY_VALID_UNTIL to HEADER_VALUE_MAX_1_DAY.toString()),
						 validUntilMs = ONE_DAY_MS)
	}

	@Test
	fun `intercept should create entity with almost correct ONE_WEEK validUntil timestamp`() {
		verifyValidUntil(headerMap = mapOf(HEADER_KEY_VALID_UNTIL to HEADER_VALUE_MAX_1_WEEK.toString()),
						 validUntilMs = ONE_WEEK_MS)
	}

	@Test
	fun `intercept should create entity with almost correct TWO_WEEKS validUntil timestamp`() {
		verifyValidUntil(headerMap = mapOf(HEADER_KEY_VALID_UNTIL to HEADER_VALUE_MAX_2_WEEKS.toString()),
						 validUntilMs = TWO_WEEKS_MS)
	}

	@Test
	fun `intercept should create entity with almost correct ONE_MONTH validUntil timestamp`() {
		verifyValidUntil(headerMap = mapOf(HEADER_KEY_VALID_UNTIL to HEADER_VALUE_MAX_1_MONTH.toString()),
						 validUntilMs = ONE_MONTH_MS)
	}

	@Test
	fun `intercept should create entity with almost correct ONE_WEEK_MS validUntil timestamp when header is not set`() {
		verifyValidUntil(headerMap = mapOf(HEADER_KEY_VALID_UNTIL to "anything"),
						 validUntilMs = ONE_WEEK_MS)
	}

	@Test
	@Suppress("SwallowedException")
	fun `intercept must enqueue worker when exception is thrown while proceeding request`() {
		val backoffDelay = 78L
		val timeUnit = TimeUnit.SECONDS
		val request: Request = mockRequest()
		val chain: Interceptor.Chain = mockk {
			every { request() } returns request
			every { proceed(request) } throws Exception()
		}
		mockDao(relaxed = true)
		mockkObject(AlioliHttpWorker)

		try {
			AlioliHttpInterceptor(context, backoffDelay, timeUnit).intercept(chain)
		} catch (_: Exception) {
			// ignored
		}

		verify { AlioliHttpWorker.enqueue(backoffDelay, timeUnit) }
	}

	private fun verifyValidUntil(headerMap: Map<String, String>,
								 validUntilMs: Long) {
		val request: Request = mockRequest(requestHeaders = headerMap)
		val response: Response = mockResponse(successful = false)
		val chain: Interceptor.Chain = mockChain(request, response)
		val dao: AlioliHttpDao = mockDao(relaxed = true)

		val result: Response = classToTest.intercept(chain)

		assertThat(result).isEqualTo(response)
		verify {
			dao.insert(match { entity ->
				isTimeNearlyValid(validUntilMs, entity.validUntilMs, ONE_MIN_MS)
			})
		}
	}

	private fun isTimeNearlyValid(plusMs: Long,
								  resultMs: Long,
								  thresholdMs: Long): Boolean {
		val currentMs: Long = System.currentTimeMillis()

		val min: Long = currentMs + plusMs - thresholdMs
		val max: Long = currentMs + plusMs + thresholdMs

		return resultMs in (min..max)

	}

	private fun mockDao(relaxed: Boolean = false, callback: AlioliHttpDao.() -> Unit = {}): AlioliHttpDao {
		val dao: AlioliHttpDao = mockk(relaxed = relaxed)
		val database: AlioliHttpDatabase = mockk {
			every { getAlioliHttpDao() } returns dao
		}

		mockkObject(AlioliHttpDatabaseProvider)
		every { AlioliHttpDatabaseProvider.getDatabase(context) } returns database

		callback.invoke(dao)

		return dao
	}

	private fun mockChain(request: Request, response: Response): Interceptor.Chain {
		return mockk {
			every { request() } returns request
			every { proceed(request) } returns response
		}
	}

	private fun mockResponse(successful: Boolean): Response {
		return mockk {
			every { isSuccessful } returns successful
		}
	}

	private fun mockRequest(requestMethod: String = "method",
							requestUrl: String = "url",
							requestBody: RequestBody? = null,
							requestHeaders: Map<String, String> = mapOf(HEADER_KEY_VALID_UNTIL to "value")): Request {
		return mockk {
			val url: HttpUrl = mockk<HttpUrl>().also { url ->
				every { url.toString() } returns requestUrl
			}

			every { headers() } returns createHeaders(requestHeaders)
			every { method() } returns requestMethod
			every { body() } returns requestBody
			every { url() } returns url
		}
	}

	private fun createHeaders(headersMap: Map<String, String>): Headers {
		val builder = Headers.Builder()

		headersMap.forEach { (key: String, value: String) ->
			builder.add(key, value)
		}

		return builder.build()

	}

	private fun List<AlioliHttpHeader>.containsAll(map: Map<String, String>): Boolean {
		forEach { entity ->
			if (!map[entity.key].equals(entity.value)) {
				return false
			}
		}

		return true
	}

	companion object {
		private const val ONE_MIN_MS: Long = 1000 * 60
		private const val ONE_DAY_MS: Long = ONE_MIN_MS * 60 * 24
		private const val ONE_WEEK_MS: Long = ONE_DAY_MS * 7
		private const val TWO_WEEKS_MS: Long = ONE_DAY_MS * 14
		private const val ONE_MONTH_MS: Long = ONE_DAY_MS * 30
	}
}