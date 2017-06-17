package net.robi42.tempmunger.predict.dao

import org.apache.spark.api.java.JavaPairRDD

interface SparkPredictor {

    /** Predicts classes of given field names via unsupervised learning. */
    fun predict(fieldNames: Set<String>, classes: Int): JavaPairRDD<String, Int>

    /** Use to update cached **R**esilient **D**istributed **D**ataset. */
    fun updateRdd()

}
