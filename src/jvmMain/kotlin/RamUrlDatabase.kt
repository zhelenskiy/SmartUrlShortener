class RamUrlDatabase : UrlDatabase() {
    private val map: MutableMap<Url, OwnerConfig> = mutableMapOf()

    override fun getUnsafeConfig(url: Url): OwnerConfig? = map[url]
    override fun registerLink(url: Url, ownerConfig: OwnerConfig) {
        map[url] = ownerConfig
    }

    override fun unregisterOnlyLink(url: Url) {
        map.remove(url)
    }
}