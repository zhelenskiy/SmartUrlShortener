import kotlinx.html.*

fun BODY.ownerEditor(config: OwnerConfig?) {
    val editing = config != null
    label {
        id = "init-date"
        style = "display:none"
        +(config?.publicConfig?.data?.expirationDate?.millis?.toString() ?: "")
    }
    p {
        a("$serverHost/") {
            i {
                +"Main page"
            }
        }
    }
    if (editing) {
        b {
            p {
                label {
                    +"Public link: "
                }
                a(href = "$serverHost/redirect?k=${config!!.publicConfig.publicUrl.text}", target = "_blank") {
                    +config.publicConfig.publicUrl.text
                }
            }
            p {
                label {
                    +"Owner link: "
                }
                a(href = "$serverHost/redirect?k=${config!!.ownerUrl.text}", target = "_blank") {
                    id = "owner-link"
                    +config.ownerUrl.text
                }
            }
        }
    }
    p {
        form {
            id = "input-form"
            h3 {
                +"Source to show"
            }
            val fromUrl: Boolean = config?.publicConfig?.data?.redirect is PageByUrl?
            div {
                p {
                    input(InputType.radio, name = "source") {
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
                    input(InputType.radio, name = "source") {
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
                id = "file-selector-div"
                label { +"Source file: " }
                input(InputType.file) {
                    id = "file-selector"
                    accept = "text/html"
                }
            }
            div {
                id = "url-selector-div"
                label { +"Url to redirect: " }
                input(InputType.url) {
                    id = "url-selector"
                    value = (config?.publicConfig?.data?.redirect as? PageByUrl)?.url?.text ?: ""
                }
            }
            div {
                p {
                    a(href = config?.ownerUrl?.text?.let { "${serverHost}/redirect?k=$it&show=true" } ?: "about:blank",
                        target = "_blank") {
                        id = "show-link"
                        +"Preview"
                    }
                }
            }
            h3 {
                +"Limitations"
            }
            div {
                p {
                    val useDate = config?.publicConfig?.data?.expirationDate != null
                    input(InputType.checkBox) {
                        id = "date-selector-checkbox"
                        checked = useDate
                        +"Expiration date: "
                    }
                    input(InputType.dateTimeLocal) {
                        id = "date-selector"
                        hidden = !useDate
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
                    input(InputType.checkBox) {
                        id = "max-clicks-selector-checkbox"
                        checked = useMaxClicks
                        +"Max number of clicks: "
                    }
                    input(InputType.number) {
                        id = "max-clicks-selector"
                        min = "0"
                        hidden = !useMaxClicks
                        required = useMaxClicks
                        value = config?.publicConfig?.data?.maxClicks?.toString() ?: ""
                    }
                }
            }
            div {
                p {
                    input(InputType.submit) {
                        id = "submit-button"
                        value = (if (editing) "Modify" else "Create")
                    }
                    if (editing) {
                        input(InputType.submit) {
                            id = "delete-button"
                            value = "Delete"
                        }
                    }
                }
            }
            div {
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