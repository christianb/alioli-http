package com.sensorberg.aliolihttp

object AlioliHttpConstant {

	private const val KEY_PREFIX = "x-alioli-http"

	/**
	 * Use this key if you wanna set a custom value until the request should be valid.
	 * Your custom value must be in milliseconds.
	 */
	const val HEADER_KEY_VALID_UNTIL = "$KEY_PREFIX-valid-until"

	/**
	 * One day in millis.
	 */
	const val HEADER_VALUE_MAX_1_DAY: Long = 86_400_000L

	/**
	 * One week in millis.
	 */
	const val HEADER_VALUE_MAX_1_WEEK: Long = HEADER_VALUE_MAX_1_DAY * 7

	/**
	 * Two weeks in millis.
	 */
	const val HEADER_VALUE_MAX_2_WEEKS: Long = HEADER_VALUE_MAX_1_DAY * 14

	/**
	 * One month in millis. Assuming 30 days per month.
	 */
	const val HEADER_VALUE_MAX_1_MONTH: Long = HEADER_VALUE_MAX_1_DAY * 30

	/**
	 * Combination of "[HEADER_KEY_VALID_UNTIL]: [HEADER_VALUE_MAX_2_WEEKS]"
	 */
	const val HEADER_DEFAULT = "$HEADER_KEY_VALID_UNTIL: $HEADER_VALUE_MAX_2_WEEKS"
}
