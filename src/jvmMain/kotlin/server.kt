import Database.OperationResult.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.pipeline.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.html.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ThreadLocalRandom

private fun HTML.create() {
    head {
        title = "Shortening new URL"
        script(src = "/static/output.js") {}
    }
    body { ownerEditor(null) }
}

private fun HTML.change(ownerConfig: OwnerConfig) {
    head {
        title = "Editing config"
        script(src = "/static/output.js") {}
    }
    body { ownerEditor(ownerConfig) }
}

private fun HTML.errorPage(code: HttpStatusCode, message: String) {
    head {
        title = "Error $code.value"
    }
    body {
        h1 {
            +"$code"
        }
        p {
            +message
        }
    }
}

object LogLock

fun UserData.validated(): UserData = this.copy(maxClicks = maxClicks?.takeIf { it >= 0 })

suspend inline fun PipelineContext<*, ApplicationCall>.receiveUserData(): UserData {
    val string = call.receiveText()
    return Json.decodeFromString<UserData>(string).validated()
}

//suspend inline fun <reified T> PipelineContext<*, ApplicationCall>.respondJson(code: HttpStatusCode, value: T) {
//    runCatching {
//        call.respondText(Json.encodeToString(value), ContentType.Application.Json, code)
//    }.onFailure { it.printStackTrace() }
//}

suspend inline fun <reified T> PipelineContext<*, ApplicationCall>.respondBad(
    code: HttpStatusCode,
    message: String
): Unit =
    call.respondText(message, status = code)

suspend fun PipelineContext<*, ApplicationCall>.respondBadHtml(code: HttpStatusCode, message: String): Unit =
    call.respondHtml(code) { errorPage(code, message) }

suspend inline fun <reified T> PipelineContext<*, ApplicationCall>.respondGood(value: T): T =
    call.respondText(runCatching { Json.encodeToString(value) }.onFailure { it.printStackTrace() }.getOrThrow(), ContentType.Application.Json, HttpStatusCode.OK).let { value }

fun main() {
    fun newLog() = ThreadLocalRandom.current().nextLong()
    fun Long.log(message: String) = synchronized(LogLock) {
        println(
            "${String.format("[%20d]", this)} ${
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            } $message"
        )
    }

    val database: Database = RamDatabase()
    embeddedServer(Netty, port = 8080, host = "127.0.0.1") {
        install(ContentNegotiation) {
            json()
        }
        routing {
            get("/") {
                call.respondHtml(HttpStatusCode.OK, HTML::index)
            }
            static("/static") {
                resources()
            }
            get("/create") {
                call.respondHtml(HttpStatusCode.OK, HTML::create)
            }
            post("/api/shorten") {
                with(newLog()) {
                    log("Shortening")
                    runCatching { receiveUserData().also { log("Input data: $it") } }
                        .onFailure { e ->
                            respondBad<OwnerConfig>(
                                HttpStatusCode.BadRequest,
                                (e.message ?: "").also { log("Error: $it") })
                        }
                        .onSuccess { data ->
                            respondGood<OwnerConfig>(
                                database.registerUser(data).also { log("Registered: $it") })
                        }
                    //what if I just want to use it as an api
                    //call.respondRedirect("$serverHost/redir?k=${registered.ownerUrl.text}")
                }
            }
            get("/redirect") {
                with(newLog()) {
                    log("Reading shortened URL")
                    val url = call.parameters["k"]?.also { log("Redirecting to: $it") } ?: run {
                        respondBadHtml(HttpStatusCode.BadRequest, "No shortened url given")
                        return@get
                    }

                    val show = call.parameters["show"]?.toBoolean() ?: false

                    when (val config = database.getConfig(Url(url)).also { log("Found config is $it") }) {
                        null -> respondBadHtml(HttpStatusCode.NotFound, "Unknown shortened URL")
                        is PublicConfig -> when (val page = config.data.redirect) {
                            is HtmlPage -> call.respondText(page.code, ContentType.Text.Html)
                            is PageByUrl -> call.respondRedirect(page.url.text)
                        }
                        is OwnerConfig ->
                            if (show)
                                when (val goTo = config.publicConfig.data.redirect) {
                                    is PageByUrl -> call.respondRedirect(goTo.url.text)
                                    is HtmlPage -> call.respondText(goTo.code, ContentType.Text.Html)
                                }
                            else call.respondHtml(HttpStatusCode.OK) { change(config) }
                    }
                }
            }
            delete("/api/delete") {
                with(newLog()) {
                    log("Deleting shortened URL")
                    val url = Url(call.parameters["k"]?.also { log("Url to delete: $it") } ?: let {
                        respondBad<Unit>(HttpStatusCode.BadRequest, "No shortened url given".also { log(it) })
                        return@delete
                    })

                    when (database.unregisterUser(url).also { log("Deletion result: $it") }) {
                        Done -> respondGood(Unit)
                        AccessDenied -> respondBad<Unit>(HttpStatusCode.Forbidden, "Only owners can remove the link")
                        NothingToDo -> respondBad<Unit>(
                            HttpStatusCode.NotFound,
                            "The link is already deleted or has never existed"
                        )
                    }
                }
            }
            put("/api/modify") {
                with(newLog()) {
                    log("Modifying shortened URL")
                    val url = Url(call.parameters["k"]?.also { log("Url to modify: $it") } ?: let {
                        respondBad<Unit>(HttpStatusCode.BadRequest, "No shortened url given".also { log(it) })
                        return@put
                    })
                    runCatching { receiveUserData() }
                        .onFailure { e ->
                            respondBad<Unit>(
                                HttpStatusCode.BadRequest,
                                "Cannot receive new config: $e".also { log(it) })
                        }
                        .onSuccess { newData ->
                            when (database.changeUserData(url, newData).also { log("Result: $it") }) {
                                Done -> respondGood(Unit)
                                AccessDenied -> respondBad<Unit>(
                                    HttpStatusCode.Forbidden,
                                    "Only owners can change configs"
                                )
                                NothingToDo -> respondBad<Unit>(
                                    HttpStatusCode.NotFound,
                                    "The link is deleted or has never existed"
                                )
                            }
                        }
                }
            }
        }
    }.start(wait = true)
}