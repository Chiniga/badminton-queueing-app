import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import java.util.*

open class PlayerModel (
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    var created_at: Date = Date(),
    @Required
    var name: String = "",
    @Required
    var level: String = "",
    var num_games: Int = 0
): RealmObject() {
    fun isValidName(name: String): Boolean {
        val regex = "/^[A-Za-z]+\$/".toRegex()
        if(!regex.matches(name)) {
            return false;
        }
        return true;
    }
}