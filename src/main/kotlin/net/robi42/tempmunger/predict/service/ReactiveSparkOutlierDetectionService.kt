package net.robi42.tempmunger.predict.service

import net.robi42.tempmunger.predict.dao.SparkTemporalClusteringPredictor
import org.apache.spark.api.java.JavaPairRDD
import org.springframework.stereotype.Service
import reactor.bus.Event
import reactor.bus.EventBus
import scala.Tuple2

private const val FIRST_CLASS = 0
private const val SECOND_CLASS = 1

@Service class ReactiveSparkOutlierDetectionService(private val predictor: SparkTemporalClusteringPredictor,
                                                    private val eventBus: EventBus) : ReactiveOutlierDetectionService {

    override fun detectOutliers(temporalFields: Set<String>) {
        if (temporalFields.isEmpty()) return

        val prediction = predictor.predict(temporalFields, classes = 2)
        val firstClass = filter(prediction, { it._2 == FIRST_CLASS })
        val secondClass = filter(prediction, { it._2 == SECOND_CLASS })
        val outliers = determineOutliers(firstClass, secondClass)

        eventBus.notify(EVENT_OUTLIERS, Event.wrap(outliers))
    }

    private fun determineOutliers(firstClass: Set<String>, secondClass: Set<String>) =
            if (firstClass.size < secondClass.size)
                firstClass
            else
                secondClass

    private fun filter(prediction: JavaPairRDD<String, Int>, predicateWith: (Tuple2<String, Int>) -> Boolean) =
            prediction.filter { predicateWith(it) }.cache()
                    .collectAsMap()
                    .keys.toSet()

}
