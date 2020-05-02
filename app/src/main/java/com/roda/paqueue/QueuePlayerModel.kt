package com.roda.paqueue

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required

open class QueuePlayerModel (
    @PrimaryKey var id: Long = 0,
    @Required var queue_id: Long = 0,
    @Required var player_id: Long = 0
): RealmObject()