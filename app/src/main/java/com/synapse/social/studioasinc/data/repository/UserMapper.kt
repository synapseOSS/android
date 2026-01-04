package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.data.local.database.UserEntity
import com.synapse.social.studioasinc.domain.model.User

object UserMapper {

    fun toEntity(user: User): UserEntity {
        return UserEntity(
            uid = user.uid,
            username = user.username,
            email = user.email,
            avatarUrl = user.avatar,
            isVerified = user.verify
        )
    }

    fun toModel(entity: UserEntity): User {
        return User(
            uid = entity.uid,
            username = entity.username,
            email = entity.email,
            avatar = entity.avatarUrl,
            verify = entity.isVerified
        )
    }
}
