const val urlChars: Int = 10

abstract class UrlDatabase {
    enum class OperationResult { Done, AccessDenied, NothingToDo }

    protected abstract fun getUnsafeConfig(url: Url): OwnerConfig?
    protected abstract fun registerLink(url: Url, ownerConfig: OwnerConfig)
    protected abstract fun unregisterOnlyLink(url: Url)

    private infix fun Url.isOwnerFor(config: OwnerConfig) = config.ownerUrl == this

    @Synchronized
    fun getConfig(url: Url): Config? {
        val ownerConfig = getUnsafeConfig(url)
        return when {
            ownerConfig == null -> null
            url isOwnerFor ownerConfig -> ownerConfig
            else -> {
                ownerConfig.publicConfig
                    .takeIf {
                        val expirationDate = it.data.expirationDate
                        expirationDate == null || expirationDate >= SerializableDate.now()
                    }
                    ?.takeIf {
                        val maxClicks = it.data.maxClicks
                        maxClicks == null || maxClicks > 0
                    }
                    ?.also { publicConfig ->
                        val maxClicks = publicConfig.data.maxClicks
                        if (maxClicks != null) {
                            val newConfig = ownerConfig.copy(
                                publicConfig =
                                publicConfig.copy(data = publicConfig.data.copy(maxClicks = maxClicks - 1))
                            )
                            registerLink(ownerConfig.ownerUrl, newConfig)
                            registerLink(publicConfig.publicUrl, newConfig)
                        }
                    }
            }
        }
    }

    @Synchronized
    fun registerUser(data: UserData): OwnerConfig {
        tailrec fun generateLink(got: Url?): Url = Url(randomString())
            .takeIf { getUnsafeConfig(it) == null }
            ?.takeIf { it != got }
            ?: generateLink(got)

        val ownerUrl = generateLink(null)
        val publicUrl = generateLink(ownerUrl)
        return OwnerConfig(ownerUrl, PublicConfig(publicUrl, data)).also { registerUser(it) }
    }

    private fun registerUser(ownerConfig: OwnerConfig) {
        registerLink(ownerConfig.ownerUrl, ownerConfig)
        registerLink(ownerConfig.publicConfig.publicUrl, ownerConfig)
    }

    @Synchronized
    fun unregisterUser(url: Url): OperationResult = when (val config = getConfig(url)) {
        null -> OperationResult.NothingToDo
        is PublicConfig -> OperationResult.AccessDenied
        is OwnerConfig -> unregisterUser(config).let { OperationResult.Done }
    }

    private fun unregisterUser(ownerConfig: OwnerConfig) {
        unregisterOnlyLink(ownerConfig.publicConfig.publicUrl)
        unregisterOnlyLink(ownerConfig.ownerUrl)
    }

    @Synchronized
    fun changeUserData(url: Url, newData: UserData): OperationResult = when (val config = getConfig(url)) {
        null -> OperationResult.NothingToDo
        is PublicConfig -> OperationResult.AccessDenied
        is OwnerConfig -> changeUserData(config, newData).let { OperationResult.Done }
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
