package com.roda.paqueue.models

import io.realm.DynamicRealm
import io.realm.RealmMigration

class DbMigration : RealmMigration {
    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        var oVersion = oldVersion
        // DynamicRealm exposes an editable schema
        val schema = realm.schema

        // Migrate to version 1: Add a new class.
        // Example:
        // open class Person(
        //     var name: String = "",
        //     var age: Int = 0,
        // ): RealmObject()
        /*if (oVersion == 0L) {
            schema.create("Person")
                .addField("name", String::class.java)
                .addField("age", Int::class.javaPrimitiveType)
            oVersion++
        }*/

        // Migrate to version 2: Add a primary key + object references
        // Example:
        // open class Person(
        //     var name: String = "",
        //     var age: Int = 0,
        //     @PrimaryKey
        //     var id: Int = 0,
        //     var favoriteDog: Dog? = null,
        //     var dogs: RealmList<Dog> = RealmList()
        // ): RealmObject()

        /*if (oVersion == 1L) {
            schema.get("Player")!!
                .addField("id", Long::class.javaPrimitiveType, FieldAttribute.PRIMARY_KEY)
                .addRealmObjectField("favoriteDog", schema.get("Dog"))
                .addRealmListField("dogs", schema.get("Dog"))
            oVersion++
        }*/
    }
}