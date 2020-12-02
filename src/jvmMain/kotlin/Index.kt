import kotlinx.html.*

fun HTML.index() {
    head {
        title("Smart URL Shortener")
    }
    body {
        div {
            h1 {
                +"Welcome to Smart URL Shortener"
            }
        }
        div {
            h3 {
                +"This service allows you to create customizable short urls."
            }
            ul {
                li {
                    +("You can choose expiration date, maximum clicks done by users " +
                            "or even specify custom HTML pages to show instead of redirecting.")
                }
                li {
                    +"You can modify settings, or delete the link manually with your owner's link."
                }
                li {
                    +"The owner's link is alive until the owner deletes it."
                }
            }
        }
        div {
            h4 {
                +"Try using it right now!"
            }
        }
        div {
            form {
                action = "$serverHost/create"
                button {
                    type = ButtonType.submit
                    +"Shorten"
                }
            }
        }
        div {
            id = "root"
        }
    }
}