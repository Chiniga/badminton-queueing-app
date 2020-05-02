import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import java.util.*

open class QueuePlayerModel (
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    var created_at: Date = Date(),
    @Required
    var queue_id: Long = 0,
    @Required
    var player_id: Long = 0
): RealmObject()