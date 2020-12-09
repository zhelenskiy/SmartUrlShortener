fun ask(prompt: String): String? {
    print(prompt)
    return readLine()?.takeIf { it.isNotEmpty() }
}

fun main() {
    while (true)
        when (val task = ask("Task to do: ")) {
            null -> {
                println("No debug task specified")
                return
            }
            "show" -> when (val url = ask("Url: ")) {
                null -> urlDatabase.selectAll().joinToString("\n")
                else -> urlDatabase.getConfig(Url(url))
            }
            "remove", "delete" -> when (val url = ask("Url: ")) {
                null -> {
                    ask("Are you sure you want to drop ALL table???\n")
                        ?.takeIf { it.toLowerCase() in listOf("y", "yes") }
                        ?.let { urlDatabase.removeAll() }
                        ?: "Cancelled"
                }
                else -> urlDatabase.unregisterUser(Url(url))
            }
            else -> "Unknown task \"$task\" found"
        }.also {
            println(it)
            println()
        }
}