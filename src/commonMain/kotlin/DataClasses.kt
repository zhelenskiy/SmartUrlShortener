import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

const val serverHost = "http://localhost:8080"

@Serializable
data class Url(val text: String)

@Serializable
data class SerializableDate(val millis: Long) {
    override fun toString(): String =
        Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).toString()

    operator fun compareTo(other: SerializableDate): Int = this.millis.compareTo(other.millis)

    companion object {
        fun now(): SerializableDate = SerializableDate(Clock.System.now().toEpochMilliseconds())
    }
}

@Serializable
data class UserData(
    val redirect: TargetPage,
    val expirationDate: SerializableDate?,
    var maxClicks: Int?
)

@Serializable
sealed class TargetPage

@Serializable
data class PageByUrl(val url: Url) : TargetPage()

@Serializable
data class HtmlPage(val code: String) : TargetPage()

@Serializable
sealed class Config

@Serializable
data class PublicConfig(val publicUrl: Url, val data: UserData) : Config()

@Serializable
data class OwnerConfig(val ownerUrl: Url, val publicConfig: PublicConfig) : Config()

//@Serializable
//sealed class Response<T>
//
//@Serializable
//data class ExpectedResponse<T>(val response: T) : Response<T>()
//
//@Serializable
//data class ErrorResponse<T>(val message: String) : Response<T>()