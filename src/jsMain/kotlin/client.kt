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
import org.w3c.files.Blob
import org.w3c.files.FileReader
import org.w3c.files.get

fun main() {

    window.onload = {
        val status = document.getElementById("status") as HTMLLabelElement
        val form = document.getElementById("input-form") as HTMLFormElement
        val submitButton = document.getElementById("submit-button") as HTMLInputElement
        val fileSelector = document.getElementById("file-selector") as HTMLInputElement
//        val highlightCheckbox = document.getElementById("highlight-checkbox") as HTMLInputElement
        val urlSelector = document.getElementById("url-selector") as HTMLInputElement
        val fileSelectorDiv = document.getElementById("file-selector-div") as HTMLDivElement
        val urlSelectorDiv = document.getElementById("url-selector-div") as HTMLDivElement
        val dateSelectorCheckbox = document.getElementById("date-selector-checkbox") as HTMLInputElement
        val dateSelector = document.getElementById("date-selector") as HTMLInputElement
        val sourceUrl = document.getElementById("source-url") as HTMLInputElement
        val maxClicksSelectorCheckbox = document.getElementById("max-clicks-selector-checkbox") as HTMLInputElement
        val maxClicksSelector = document.getElementById("max-clicks-selector") as HTMLInputElement
//        val codeSourceBlock = document.getElementById("code-source-block") as HTMLDivElement
        val ownerID = document.getElementById("owner-id") as? HTMLLabelElement
        val initDate = document.getElementById("init-date") as? HTMLLabelElement
        val showLink = document.getElementById("show-link") as HTMLButtonElement
        val deleteButton = document.getElementById("delete-button") as? HTMLInputElement
//        val codeSource = (document.getElementById("code-source"))!!
        val highlightButton = document.getElementById("highlight-button") as HTMLButtonElement
        val fileStatus = document.getElementById("file-status") as HTMLLabelElement
        val clearButton = document.getElementById("clear-button") as HTMLButtonElement
//        val fileSelectorLabel = document.getElementById("file-selector-label") as HTMLLabelElement
        val fileReporter = document.getElementById("file-reporter") as HTMLInputElement

        var loadedHtml = (document.getElementById("init-user-html") as HTMLLabelElement).textContent ?: ""


        fun isFromUrl(): Boolean = sourceUrl.checked

        fun customCheckValidity(form: HTMLFormElement) =
            form.checkValidity() && (isFromUrl() || loadedHtml.isNotBlank())

        fun defaultStatus() {
            if (!customCheckValidity(form)) {
                status.textContent = "Not all fields are filled in"
                status.style.color = "red"
            } else {
                status.textContent = "The config is filled correctly"
                status.style.color = "blue"
            }
        }

        fun chosenFile(fileSelector: HTMLInputElement): Blob? = fileSelector.files?.get(0)

        fun updateSourceType() {
            val fromUrl: Boolean = isFromUrl()
            fileSelectorDiv.style.display = if (fromUrl) "none" else "block"
            highlightButton.style.display = if (fromUrl) "none" else "block"
            fileReporter.style.display = if (fromUrl) "none" else "block"
            clearButton.className = "button" + if (fromUrl) " last-button" else ""
//        val fileSelector = getFileSelector()
//        fileSelector.required = !fromUrl
            urlSelectorDiv.style.display = if (fromUrl) "block" else "none"
            urlSelector.required = fromUrl
            val useDate = dateSelectorCheckbox.checked
            dateSelector.apply {
                style.visibility = if (useDate) "visible" else "hidden"
                required = useDate
            }
            val useMaxClicks = maxClicksSelectorCheckbox.checked
            maxClicksSelector.apply {
                style.visibility = if (useMaxClicks) "visible" else "hidden"
                required = useMaxClicks
            }
//            codeSourceBlock.style.display = if (highlightCheckbox.checked) "block" else "none"
            defaultStatus()
        }

        fun sendData(userData: UserData?) {
            val url = ownerID?.textContent
            val client = HttpClient(Js) {
                install(JsonFeature) {
                    serializer = KotlinxSerializer()
                }
            }
            GlobalScope.launch {
                try {
                    when {
                        userData == null -> {
                            client.delete<Unit>("$serverHost/api/delete?k=$url")
                            window.location.href = "$serverHost/"
                        }
                        url != null -> {
//                        window.alert(url)
                            client.put<Unit>("$serverHost/api/modify?k=$url") {
                                contentType(ContentType.Application.Json)
                                body = userData
                            }
                            window.location.href = "$serverHost/$url"
                        }
                        else -> {
                            val registered: OwnerConfig = client.post("$serverHost/api/shorten/") {
                                contentType(ContentType.Application.Json)
                                body = userData
                            }
//                        window.alert(registered.toString())
                            window.location.href = "$serverHost/${registered.ownerUrl.text}"
                        }
                    }
                    status.textContent = "Operation has succeeded"
                    status.style.color = "blue"
                } catch (e: Throwable) {
                    console.log(e.stackTraceToString())
                    status.textContent = "Error happened: ${e.message}"
                    status.style.color = "red"
                }
            }
        }

        initDate?.textContent?.toLongOrNull()?.let {
            dateSelector.valueAsNumber = it.toDouble()
        }
        form.onchange = { updateSourceType() }
        sourceUrl.onchange = { updateSourceType() }
        maxClicksSelector.onkeyup = { updateSourceType() }
        urlSelector.onkeyup = { updateSourceType() }
        highlightButton.onclick = {
            val page = """<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
	<title>Source code</title>
	<link href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.22.0/themes/prism.css" rel="stylesheet"/>
</head>
<body><pre><code>
${js("Prism").highlight(loadedHtml, js("Prism").languages.html, "html") as String}
</code></pre>
	<script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.22.0/components/prism-core.min.js"></script>
	<script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.22.0/plugins/autoloader/prism-autoloader.min.js"></script>
</body>
</html>"""
            window.open()?.document?.apply {
                write(page)
//                getElementById("code")!!.textContent = loadedHtml
            }
        }
        fun updateFileStatus() {
            fileReporter.setCustomValidity("")
            if (loadedHtml.isBlank()) {
                fileStatus.textContent = "No file content loaded"
                fileStatus.style.color = "red"
            } else {
                fileStatus.textContent = "File content is loaded"
                fileStatus.style.color = "blue"
            }
        }
//        highlightCheckbox.onchange = {
//            codeSourceBlock.style.display = if (highlightCheckbox.checked) "block" else "none"
//            Unit
//        }
        showLink.onclick = {
            if (isFromUrl())
                window.open(url = urlSelector.value, target = "_blank")
            else
                window.open(target = "_blank")?.document?.write(loadedHtml)
        }
        clearButton.onclick = {
            if (isFromUrl())
                urlSelector.value = ""
            else
                loadedHtml = ""
            updateFileStatus()
            updateSourceType()
        }
        fileSelector.onchange = {
            val chosenFile = chosenFile(fileSelector)
            if (chosenFile != null) {
                val fileReader = FileReader()
                submitButton.readOnly = true
                status.innerText = "Loading from disk"
                status.style.color = "yellow"
                fileReader.onload = { e ->
                    loadedHtml = e.target.asDynamic().result as String
//                    window.alert(loadedHtml)
//                    codeSource.let {
//                        it.textContent = loadedHtml
//                        status.innerText = "Loaded from disk successfully"
//                        status.innerText = "Highlighting"
//                        status.style.color = "yellow"
//                        val converted = js("Prism").highlight(loadedHtml, js("Prism").languages.html, "html") as String
//                        status.innerText = "Loading highlighted"
//                        status.style.color = "yellow"
//                        it.innerHTML = converted
//                    }
                    updateFileStatus()
                    defaultStatus()
                }
                fileReader.onloadend = {
                    submitButton.readOnly = false
                    fileSelector.value = ""
                    Unit
                }
                fileReader.onerror = {
                    updateFileStatus()
                    console.log("Error while loading from disk happened: $it")
                    status.innerText = "An error occurred reading this file: $it"
                    status.style.color = "red"
                    Unit
                }
                fileReader.readAsText(chosenFile)
            } else {
                loadedHtml = ""
//                codeSource.innerHTML = "You will see your code here"
            }
        }
        submitButton.onclick = { _ ->
            if (customCheckValidity(form)) {
                val date = dateSelector.takeIf { dateSelectorCheckbox.checked }?.value
                    ?.runCatching { toLocalDateTime() }?.getOrNull()
                    ?.let { SerializableDate(it.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()) }
                val n = maxClicksSelector
                    .takeIf { maxClicksSelectorCheckbox.checked }
                    ?.value?.toIntOrNull()
                val targetPage = if (isFromUrl()) PageByUrl(Url(urlSelector.value)) else HtmlPage(loadedHtml)
                sendData(UserData(targetPage, date, n))
            } else if (form.checkValidity()) {
                fileSelector.required = true
                val msg = fileSelector.validationMessage
                fileSelector.required = false
                fileReporter.setCustomValidity(msg)
                fileReporter.pattern = "_"
                console.log(msg)
                form.reportValidity()
                fileReporter.pattern = ".*"
            }
        }
        fileReporter.onclick = {
            fileSelector.click()
            updateFileStatus()
            updateSourceType()
            false
        }
        fileReporter.onselect = { fileReporter.setSelectionRange(0, 0); }
        fileReporter.onkeydown = {
            if (it.keyCode == 13) { //enter
                fileSelector.click()
                false
            } else it.keyCode == 9 //tab
        }
        deleteButton?.onclick = { sendData(null) }
        form.onplay = { false }
        form.onsubmit = { false }
        updateFileStatus()
        updateSourceType()
    }
}
