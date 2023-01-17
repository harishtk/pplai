package com.aiavatar.app.commons.util

/**
 * A generic class that holds data and it's state
 *
 * @param <T>
 */
sealed class Result<out R> {

    data class Success<out T>(val data: T): Result<T>()
    data class Error(val exception: Exception): Result<Nothing>()
    object Loading : Result<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[exception=$exception]"
            Loading         -> "Loading"
        }
    }
}

/**
 * `true` if [Result] is of type [Result.Success] & holds a non-null [Result.Success.data].
 */
val Result<*>.succeeded
    get() = this is Result.Success && this.data != null