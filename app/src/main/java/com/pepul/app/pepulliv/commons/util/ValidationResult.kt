package com.pepul.app.pepulliv.commons.util

data class ValidationResult(
    val typedValue: String, /* for StateFlow to recognize different value */
    val successful: Boolean = false,
    val errorMessage: UiText? = null
)