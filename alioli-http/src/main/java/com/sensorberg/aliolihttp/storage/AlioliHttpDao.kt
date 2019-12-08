package com.sensorberg.aliolihttp.storage

import androidx.room.*
import com.sensorberg.aliolihttp.storage.entity.ALIOLI_HTTP_ENTITY_TABLE_NAME
import com.sensorberg.aliolihttp.storage.entity.AlioliHttpEntity

@Dao
internal interface AlioliHttpDao {

	@Insert(onConflict = OnConflictStrategy.ABORT)
	fun insert(alioliHttpEntity: AlioliHttpEntity): Long

	@Query("SELECT * FROM $ALIOLI_HTTP_ENTITY_TABLE_NAME")
	fun getAll(): List<AlioliHttpEntity>

	@Query("DELETE FROM $ALIOLI_HTTP_ENTITY_TABLE_NAME WHERE :id=id")
	fun delete(id: Long)

	@Query("SELECT count(*) FROM $ALIOLI_HTTP_ENTITY_TABLE_NAME")
	fun size(): Int
}