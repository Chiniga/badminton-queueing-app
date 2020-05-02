package com.roda.paqueue

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import io.realm.kotlin.*

open class QueueModel (
    @PrimaryKey var id: Long = 0,
    @Required var status: String = ""
): RealmObject()