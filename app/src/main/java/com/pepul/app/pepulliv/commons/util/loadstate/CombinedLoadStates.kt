package com.pepul.app.pepulliv.commons.util.loadstate

import com.pepulnow.app.data.LoadState

public class CombinedLoadStates(
    public val refresh: LoadState,
    public val prepend: LoadState,
    public val append: LoadState,
    public val source: LoadState
) {

    override fun toString(): String {
        return "CombinedLoadStates(refresh=$refresh, prepend=$prepend, append=$append, source=$source)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CombinedLoadStates

        if (refresh != other.refresh) return false
        if (prepend != other.prepend) return false
        if (append != other.append) return false
        if (source != other.source) return false

        return true
    }

    override fun hashCode(): Int {
        var result = refresh.hashCode()
        result = 31 * result + prepend.hashCode()
        result = 31 * result + append.hashCode()
        result = 31 * result + source.hashCode()
        return result
    }
}