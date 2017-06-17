package net.robi42.tempmunger.transform

import net.robi42.tempmunger.config.ApplicationProperties
import org.apache.spark.SparkConf
import org.apache.spark.api.java.JavaSparkContext
import org.springframework.stereotype.Component

@Component class SparkEsContextProvider(properties: ApplicationProperties) {

    companion object {
        const val ES_DOC_ID = "_id"

        private const val TRUE = "true"
        private const val FALSE = "false"
    }

    final val context: JavaSparkContext

    init {
        val appName = properties.spring.application.name!!
        val sparkConf = SparkConf()
                .setAppName(appName)
                .set("spark.app.id", appName)
                .set("spark.driver.allowMultipleContexts", TRUE)
                .set("spark.ui.enabled", FALSE)
                .set("es.index.auto.create", TRUE)
                .set("es.mapping.id", ES_DOC_ID)
                .set("es.mapping.exclude", ES_DOC_ID)
                .set("es.mapping.date.rich", FALSE)
                .set("es.write.operation", "upsert")
                .setMaster("local[*]")

        context = JavaSparkContext(sparkConf)
    }

}
