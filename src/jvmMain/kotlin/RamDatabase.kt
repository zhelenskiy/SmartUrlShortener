class RamDatabase: Database() {
    private val map: MutableMap<Url, Config> = mutableMapOf()

    override fun getUnsafeConfig(url: Url): Config? = map[url]

    override fun <T : Config> registerNewLink(url: Url, value: (Url) -> T): T? =
        if (url in map) null else value(url).also { map[url] = it }

    override fun unregisterOnlyLink(url: Url) = map.remove(url).let { }
}