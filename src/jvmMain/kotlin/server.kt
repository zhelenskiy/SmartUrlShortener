import UrlDatabase.OperationResult.*
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
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ThreadLocalRandom
import kotlin.time.*

fun HEAD.commonHeadPart() {
    link {
        href = "/static/icon.png"
        rel = "icon"
    }
    link {
        href = "https://use.fontawesome.com/releases/v5.8.1/css/all.css"
        rel = "stylesheet"
    }
    link {
        href = "https://fonts.googleapis.com/css?family=Fira+Sans:300,400,600&display=swap"
        rel = "stylesheet"
    }
    link {
        href = "/static/Styles.css"
        rel = "stylesheet"
    }
    meta(name = "viewport", content = "width=device-width, initial-scale=1")
}

private fun HTML.create() {
    head {
        title = "Shortening new URL"
        commonHeadPart()
        script(src = "/static/output.js") {}
    }
    body(classes = "colorful-background") { ownerEditor(null) }
}

private fun HTML.change(ownerConfig: OwnerConfig) {
    head {
        title = "Editing config"
        commonHeadPart()
        script(src = "/static/output.js") {}
    }
    body(classes = "colorful-background") { ownerEditor(ownerConfig) }
}

private fun HTML.errorPage(code: HttpStatusCode, message: String) {
    head {
        title = "Error $code.value"
        commonHeadPart()
    }
    body(classes = "colorful-background") {
        div("main-block") {
            div("header") {
                p {
                    h1 {
                        div("font-size: xx-large;") {
                            +"${code.value}"
                        }
                    }
                    h2 {
                        div("font-size: x-large;") {
                            +code.description
                        }
                    }
                }
            }
            h1 {
                +message
            }
            h1 {
                a(href = "$serverHost/") {
                    +"Go to the main page"
                }
            }
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
    call.respondText(
        runCatching { Json.encodeToString(value) }.onFailure { it.printStackTrace() }.getOrThrow(),
        ContentType.Application.Json,
        HttpStatusCode.OK
    ).let { value }

val dbConfig = runCatching {
    val (driver, user, password) = Files.readAllLines(Paths.get("credentials.txt"))
    LaunchConfig(driver, user, password)
}.onFailure {
    println("Cannot load database configuration:\n${it.message}")
    it.printStackTrace(System.out)
    println("Using local database")
}.getOrDefault(LaunchConfig("jdbc:sqlite:identifier.sqlite", "", ""))
    .also {
        println("Using driver: ${it.driver}")
        println("User: ${it.user}")
        println("Password: ${String(it.password.map { '*' }.toCharArray())}")
    }

//    val urlDatabase: UrlDatabase = RamUrlDatabase()
val urlDatabase: ExposedUrlDatabase = ExposedUrlDatabase(dbConfig)

@ExperimentalTime
fun main(vararg args: String) {
    fun newLog() = ThreadLocalRandom.current().nextLong()
    val logging = "no-log" !in args
    fun Long.log(message: String) {
        if (logging) {
            synchronized(LogLock) {
                val curTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                println("${String.format("[%20d]", this)} $curTime $message")
            }
        }
    }

//    val guards = Array(6) { LoopGuard((50L * 10.0.pow(it)).roundToLong(), (100L * 10.0.pow(it)).milliseconds) }
    val guards = emptyArray<LoopGuard>()

    embeddedServer(Netty) {
        install(ContentNegotiation) {
            json()
        }
        routing {
            get("/") {
                call.respondHtml(HttpStatusCode.OK, HTML::index)
            }
            static("/static") {
                resources()
                resources("files")
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
                                urlDatabase.registerUser(data).also { log("Registered: $it") })
                        }
                    //what if I just want to use it as an api
                    //call.respondRedirect("$serverHost/redir?k=${registered.ownerUrl.text}")
                }
            }
            get("/{k}") {
                with(newLog()) {
                    log("Reading shortened URL")
                    val url = call.parameters["k"]?.also { log("Redirecting to: $it") } ?: run {
                        respondBadHtml(HttpStatusCode.BadRequest, "No shortened url given")
                        return@get
                    }

                    when (val config = urlDatabase.getConfig(Url(url)).also { log("Found config is $it") }) {
                        null -> respondBadHtml(HttpStatusCode.NotFound, "Unknown shortened URL")
                        is PublicConfig -> when (val page = config.data.redirect) {
                            is HtmlPage -> call.respondText(page.code, ContentType.Text.Html)
                            is PageByUrl -> when {
                                call.request.queryParameters.contains("recursive") ->
                                    respondBadHtml(HttpStatusCode.Forbidden, "Cannot redirect to other public link")
                                guards.any { !it.isSafe(page.url) } ->
                                    respondBadHtml(HttpStatusCode.Forbidden, "Too many requests to the page.")
                                else -> call.respondRedirect(
                                    page.url.text.let { if (it.startsWith(serverHost)) "$it?recursive" else it })
                            }
                        }
                        is OwnerConfig -> call.respondHtml(HttpStatusCode.OK) { change(config) }
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

                    when (urlDatabase.unregisterUser(url).also { log("Deletion result: $it") }) {
                        Done -> respondGood(Unit)
                        AccessDenied -> respondBad<Unit>(
                            HttpStatusCode.Forbidden,
                            "Only owners can remove the link"
                        )
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
                            when (urlDatabase.changeUserData(url, newData).also { log("Result: $it") }) {
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