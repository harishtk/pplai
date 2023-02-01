package com.aiavatar.app.core.data.source.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.domain.model.LoginUser

@Entity(tableName = LoginUserTable.name)
data class LoginUserEntity(
    @ColumnInfo("username")
    val username: String
) {
    @ColumnInfo("id")
    @PrimaryKey(autoGenerate = true)
    var _id: Long? = null

    @ColumnInfo("userId")
    var userId: String? = null
}

fun LoginUserEntity.toLoginUser(): LoginUser {
    return LoginUser(
        username = username
    ).also {
        it.userId = userId
    }
}

fun LoginUser.toEntity(): LoginUserEntity {
    return LoginUserEntity(
        username = username
    ).also {
        it.userId = userId
    }
}

object LoginUserTable {
    const val name = AppDatabase.TABLE_LOGIN_USER

    object Columns {
        const val ID            = "id"
        const val USER_ID       = "user_id"
        const val USERNAME      = "username"
    }
}
