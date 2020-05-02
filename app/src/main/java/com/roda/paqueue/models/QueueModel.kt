import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import io.realm.kotlin.*
import java.util.*

open class QueueModel (
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    var created_at: Date = Date(),
    @Required
    var status: String = ""
): RealmObject()