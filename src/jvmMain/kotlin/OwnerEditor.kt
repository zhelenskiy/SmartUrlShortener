import kotlinx.html.*

fun BODY.ownerEditor(config: OwnerConfig?) {
    val editing = config != null
    div("main-block") {
        label {
            id = "init-date"
            style = "display:none"
            +(config?.publicConfig?.data?.expirationDate?.millis?.toString() ?: "")
        }
        h2 {
            a("$serverHost/") {
                i {
                    +"Main page"
                }
            }
        }
        if (editing) {
            b {
                label {
                    +"Owner ID: "
                }
                label {
                    id = "owner-id"
                    +config!!.ownerUrl.text
                }
            }
            b {
                label {
                    +"Public link: "
                }
                val href = "$serverHost/${config!!.publicConfig.publicUrl.text}"
                a(href = href, target = "_blank") {
                    +href
                }
            }
            b {
                label {
                    +"Owner link: "
                }
                val href = "$serverHost/${config!!.ownerUrl.text}"
                a(href = href, target = "_blank") {
                    +href
                }
            }
        }
        form {
            id = "input-form"
            h3("header") {
                +"Source to show"
            }
            val fromUrl: Boolean = config?.publicConfig?.data?.redirect is PageByUrl?
            div {
                p {
                    input(InputType.radio, name = "source", classes = "checker") {
                        value = "url"
                        checked = fromUrl
                        id = "source-url"
                        required = fromUrl
                    }
                    label {
                        htmlFor = "source-url"
                        +"Link"
                    }
                }

                p {
                    input(InputType.radio, name = "source", classes = "checker") {
                        value = "file"
                        checked = !fromUrl
                        id = "source-file"
                        required = !fromUrl
                    }
                    label {
                        htmlFor = "source-file"
                        +"File"
                    }
                }
            }
            div {
                style = "display: flex;align-items: center"
                div {
                    id = "url-selector-div"
                    label { +"Url to redirect: " }
                    input(InputType.url, classes = "text") {
                        id = "url-selector"
                        value = (config?.publicConfig?.data?.redirect as? PageByUrl)?.url?.text ?: ""
                        placeholder = "Write your URL here"
                    }
                }
                div {
                    id = "file-selector-div"
                    div {
                        style = "display: flex; flex-wrap: wrap; align-items: center;"
                        label {
                            +"Source file: "
                        }
                        label(classes = "file-label") {
                            id = "file-selector-label"
                            htmlFor = "file-selector"
                            +"choose file..."
                        }
                        input(InputType.file) {
                            id = "file-selector"
                            accept = "text/html"
                            style = "display:none;"
                        }
                        i {
                            label {
                                id = "file-status"
                            }
                        }
                    }
                }
                label(classes = "text") {
                    style = "width:0;padding-left:0;padding-right:0;opacity:0%"
                    +"a"
                }
                label(classes = "file-label") {
                    style = "width:0;padding-left:0;padding-right:0;opacity:0%"
                    +"a"
                }
            }
            div {
//                            id = "code-source-block"
                style = "display:none"
//                            style = "display:inline-block;overflow:auto;resize:both;border:solid 1px black"
//                            link(
//                                href = "https://cdnjs.cloudflare.com/ajax/libs/prism/1.22.0/themes/prism.css",
//                                rel = "stylesheet"
//                            )
                script(src = "https://cdnjs.cloudflare.com/ajax/libs/prism/1.22.0/components/prism-core.min.js") { }
                script(src = "https://cdnjs.cloudflare.com/ajax/libs/prism/1.22.0/plugins/autoloader/prism-autoloader.min.js") { }
                pre {
                    code(classes = "language-html") {
//                                    id = "code-source"
//                                    +"You will see your code here"
                    }
                }
                label {
                    id = "init-user-html"
                    +((config?.publicConfig?.data?.redirect as? HtmlPage)?.code ?: "")
                }
            }
            div("button-group") {
                button(classes = "button first-button") {
                    id = "show-link"
                    +"Preview"
                }
                button(classes = "button last-button") {
                    id = "clear-button"
                    +"Clear"
                }
                button(classes = "button last-button") {
                    id = "highlight-button"
                    style = "display:none"
                    +"Show source code"
                }
            }
            h3("header") {
                +"Limitations"
            }
            div {
                p {
                    val useDate = config?.publicConfig?.data?.expirationDate != null
                    input(InputType.checkBox, classes = "checker") {
                        id = "date-selector-checkbox"
                        checked = useDate
                    }
                    label {
                        htmlFor = "date-selector-checkbox"
                        +"Expiration date: "
                    }
                    input(InputType.dateTimeLocal, classes = "text") {
                        id = "date-selector"
//                            hidden = !useDate
                        required = useDate
//                        value = config?.publicConfig?.data?.expirationDate?.millis?.let {
//                            Instant.fromEpochMilliseconds(it).toString()
//                        } ?: ""
                    }
                }
            }
            div {
                p {
                    val useMaxClicks = config?.publicConfig?.data?.maxClicks != null
                    input(InputType.checkBox, classes = "checker") {
                        id = "max-clicks-selector-checkbox"
                        checked = useMaxClicks
                    }
                    label {
                        htmlFor = "max-clicks-selector-checkbox"
                        +"Max number of clicks: "
                    }
                    input(InputType.number, classes = "text") {
                        id = "max-clicks-selector"
                        min = "0"
//                            hidden = !useMaxClicks
                        required = useMaxClicks
                        value = config?.publicConfig?.data?.maxClicks?.toString() ?: ""
                        placeholder = "Write a number here"
                    }
                }
            }
            p {
                div("button-group") {
//                    style = "display:flex; flex-wrap: wrap;"
                    input(InputType.submit, classes = "button first-button${if (editing) "" else " last-button"}") {
//                        style = "flex: 1 1 auto;"
                        id = "submit-button"
                        value = (if (editing) "Modify" else "Create")
                    }
                    if (editing) {
                        input(InputType.submit, classes = "button last-button") {
//                            style = "flex: 1 1 auto;"
                            id = "delete-button"
                            value = "Delete"
                        }
                    }
                }
                p {
                    i {
                        label {
                            id = "status"
                        }
                    }
                }
            }
        }
    }
}