package com.pepulai.app.commons.util

class ResolvableException : Exception {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(cause: Throwable?) : super(cause)
}