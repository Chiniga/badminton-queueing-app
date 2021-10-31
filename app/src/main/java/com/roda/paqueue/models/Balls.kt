package com.roda.paqueue.models

import io.realm.RealmObject

open class Balls (
    var cost: Double = 0.00,
    var calculation_method: Int = 0,
    var used: Int = 0
): RealmObject()
