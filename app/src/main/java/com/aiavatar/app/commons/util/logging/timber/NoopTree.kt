package com.aiavatar.app.commons.util.logging.timber

import timber.log.Timber

class NoopTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Noop
    }
}