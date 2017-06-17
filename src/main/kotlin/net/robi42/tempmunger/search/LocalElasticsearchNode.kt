package net.robi42.tempmunger.search

import org.elasticsearch.Version
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.env.Environment
import org.elasticsearch.node.Node
import org.elasticsearch.script.groovy.GroovyPlugin
import java.io.File

internal class LocalElasticsearchNode(clusterName: String) : Node(
        Environment(Settings.builder()
                .put("local", true)
                .put("name", "TempMunger")
                .put("cluster.name", clusterName)
                .put("path.home", File("").absolutePath)
                .put("script.inline", true)
                .build()),
        Version.CURRENT,
        setOf(GroovyPlugin::class.java))
