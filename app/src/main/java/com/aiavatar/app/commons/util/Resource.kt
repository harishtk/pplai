package com.aiavatar.app.commons.util

import java.util.Date

/**
 * A generic class that describes data with a status.
 */
class Resource<T> private constructor(
    val status: Status,
    var data: T? = null,
    val t: Throwable? = null,
    val date: Date? = null
) {

    companion object {
        fun <T> success(data: T?, date: Date?): Resource<T> {
            return Resource(status = Status.SUCCESS, data = data, date = date)
        }

        fun <T> error(t: Throwable?, data: T?, date: Date?): Resource<T> {
            return Resource(status = Status.ERROR, data, t, date)
        }

        fun <T> loading(data: T?): Resource<T> {
            return Resource(status = Status.LOADING, data = data)
        }

        fun <T> cached(data: T?, date: Date?): Resource<T> {
            return Resource(status = Status.CACHED, data = data, date = date)
        }

        fun <T> reAuthenticate(): Resource<T> {
            return Resource(Status.REAUTH)
        }

        fun <T> logout(): Resource<T> {
            return Resource(Status.LOGOUT)
        }
    }
}

enum class Status {
    SUCCESS, ERROR, LOADING, CACHED, REAUTH, LOGOUT
}