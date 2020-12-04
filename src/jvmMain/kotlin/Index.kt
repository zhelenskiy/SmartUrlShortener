import kotlinx.html.*

fun HTML.index() {
    head {
        title("Smart URL Shortener")
        commonHeadPart()
    }
    body(classes = "colorful-background") {
        div("main-block") {
            style ="margin-left:30%;margin-right:30%;margin-top:10%;margin-bottom:10%;"
            h3 {
                div("header") {
                    h2 {
                        +"Welcome to Smart URL Shortener"
                    }
                }
                div {
                    div("header") {
                        b {
                            +"This service allows you to create customizable short urls."
                        }
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
                div("header") {
                    +"Try using it right now!"
                }
                h2 {
                    form {
                        action = "$serverHost/create"
                        button(classes = "button bigButton") {
                            type = ButtonType.submit
                            style = "width:100%;border-radius:60px"
                            +"Shorten"
                        }
                    }
                }
                div {
                    id = "root"
                }
            }
        }
    }
}