package net.robi42.tempmunger.transform

import com.google.common.collect.ImmutableMap
import net.robi42.tempmunger.domain.model.TemporalEntry
import net.robi42.tempmunger.transform.SparkEsContextProvider.Companion.ES_DOC_ID
import org.apache.spark.api.java.JavaPairRDD
import org.apache.spark.api.java.JavaSparkContext
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.spark.rdd.api.java.JavaEsSpark.esRDD
import org.elasticsearch.spark.rdd.api.java.JavaEsSpark.saveToEs
import org.springframework.stereotype.Component
import java.util.*

@Component class SparkEsRddProvider(private val contextProvider: SparkEsContextProvider) {

    companion object {
        const val ES_RESOURCE = "${TemporalEntry.INDEX_NAME}/${TemporalEntry.TYPE_NAME}"
    }

    private val context: JavaSparkContext
        get() = contextProvider.context

    lateinit var rdd: JavaPairRDD<String, Map<String, Any>>

    final fun updateRdd(query: Optional<QueryBuilder>) {
        rdd = if (query.isPresent)
            esRDD(context, ES_RESOURCE, query.get().toJson()).cache()
        else
            esRDD(context, ES_RESOURCE).init().cache()
    }

    private fun QueryBuilder.toJson() = """{"query": $this}"""

    private fun JavaPairRDD<String, Map<String, Any>>.init(): JavaPairRDD<String, Map<String, Any>> {
        saveToEs(this.map {
            val id = it._1
            val values = it._2

            ImmutableMap.builder<String, Any>()
                    .put(ES_DOC_ID, id)
                    .putAll(values)
                    .build()
        }, ES_RESOURCE)

        return this
    }

}
