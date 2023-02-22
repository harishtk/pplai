package com.aiavatar.app.commons.util

import okhttp3.Cache

/**
 * A generic class that holds data and it's state
 *
 * @param <T>
 */
sealed class Result<out R> {

    data class Success<R>(val data: R): Result<R>()
    sealed class Error(val exception: Exception): Result<Nothing>() {
        class RecoverableError(exception: Exception) : Error(exception)
        class NonRecoverableError(exception: Exception) : Error(exception)
    }
    object Loading : Result<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*>   -> "Success[data=$data]"
            is Error        -> when (this) {
                is Error.NonRecoverableError -> "NonRecoverableError[exception=$exception]"
                is Error.RecoverableError -> "RecoverableError[exception=$exception]"
            }
            Loading         -> "Loading"
        }
    }
}

/**
 * `true` if [Result] is of type [Result.Success] & holds a non-null [Result.Success.data].
 */
val Result<*>.succeeded
    get() = this is Result.Success && this.data != null