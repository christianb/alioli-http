package com.sensorberg.aliolihttp

import android.content.Context
import androidx.work.ListenableWorker
import com.sensorberg.aliolihttp.storage.AlioliHttpDatabaseProvider
import com.sensorberg.aliolihttp.storage.AlioliHttpDao
import com.sensorberg.aliolihttp.storage.AlioliHttpDatabase
import com.sensorberg.aliolihttp.storage.entity.AlioliHttpEntity
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import okhttp3.OkHttpClient
import okhttp3.Response
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test

class AlioliHttpWorkerTest {

	private val context: Context = mockk()
	private val okHttpClient: OkHttpClient = mockk()

	private lateinit var classToTest: AlioliHttpWorker

	@Before
	fun setUp() {
		mockkObject(AlioliHttpClientProvider)
		every { AlioliHttpClientProvider.createOkHttpClient() } returns okHttpClient

		classToTest = AlioliHttpWorker(context, mockk())
	}

	@Test
	fun `doWork should return success when no items available in DB`() {
		val dao: AlioliHttpDao = mockDao {
			every { getAll() } returns emptyList()
		}

		val result: ListenableWorker.Result = classToTest.doWork()

		assertThat(result).isEqualTo(ListenableWorker.Result.Success())
		verify(exactly = 0) { dao.delete(any()) }
	}

	@Test
	fun `doWork should return retry items are still available in DB after processing`() {
		mockDao {
			val entity: AlioliHttpEntity = mockEntity()
			every { getAll() } returns listOf(entity)
			every { size() } returns 1
		}

		val response: Response = mockk {
			every { isSuccessful } returns false
		}
		every { okHttpClient.newCall(any()).execute() } returns response

		val result: ListenableWorker.Result = classToTest.doWork()

		assertThat(result).isEqualTo(ListenableWorker.Result.Retry())
	}

	@Test
	fun `doWork should delete entity from DB when when not valid anymore`() {
		val id = 74L
		val entity: AlioliHttpEntity = mockEntity(valid = false, newId = id)
		val dao: AlioliHttpDao = mockDao(relaxed = true) {
			every { getAll() } returns listOf(entity)
		}

		mockResponse(successful = true)

		classToTest.doWork()

		verify { dao.delete(id) }
		verify(exactly = 0) { okHttpClient.newCall(any()).execute() }
	}

	@Test
	fun `doWork should delete entity from DB when response successful`() {
		val id = 42L
		val entity: AlioliHttpEntity = mockEntity(newId = id)
		val dao: AlioliHttpDao = mockDao(relaxed = true) {
			every { getAll() } returns listOf(entity)
		}

		mockResponse(successful = true)

		classToTest.doWork()

		verify { dao.delete(id) }
	}

	@Test
	fun `doWork must not delete entity from DB when response failed`() {
		val dao: AlioliHttpDao = mockDao(relaxed = true) {
			every { getAll() } returns listOf(mockEntity())
		}

		mockResponse(successful = false)

		classToTest.doWork()

		verify(exactly = 0) { dao.delete(any()) }
	}

	@Test
	fun `doWork must not delete entity from DB when response throws exception`() {
		val dao: AlioliHttpDao = mockDao(relaxed = true)
		every { okHttpClient.newCall(any()).execute() } throws Exception()

		classToTest.doWork()

		verify(exactly = 0) { dao.delete(any()) }
	}

	private fun mockResponse(successful: Boolean): Response {
		val response: Response = mockk {
			every { isSuccessful } returns successful
		}

		every { okHttpClient.newCall(any()).execute() } returns response

		return response
	}

	private fun mockEntity(valid: Boolean = true, newId: Long = 1): AlioliHttpEntity = mockk {
		every { isValid() } returns valid
		every { alioliHttpHeaderList } returns arrayListOf()
		every { url } returns "https://sensorberg.com/"
		every { method } returns "GET"
		every { alioliHttpBody } returns null
		every { id } returns newId
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
}