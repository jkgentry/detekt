package io.gitlab.arturbosch.detekt.api

import io.gitlab.arturbosch.detekt.api.internal.ValidatableConfiguration
import io.gitlab.arturbosch.detekt.api.internal.validateConfig
import org.yaml.snakeyaml.Yaml
import java.io.BufferedReader
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

/**
 * Config implementation using the yaml format. SubConfigurations can return sub maps according to the
 * yaml specification.
 */
@Suppress("UNCHECKED_CAST")
class YamlConfig internal constructor(
    val properties: Map<String, Any>,
    override val parent: HierarchicalConfig.Parent?
) : BaseConfig(), ValidatableConfiguration {

    override fun subConfig(key: String): Config {
        val subProperties = properties.getOrElse(key) { mapOf<String, Any>() }
        return YamlConfig(subProperties as Map<String, Any>, HierarchicalConfig.Parent(this, key))
    }

    override fun <T : Any> valueOrDefault(key: String, default: T): T {
        val result = properties[key]
        return valueOrDefaultInternal(key, result, default) as T
    }

    override fun <T : Any> valueOrNull(key: String): T? {
        return properties[key] as? T?
    }

    override fun toString(): String {
        return "YamlConfig(properties=$properties)"
    }

    override fun validate(baseline: Config, excludePatterns: Set<Regex>): List<Notification> =
        validateConfig(this, baseline, excludePatterns)

    companion object {

        private const val YAML = ".yml"

        /**
         * Factory method to load a yaml configuration. Given path must exist and end with "yml".
         */
        fun load(path: Path): Config {
            require(Files.exists(path)) { "Unable to load config from '$path': file does not exist!" }
            require(path.toString().endsWith(YAML)) { "Invalid config file name $path: file does not end with $YAML!" }
            return load(Files.newBufferedReader(path))
        }

        /**
         * Factory method to load a yaml configuration from a URL.
         */
        fun loadResource(url: URL): Config = load(url.openStream().bufferedReader())

        private fun load(reader: BufferedReader): Config = reader.use {
            val yamlInput = it.lineSequence().joinToString("\n")
            if (yamlInput.isEmpty()) {
                Config.empty
            } else {
                val map: Any = Yaml().load(yamlInput)
                if (map is Map<*, *>) {
                    YamlConfig(map as Map<String, Any>, parent = null)
                } else {
                    throw Config.InvalidConfigurationError()
                }
            }
        }
    }
}
