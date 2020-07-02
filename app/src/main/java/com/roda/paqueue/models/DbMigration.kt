package com.roda.paqueue.models

import io.realm.DynamicRealm
import io.realm.RealmMigration

class DbMigration : RealmMigration {
    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        // DynamicRealm exposes an editable schema
        val schema = realm.schema

        if (oldVersion == 0L) {
            schema.get("Player")!!.renameField("queue", "queues")
        }
    }
}