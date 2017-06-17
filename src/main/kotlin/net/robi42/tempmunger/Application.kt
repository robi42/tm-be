package net.robi42.tempmunger

import com.google.common.io.Resources.getResource
import com.google.common.io.Resources.toByteArray
import net.robi42.tempmunger.api.service.SparkEsServiceProvider
import net.robi42.tempmunger.api.service.TemporalEntryService
import net.robi42.tempmunger.config.ApplicationProperties
import net.robi42.tempmunger.domain.model.TemporalEntry
import net.robi42.tempmunger.importexport.dao.CsvImporter
import net.robi42.tempmunger.search.CustomEntityMapper
import net.robi42.tempmunger.search.LocalElasticsearchNode
import org.elasticsearch.client.Client
import org.elasticsearch.node.Node
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors.newCachedThreadPool
import javax.inject.Inject

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}

@SpringBootApplication class Application {

    @Inject private lateinit var properties: ApplicationProperties

    @Bean fun init(importer: CsvImporter,
                   serviceProvider: SparkEsServiceProvider,
                   entryService: TemporalEntryService) = ApplicationRunner {
        val csv = toByteArray(getResource("muenster-spendings.csv"))
        importer.import(csv, separator = ',')
        serviceProvider.transformer.updateRdd()

        taskExecutor().submit {
            serviceProvider.predictor.updateRdd()
            entryService.detectOutliers()
        }
    }

    @Primary @Bean fun elasticsearchNode(): Node =
            LocalElasticsearchNode(clusterName = properties.spring.application.name!!).start()

    @Primary @Bean fun elasticsearchClient(): Client = elasticsearchNode().client()

    @Primary @Bean fun elasticsearchTemplate(entityMapper: CustomEntityMapper)
            = ElasticsearchTemplate(elasticsearchClient(), entityMapper)

    @Bean fun temporalEntryIndexHealthIndicator(elasticsearchTemplate: ElasticsearchOperations) = HealthIndicator {
        if (elasticsearchTemplate.typeExists(TemporalEntry.INDEX_NAME, TemporalEntry.TYPE_NAME))
            Health.up().build()
        else
            Health.down().build()
    }

    @Bean fun taskExecutor(): ExecutorService = newCachedThreadPool()

    @Bean fun mvcConfigAdapter() = object : WebMvcConfigurerAdapter() {
        override fun addViewControllers(registry: ViewControllerRegistry) {
            registry.apply {
                // Mount REST docs at `/docs`.
                addViewController("/docs").setViewName("forward:/docs/index.html")
                addRedirectViewController("/docs/", "/docs")
            }
        }
    }

}
