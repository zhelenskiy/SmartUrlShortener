//import io.ktor.client.features.json.serializer.*
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.w3c.dom.*
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.FileReader
import org.w3c.files.get

fun main() {
    fun isFromUrl() = (document.getElementById("source-url") as HTMLInputElement).checked

    fun getFileSelector() = document.getElementById("file-selector") as HTMLInputElement

    fun getUrlSelector() = document.getElementById("url-selector") as HTMLInputElement

    fun chosenFile(fileSelector: HTMLInputElement): Blob? =
        fileSelector.files?.get(0)

    fun updateSourceType() {
        val fromUrl: Boolean = isFromUrl()
        (document.getElementById("file-selector-div") as HTMLDivElement).style.display =
            if (fromUrl) "none" else "block"
        val fileSelector = getFileSelector()
        fileSelector.required = !fromUrl
        (document.getElementById("url-selector-div") as HTMLDivElement).style.display =
            if (fromUrl) "block" else "none"
        val urlSelector = getUrlSelector()
        urlSelector.required = fromUrl
        (document.getElementById("show-link") as HTMLAnchorElement).href =
            (if (fromUrl) urlSelector.value.takeUnless { it.isBlank() }
            else chosenFile(fileSelector)?.let { URL.createObjectURL(it) })
                ?: "about:blank"
        val useDate = (document.getElementById("date-selector-checkbox") as HTMLInputElement).checked
        (document.getElementById("date-selector") as HTMLInputElement).apply {
            hidden = !useDate
            required = useDate
        }
        val useMaxClicks = (document.getElementById("max-clicks-selector-checkbox") as HTMLInputElement).checked
        (document.getElementById("max-clicks-selector") as HTMLInputElement).apply {
            hidden = !useMaxClicks
            required = useMaxClicks
        }
        val status = document.getElementById("status") as HTMLLabelElement
        val form = document.getElementById("input-form") as HTMLFormElement
        if (!form.checkValidity()) {
            status.textContent = "Not all fields are filled in"
            status.style.color = "red"
        } else {
            status.textContent = "The config is filled correct"
            status.style.color = "green"
        }
    }

    fun sendData(userData: UserData?) {
        val url = (document.getElementById("owner-link") as? HTMLAnchorElement)?.text
        val client = HttpClient(Js) {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
        }
        GlobalScope.launch {
            val status = document.getElementById("status") as HTMLLabelElement
            try {
                when {
                    userData == null -> {
                        client.delete<Unit>("$serverHost/api/delete?k=$url")
                        window.location.href = "$serverHost/"
                    }
                    url != null -> {
                        window.alert(url)
                        client.put<Unit>("$serverHost/api/modify?k=$url") {
                            contentType(ContentType.Application.Json)
                            body = userData
                        }
                        window.location.href = "$serverHost/redirect?k=$url"
                    }
                    else -> {
                        val registered: OwnerConfig = client.post("$serverHost/api/shorten/") {
                            contentType(ContentType.Application.Json)
                            body = userData
                        }
                        window.alert(registered.toString())
                        window.location.href = "$serverHost/redirect?k=${registered.ownerUrl.text}"
                    }
                }
                status.textContent = "Operation has succeeded"
                status.style.color = "green"
            } catch(e: Throwable) {
                console.log(e.stackTraceToString())
                status.textContent = "Error happened: ${e.message}"
                status.style.color = "red"
            }
        }
    }
    window.onload = {
        val status = document.getElementById("status") as HTMLLabelElement
        (document.getElementById("init-date") as? HTMLLabelElement)?.textContent?.toLongOrNull()?.let {
            eval("document.getElementById(\"date-selector\").valueAsNumber = $it;")
        }
        val form = document.getElementById("input-form") as HTMLFormElement
        form.onchange = { updateSourceType() }
        val submitButton = document.getElementById("submit-button") as HTMLInputElement
        submitButton.onclick = { _ ->
            if (!form.checkValidity()) {
                status.textContent = "Not all fields are filled in"
                status.style.color = "red"
            } else {
                status.textContent = "The config is filled correct"
                status.style.color = "green"
                val date = ((document.getElementById("date-selector") as HTMLInputElement)
                    .takeIf { (document.getElementById("date-selector-checkbox") as HTMLInputElement).checked }
                    ?.value
                    ?.runCatching { toLocalDateTime() }?.getOrNull())
                    ?.let { SerializableDate(it.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()) }
                val n = (document.getElementById("max-clicks-selector") as HTMLInputElement)
                    .takeIf { (document.getElementById("max-clicks-selector-checkbox") as HTMLInputElement).checked }
                    ?.value?.toIntOrNull()
                if (isFromUrl()) {
                    val urlText = getUrlSelector().value
                    if (urlText.isNotBlank()) {
                        sendData(UserData(PageByUrl(Url(urlText)), date, n))
                    }
                } else {
                    val chosenFile = chosenFile(getFileSelector())
                    if (chosenFile != null) {
                        val fileReader = FileReader()
                        submitButton.readOnly = true
                        status.innerText = "Loading from disk"
                        status.style.color = "yellow"
                        fileReader.onload = { e ->
                            val source = e.target.asDynamic().result as String
                            status.innerText = "Loaded from disk successfully"
                            status.style.color = "green"
                            sendData(UserData(HtmlPage(source), date, n))
                        }
                        fileReader.onloadend = {
                            submitButton.readOnly = false
                            Unit
                        }
                        fileReader.onerror = {
                            console.log("Error while loading from disk happened: $it")
                            status.innerText = "An error occurred reading this file: $it"
                            status.style.color = "red"
                            Unit
                        }
                        fileReader.readAsText(chosenFile)
                    }
                }
            }
        }
        (document.getElementById("delete-button") as? HTMLInputElement)?.onclick = {
            sendData(null)
        }
        updateSourceType()
    }
}

