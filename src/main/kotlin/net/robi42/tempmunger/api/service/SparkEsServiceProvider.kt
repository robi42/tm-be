package net.robi42.tempmunger.api.service

import net.robi42.tempmunger.predict.dao.SparkTemporalClusteringPredictor
import net.robi42.tempmunger.predict.service.ReactiveSparkOutlierDetectionService
import net.robi42.tempmunger.transform.SparkEsContextProvider
import net.robi42.tempmunger.transform.dao.SparkEsTransformer
import org.springframework.stereotype.Component

@Component class SparkEsServiceProvider(private val contextProvider: SparkEsContextProvider,
                                        val transformer: SparkEsTransformer,
                                        val predictor: SparkTemporalClusteringPredictor,
                                        val outlierDetectionService: ReactiveSparkOutlierDetectionService) {

    val context get() = contextProvider.context

}
