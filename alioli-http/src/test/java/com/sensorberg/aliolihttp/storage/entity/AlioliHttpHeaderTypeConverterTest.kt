package com.sensorberg.aliolihttp.storage.entity

import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test

class AlioliHttpHeaderTypeConverterTest {

	private lateinit var classUnderTest: AlioliHttpHeaderTypeConverter

	@Before
	fun setUp() {
		classUnderTest = AlioliHttpHeaderTypeConverter()
	}

	@Test
	fun `should convert empty list to string and back to empty list`() {
		assertList()
	}

	@Test
	fun `should convert entity list to string and back to list`() {
		assertList(AlioliHttpHeader("key", "value"))
	}

	@Test
	fun `should convert entity list to string and back to list with duplicate element`() {
		assertList(AlioliHttpHeader("key", "value"),
				   AlioliHttpHeader("key", "value"))
	}

	@Test
	fun `fromString should return empty list`() {
		val resultList: ArrayList<AlioliHttpHeader> = classUnderTest.fromString("")

		assertThat(resultList).isEmpty()
	}

	@Test
	fun `fromHeaderEntity should return empty json string`() {
		val resultString = classUnderTest.fromHeaderEntity(arrayListOf())

		assertThat(resultString).isEqualTo("[]")
	}

	private fun assertList(vararg inputElements: AlioliHttpHeader) {
		val arrayList: ArrayList<AlioliHttpHeader> = arrayListOf()
		inputElements.forEach {
			arrayList.add(it)
		}

		val resultString: String = classUnderTest.fromHeaderEntity(arrayList)
		val resultList: ArrayList<AlioliHttpHeader> = classUnderTest.fromString(resultString)

		assertThat(resultList).hasSize(inputElements.size)

		inputElements.forEach { inputEntity ->
			val result: AlioliHttpHeader? = resultList.find { it.key == inputEntity.key && it.value == inputEntity.value }
			assertThat(result).isNotNull
		}
	}
}