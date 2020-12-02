abstract class Database {
    enum class OperationResult { Done, AccessDenied, NothingToDo }

    protected abstract fun getUnsafeConfig(url: Url): Config?
    protected abstract fun <T : Config> registerNewLink(url: Url, value: (Url) -> T): T?
    protected abstract fun unregisterOnlyLink(url: Url)

    @Synchronized
    fun getConfig(url: Url): Config? = when (val config = getUnsafeConfig(url)) {
        null -> null
        is OwnerConfig -> config
        is PublicConfig -> config
            .takeIf {
                (it.data.expirationDate == null || it.data.expirationDate >= SerializableDate.now()) &&
                        (it.data.maxClicks == null || it.data.maxClicks!! > 0)
            }
            ?.let { PublicConfig(it.publicUrl, it.data.copy()) }
            ?.also { config.data.maxClicks = config.data.maxClicks?.dec() }
    }

    @Synchronized
    fun registerUser(data: UserData): OwnerConfig {
        tailrec fun <T : Config> generateLink(then: (Url) -> T): T =
            registerNewLink(Url(randomString()), then) ?: generateLink(then)

        return generateLink { ownerUrl ->
            OwnerConfig(ownerUrl, generateLink { publicUrl -> PublicConfig(publicUrl, data) })
        }
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
        unregisterUser(ownerConfig)
        val publicConfig = registerNewLink(ownerConfig.publicConfig.publicUrl) { PublicConfig(it, newData) }!!
        registerNewLink(ownerConfig.ownerUrl) { OwnerConfig(it, publicConfig) }!!
    }

    private fun randomString(): String {
        val characters = ('a'..'z') + ('0'..'9')
        return String(CharArray(10) { characters.random() })
    }
}
