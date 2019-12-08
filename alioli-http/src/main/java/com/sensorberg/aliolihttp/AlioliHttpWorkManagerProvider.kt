package com.sensorberg.aliolihttp

import androidx.work.WorkManager

internal object AlioliHttpWorkManagerProvider {

	fun getWorkManager(): WorkManager = WorkManager.getInstance()
}