package net.robi42.tempmunger.search.dao

import com.fasterxml.jackson.databind.JsonNode
import org.elasticsearch.index.query.QueryBuilder
import java.util.*

interface SearchRepository {

    /** Main search interface. */
    fun search(from: Int = 0,
               size: Int = 20,
               ids: Set<UUID> = emptySet(),
               sort: String? = null,
               query: Map<String, Any> = emptyMap()): JsonNode

    /** Helper for creating queries. */
    fun createQuery(q: String): QueryBuilder

    /** Returns all documents currently stored. */
    fun findAll(): List<MutableMap<String, Any>>

    /** Avg aggregation on temporal fields. */
    fun avgAgg(temporalField: String): Double

}
