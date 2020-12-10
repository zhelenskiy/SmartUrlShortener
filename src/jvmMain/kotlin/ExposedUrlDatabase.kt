import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.transaction

internal object SqlDB : LongIdTable() {
    val url: Column<String> = varchar("url", urlChars).uniqueIndex()
    val configJson: Column<String> = text("config")
}

internal class UrlEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<UrlEntity>(SqlDB)

    var url: String by SqlDB.url
    var configJson by SqlDB.configJson
}

class ExposedUrlDatabase(user: String = "", password: String = "") : UrlDatabase() {
    init {
        Database.connect("jdbc:sqlite:identifier.sqlite", user = user, password = password)
        println("Connected to database")
        loggedTransaction {
            create(SqlDB)
        }
        println("Table created")
    }

    private fun <T> loggedTransaction(db: Database? = null, statement: Transaction.() -> T): T = runCatching {
        transaction(db) {
            addLogger(StdOutSqlLogger)
            statement()
        }
    }.onFailure { println(it); it.printStackTrace() }.getOrThrow()


    override fun getUnsafeConfig(url: Url): OwnerConfig? = loggedTransaction {
        addLogger(StdOutSqlLogger)
        UrlEntity.find { SqlDB.url eq url.text }.firstOrNull()?.configJson?.let { Json.decodeFromString(it) }
    }

    override fun registerLink(url: Url, ownerConfig: OwnerConfig) {
        loggedTransaction {
            addLogger(StdOutSqlLogger)
            println("Registering new link: $url")
            val encodedConfig = Json.encodeToString(ownerConfig)
            when (val alreadyHave = UrlEntity.find { SqlDB.url eq url.text }.singleOrNull()) {
                null -> {
                    UrlEntity.new {
                        this.url = url.text
                        configJson = encodedConfig
                    }
                    println("Registered new link: $url")
                }
                else -> {
                    alreadyHave.configJson = encodedConfig
                    println("Modified new link: $url")
                }
            }
        }
    }

    override fun unregisterOnlyLink(url: Url) {
        loggedTransaction {
            addLogger(StdOutSqlLogger)
            UrlEntity.find { SqlDB.url eq url.text }.firstOrNull()?.delete()
        }
    }

    override fun <R> atomic(f: UrlDatabase.() -> R): R = transaction { f() }

    fun removeAll() {
        loggedTransaction {
            UrlEntity.all().map { it.delete() }
        }
    }

    fun selectAll() = loggedTransaction {
        UrlEntity.all().map { Pair(Url(it.url), Json.decodeFromString<OwnerConfig>(it.configJson)) }
    }
}