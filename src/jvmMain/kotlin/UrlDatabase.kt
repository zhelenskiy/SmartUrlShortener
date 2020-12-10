const val urlChars: Int = 10

abstract class UrlDatabase {
    enum class OperationResult { Done, AccessDenied, NothingToDo }

    protected abstract fun getUnsafeConfig(url: Url): OwnerConfig?
    protected abstract fun registerLink(url: Url, ownerConfig: OwnerConfig)
    protected abstract fun unregisterOnlyLink(url: Url)
    protected abstract fun <R> atomic(f: UrlDatabase.() -> R): R

    private infix fun Url.isOwnerFor(config: OwnerConfig) = config.ownerUrl == this

    fun getConfig(url: Url): Config? = atomic {
        val ownerConfig = getUnsafeConfig(url)
        fun <T> T?.isNullOr(predicate: (T) -> Boolean) = this == null || predicate(this)
        when {
            ownerConfig == null -> null
            url isOwnerFor ownerConfig -> ownerConfig
            else -> ownerConfig.publicConfig
                .takeIf { config -> config.data.expirationDate.isNullOr { it >= SerializableDate.now() } }
                ?.takeIf { config -> config.data.maxClicks.isNullOr { it > 0 } }
                ?.also { config ->
                    config.data.maxClicks?.let {
                        ownerConfig.copy(publicConfig = config.copy(data = config.data.copy(maxClicks = it - 1)))
                    }?.let(::registerUser)
                }
        }
    }

    fun registerUser(data: UserData): OwnerConfig = atomic {
        tailrec fun generateLink(got: Url?): Url = Url(randomString())
            .takeIf { getUnsafeConfig(it) == null }
            ?.takeIf { it != got }
            ?: generateLink(got)

        val ownerUrl = generateLink(null)
        val publicUrl = generateLink(ownerUrl)
        OwnerConfig(ownerUrl, PublicConfig(publicUrl, data)).also(::registerUser)
    }

    private fun registerUser(ownerConfig: OwnerConfig) {
        registerLink(ownerConfig.ownerUrl, ownerConfig)
        registerLink(ownerConfig.publicConfig.publicUrl, ownerConfig)
    }

    fun unregisterUser(url: Url): OperationResult = atomic {
        when (val config = getConfig(url)) {
            null -> OperationResult.NothingToDo
            is PublicConfig -> OperationResult.AccessDenied
            is OwnerConfig -> unregisterUser(config).let { OperationResult.Done }
        }
    }

    private fun unregisterUser(ownerConfig: OwnerConfig) {
        unregisterOnlyLink(ownerConfig.publicConfig.publicUrl)
        unregisterOnlyLink(ownerConfig.ownerUrl)
    }

    fun changeUserData(url: Url, newData: UserData): OperationResult = atomic {
        when (val config = getConfig(url)) {
            null -> OperationResult.NothingToDo
            is PublicConfig -> OperationResult.AccessDenied
            is OwnerConfig -> changeUserData(config, newData).let { OperationResult.Done }
        }
    }

    private fun changeUserData(ownerConfig: OwnerConfig, newData: UserData) {
        val newConfig = ownerConfig.copy(publicConfig = ownerConfig.publicConfig.copy(data = newData))
        registerUser(newConfig)
    }

    private fun randomString(): String {
        val characters = ('a'..'z') + ('0'..'9')
        return String(CharArray(urlChars) { characters.random() })
    }
}
