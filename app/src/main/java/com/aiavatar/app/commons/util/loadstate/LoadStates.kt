package com.aiavatar.app.commons.util.loadstate

public data class LoadStates(
    public val refresh: LoadState,
    public val prepend: LoadState,
    public val append: LoadState,
    public val action: LoadState
) {

    public inline fun forEach(op: (LoadType, LoadState) -> Unit) {
        op(LoadType.REFRESH, refresh)
        op(LoadType.PREPEND, prepend)
        op(LoadType.APPEND, append)
        op(LoadType.ACTION, action)
    }

    internal fun modifyState(loadType: LoadType, newState: LoadState): LoadStates {
        return when (loadType) {
            LoadType.REFRESH -> copy(
                refresh = newState
            )
            LoadType.PREPEND -> copy(
                prepend = newState
            )
            LoadType.APPEND -> copy(
                append = newState
            )
            LoadType.ACTION -> copy(
                action = newState
            )
        }
    }

    internal fun get(loadType: LoadType) = when (loadType) {
        LoadType.REFRESH -> refresh
        LoadType.PREPEND -> prepend
        LoadType.APPEND -> append
        LoadType.ACTION -> action
    }

    internal companion object {
        val IDLE = LoadStates(
            refresh = LoadState.NotLoading.InComplete,
            prepend = LoadState.NotLoading.InComplete,
            append = LoadState.NotLoading.InComplete,
            action = LoadState.NotLoading.InComplete
        )
    }
}