package com.example.app.commons.util

import com.example.app.commons.util.loadstate.LoadType


sealed class PagedRequest<Key: Any> constructor(
    public val loadSize: Int,
) {

    public abstract val key: Key?

    public class Refresh<Key: Any> constructor(
        override val key: Key?,
        loadSize: Int
    ) : PagedRequest<Key>(loadSize = loadSize)

    public class Append<Key: Any> constructor(
        override val key: Key,
        loadSize: Int
    ) : PagedRequest<Key>(loadSize = loadSize)

    public class Prepend<Key: Any> constructor(
        override val key: Key,
        loadSize: Int
    ) : PagedRequest<Key>(loadSize = loadSize)

    companion object {
        fun <Key: Any> create(
            loadType:
            LoadType,
            key: Key?,
            loadSize: Int
        ): PagedRequest<Key> = when (loadType) {
            LoadType.REFRESH -> Refresh(
                key = key,
                loadSize = loadSize
            )
            LoadType.APPEND -> Append(
                loadSize = loadSize,
                key = requireNotNull(key) {
                    "key must not be null for Append"
                }
            )
            LoadType.PREPEND -> Prepend(
                loadSize = loadSize,
                key = requireNotNull(key) {
                    "key must not be null for Prepend"
                }
            )
            LoadType.ACTION -> error("PagedRequest is invalid for loadType ACTION")
        }
    }
}
