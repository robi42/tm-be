package net.robi42.tempmunger.search.dao

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import net.robi42.tempmunger.domain.model.TemporalEntry
import net.robi42.tempmunger.error.TempMungerException
import net.robi42.tempmunger.error.TempMungerValidationException
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.ElasticsearchTimeoutException
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.Client
import org.elasticsearch.common.unit.TimeValue.timeValueMinutes
import org.elasticsearch.common.unit.TimeValue.timeValueSeconds
import org.elasticsearch.common.xcontent.ToXContent.EMPTY_PARAMS
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder
import org.elasticsearch.index.query.MultiMatchQueryBuilder.Type.PHRASE_PREFIX
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders.*
import org.elasticsearch.script.Script
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.aggregations.AggregationBuilders.avg
import org.elasticsearch.search.aggregations.metrics.avg.Avg
import org.elasticsearch.search.sort.SortOrder.ASC
import org.elasticsearch.search.sort.SortOrder.DESC
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.stereotype.Repository
import java.io.IOException
import java.util.*

private const val MESSAGE_SEARCH_ERROR = "Error getting search response"
private const val FIELD_TYPE = "type"
private const val AGG_AVG = "avg"

@Repository class ElasticsearchRepository(private val client: Client,
                                          @Value(TemporalEntry.INDEX_NAME)
                                          private val indexName: String,
                                          private val objectMapper: ObjectMapper,
                                          val elasticsearchTemplate: ElasticsearchOperations) : SearchRepository {

    override fun search(from: Int,
                        size: Int,
                        ids: Set<UUID>,
                        sort: String?,
                        query: Map<String, Any>): JsonNode {
        val searchResponse = buildSearchRequest(from, size, ids, sort, query)
                .get(timeoutTenSeconds())

        try {
            val json = getJson(searchResponse)
            return objectMapper.readTree(json.string())
        } catch (e: Exception) {
            when (e) {
                is ElasticsearchTimeoutException, is IOException ->
                    throw TempMungerException(MESSAGE_SEARCH_ERROR, e)
                else -> throw e
            }
        }
    }

    private fun buildSearchRequest(from: Int,
                                   size: Int,
                                   ids: Set<UUID>,
                                   sort: String?,
                                   query: Map<String, Any>): SearchRequestBuilder {
        val requestBuilder = with(client.prepareSearch(indexName)) {
            setFrom(from)
            setSize(size)
            setFetchSource(true)
        }

        requestBuilder.let {
            setQuery(query, ids, it)
            setAggs(query, it)
            addScriptFields(query, it)
            addSort(sort, it)
        }

        return requestBuilder
    }

    private fun setQuery(query: Map<String, Any>, ids: Set<UUID>, requestBuilder: SearchRequestBuilder) {
        val q = query["query"] as String?
        val queryBuilder = boolQuery()

        queryBuilder.must(if (q != null && q.isNotBlank()) createQuery(q) else matchAllQuery())

        if (ids.isNotEmpty()) {
            queryBuilder.filter(idsQuery().ids(*ids.map(UUID::toString).toTypedArray()))
        }

        requestBuilder.setQuery(queryBuilder)
    }

    override fun createQuery(q: String): QueryBuilder {
        val mappingProperties = getMappingProperties()
        val fields = getFields(mappingProperties)

        if (fields.isEmpty()) {
            return boolQuery().mustNot(matchAllQuery())
        }
        return multiMatchQuery(q, *fields)
                .type(PHRASE_PREFIX)
                .maxExpansions(10000)
    }

    private fun getMappingProperties(): Map<*, *> {
        val mapping = elasticsearchTemplate.getMapping(TemporalEntry.INDEX_NAME, TemporalEntry.TYPE_NAME)
        return mapping["properties"] as Map<*, *>
    }

    private fun getFields(mappingProperties: Map<*, *>) = mappingProperties.filterValues {
        val field = it as Map<*, *>
        field[FIELD_TYPE]!! == "string"
    }.map {
        "${it.key}.search"
    }.toTypedArray()

    private fun setAggs(query: Map<String, Any>, requestBuilder: SearchRequestBuilder) {
        val aggs = query["aggs"] as Map<*, *>?
        aggs?.let {
            requestBuilder.setAggregations(it)
        }
    }

    private fun addScriptFields(query: Map<String, Any>, requestBuilder: SearchRequestBuilder) {
        val scriptFields = query["scriptFields"] as List<*>?

        validate(scriptFields)

        scriptFields?.forEach {
            val script = epochMilliScript(it as String)
            requestBuilder.addScriptField(it, script)
        }
    }

    private fun validate(scriptFields: List<*>?) {
        if (scriptFields == null || scriptFields.isEmpty()) {
            return
        }

        val temporalFields = getMappingProperties().filterValues {
            val field = it as Map<*, *>
            field[FIELD_TYPE]!! == "date"
        }.map { it.key }.toSet()

        if (temporalFields.intersect(scriptFields).isEmpty()) {
            throw TempMungerValidationException("Invalid field")
        }
    }

    private fun epochMilliScript(field: String) = Script("doc['$field'].date.getMillis()")

    private fun addSort(sort: String?, requestBuilder: SearchRequestBuilder) {
        if (!sort.isNullOrBlank()) {
            requestBuilder.addSort(sort?.replace(Regex("^-"), ""), if (sort!!.startsWith("-")) DESC else ASC)
        } else {
            requestBuilder.addSort("_timestamp", DESC)
        }
    }

    private fun getJson(searchResponse: SearchResponse): XContentBuilder {
        val json = jsonBuilder().startObject()
        searchResponse.toXContent(json, EMPTY_PARAMS)
        json.endObject()

        return json
    }

    override fun findAll(): List<MutableMap<String, Any>> {
        val documents = mutableListOf<SearchHit>()
        var scrollId: String? = null

        try {
            val searchResponse = searchScroll()
            var hits = searchResponse.hits.hits
            documents.addAll(hits)
            scrollId = searchResponse.scrollId

            while (hits.isNotEmpty()) {
                val scrollResponse = scrollWith(scrollId)
                hits = scrollResponse.hits.hits
                documents.addAll(hits)
                scrollId = scrollResponse.scrollId
            }

            return documents.map(SearchHit::sourceAsMap)
        } catch (e: ElasticsearchException) {
            throw TempMungerException("Scrolling search failed", e)
        } finally {
            if (scrollId != null) {
                clearScroll(scrollId)
            }
        }
    }

    private fun searchScroll() =
            client.prepareSearch(TemporalEntry.INDEX_NAME)
                    .setScroll(keepAliveOneMinute())
                    .setQuery(matchAllQuery())
                    .get(timeoutTenSeconds())

    private fun scrollWith(scrollId: String?) =
            client.prepareSearchScroll(scrollId)
                    .setScroll(keepAliveOneMinute())
                    .get(timeoutTenSeconds())

    private fun clearScroll(scrollId: String?) {
        client.prepareClearScroll()
                .addScrollId(scrollId)
                .get(timeoutTenSeconds())
    }

    override fun avgAgg(temporalField: String): Double {
        validate(listOf(temporalField))

        val epochMilliScript = epochMilliScript(temporalField)
        val agg = avg(AGG_AVG)
                .field(temporalField)
                .script(epochMilliScript)

        return client.prepareSearch(TemporalEntry.INDEX_NAME)
                .setSize(0)
                .setQuery(matchAllQuery())
                .addAggregation(agg)
                .get(timeoutTenSeconds())
                .aggregations
                .get<Avg>(AGG_AVG)
                .value
    }

    private fun timeoutTenSeconds() = timeValueSeconds(10)

    private fun keepAliveOneMinute() = timeValueMinutes(1)

}
