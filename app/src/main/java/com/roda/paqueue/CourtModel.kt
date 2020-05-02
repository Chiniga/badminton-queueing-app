package com.roda.paqueue

import io.realm.RealmObject
import io.realm.kotlin.*

open class CourtModel (
    var courts: Int = 0
): RealmObject()