package net.robi42.tempmunger.predict.service

const val EVENT_OUTLIERS = "outliers"

interface ReactiveOutlierDetectionService {

    fun detectOutliers(temporalFields: Set<String>)

}
