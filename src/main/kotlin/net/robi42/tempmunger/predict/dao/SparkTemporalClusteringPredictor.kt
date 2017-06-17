package net.robi42.tempmunger.predict.dao

import net.robi42.tempmunger.error.TempMungerParseException
import net.robi42.tempmunger.transform.SparkEsRddProvider
import net.robi42.tempmunger.transform.util.LenientDateTimeParser
import net.robi42.tempmunger.util.logger
import net.robi42.tempmunger.util.toEpochMilli
import org.apache.spark.api.java.JavaPairRDD
import org.apache.spark.api.java.JavaRDD
import org.apache.spark.mllib.clustering.KMeans
import org.apache.spark.mllib.clustering.KMeansModel
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.linalg.Vectors
import org.springframework.stereotype.Component
import scala.Tuple2
import java.util.Optional.empty
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit.SECONDS

private const val ITERATIONS = 20
private const val RUNS = 3

@Component class SparkTemporalClusteringPredictor(private val rddProvider: SparkEsRddProvider) : SparkPredictor {

    companion object {
        private val dateTimeParser = LenientDateTimeParser()

        private fun vectorValues(document: Map<String, Any>, fieldNames: Set<String>) = fieldNames.map { field ->
            val temporal = document[field].toString()

            try {
                val dateTime = dateTimeParser.parse(temporal)
                val epochMilli = dateTime.toEpochMilli()
                epochMilli.toDouble()
            } catch (e: TempMungerParseException) {
                Double.MAX_VALUE
            }
        }.toDoubleArray()

        private fun predict(entry: Pair<String, Vector>, model: KMeansModel): Tuple2<String, Int> {
            val id = entry.first
            val point = entry.second
            val clusterClass = model.predict(point)

            return Tuple2(id, clusterClass)
        }
    }

    private val log by logger()

    private val rdd: JavaPairRDD<String, Map<String, Any>>
        get() = rddProvider.rdd

    override fun predict(fieldNames: Set<String>, classes: Int): JavaPairRDD<String, Int> {
        log.debug("Clustering with fields: {}", fieldNames)

        val data = vectorize(rdd, fieldNames)
        log.debug("Vectorized data with epoch millis")

        val (trainingSet) = randomSplit(data)
        log.debug("Created training set via random split")

        val vectors = trainingSet.map { it.second }.cache()
        log.debug("Created vectors for model")

        val model = createModel(vectors, classes)
        log.debug("Created predictive model")

        return data.mapToPair { predict(it, model) }.cache()
    }

    override fun updateRdd() {
        rddProvider.updateRdd(query = empty())
        SECONDS.sleep(1) // Wait for ES RDD refresh.
    }

    private fun vectorize(rdd: JavaPairRDD<String, Map<String, Any>>, fieldNames: Set<String>) = rdd.map {
        val id = it._1
        val document = it._2
        val values = vectorValues(document, fieldNames)

        id to Vectors.dense(values)
    }.cache()

    private fun randomSplit(data: JavaRDD<Pair<String, Vector>>) = data.randomSplit(weights(0.9 to 0.1), seed())

    private fun weights(split: Pair<Double, Double>) = arrayOf(split.first, split.second).toDoubleArray()

    private fun seed() = ThreadLocalRandom.current().nextLong()

    private fun createModel(vectors: JavaRDD<Vector>, classes: Int)
            = KMeans.train(vectors.rdd(), classes, ITERATIONS, RUNS)

}
