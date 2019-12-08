package com.sensorberg.aliolihttp.storage

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.sensorberg.aliolihttp.storage.entity.AlioliHttpBody
import com.sensorberg.aliolihttp.storage.entity.AlioliHttpEntity
import com.sensorberg.aliolihttp.storage.entity.AlioliHttpHeader
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {

	private lateinit var database: AlioliHttpDatabase

	@Before
	fun setUp() {
		val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

		database = Room.inMemoryDatabaseBuilder(context, AlioliHttpDatabase::class.java).build()

		mockkObject(AlioliHttpDatabaseProvider)
		every { AlioliHttpDatabaseProvider.getDatabase(context) } returns database
	}

	@After
	fun tearDown() {
		database.clearAllTables()
		database.close()
	}

	@Test
	fun verify_entry_is_in_db_and_has_same_id() {
		val entity: AlioliHttpEntity = createEntity()
		val id: Long = getDao().insert(entity)

		val result: AlioliHttpEntity = getDao().getAll().first()

		assertEquals("id must be same", id, result.id)
	}

	@Test
	fun verify_entry_is_in_db_and_has_same_method() {
		val method = "GET"
		val entity: AlioliHttpEntity = createEntity(method = method)
		getDao().insert(entity)

		val result: AlioliHttpEntity = getDao().getAll().first()

		assertEquals("method must be same", method, result.method)
	}

	@Test
	fun verify_entry_is_in_db_and_has_same_body() {
		val body = "body"
		val contentType = "contentType"
		val entity: AlioliHttpEntity = createEntity(alioliHttpBody = AlioliHttpBody(body, contentType))
		getDao().insert(entity)

		val resultAlioliHttpBody: AlioliHttpBody? = getDao().getAll().first().alioliHttpBody

		assertEquals("body must be same", body, resultAlioliHttpBody?.body)
		assertEquals("contentType must be same", contentType, resultAlioliHttpBody?.contentType)
	}

	@Test
	fun verify_entry_is_in_db_and_has_same_url() {
		val url = "https://sensorberg.com"
		val entity: AlioliHttpEntity = createEntity(url = url)
		getDao().insert(entity)

		val result: AlioliHttpEntity = getDao().getAll().first()

		assertEquals("url must be same", url, result.url)
	}

	@Test
	fun verify_entry_is_in_db_and_has_same_validUntil() {
		val validUntilMs = 42L
		val entity: AlioliHttpEntity = createEntity(validUntilMs = validUntilMs)
		getDao().insert(entity)

		val result: AlioliHttpEntity = getDao().getAll().first()

		assertEquals("validUntil must be same", validUntilMs, result.validUntilMs)
	}

	@Test
	fun verify_entry_is_in_db_and_has_same_headers() {
		val key = "key"
		val value = "value"
		getDao().insert(createEntity(headers = listOf(key to value)))

		val result: AlioliHttpEntity = getDao().getAll().first()

		val alioliHttpHeaderEntity: AlioliHttpHeader = result.alioliHttpHeaderList.first()
		assertEquals("key must be same", key, alioliHttpHeaderEntity.key)
		assertEquals("value must be same", value, alioliHttpHeaderEntity.value)
	}

	@Test
	fun db_must_delete_item() {
		val dao: AlioliHttpDao = getDao()
		dao.insert(createEntity())
		dao.delete(dao.getAll().first().id)

		assertTrue("database must be empty", dao.getAll().isEmpty())
		assertEquals("size must be zero", 0, dao.size())
	}

	private fun createEntity(method: String = "",
							 url: String = "",
							 alioliHttpBody: AlioliHttpBody? = null,
							 headers: List<Pair<String, String>> = emptyList(),
							 validUntilMs: Long = 0,
							 id: Long = 0): AlioliHttpEntity {
		return AlioliHttpEntity(method, url, alioliHttpBody, toHeaderEntityList(headers), validUntilMs, id)
	}

	private fun toHeaderEntityList(headerList: List<Pair<String, String>>): ArrayList<AlioliHttpHeader> {
		val arrayList: ArrayList<AlioliHttpHeader> = arrayListOf()

		headerList.forEach { pair ->
			arrayList.add(AlioliHttpHeader(pair.first, pair.second))
		}

		return arrayList
	}

	private fun getDao(): AlioliHttpDao = database.getAlioliHttpDao()
}