package net.robi42.tempmunger.search.dao

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import net.robi42.tempmunger.domain.model.TemporalEntry.Companion.INDEX_NAME
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.Client
import org.elasticsearch.common.unit.TimeValue
import org.junit.Test
import org.mockito.Matchers.any
import org.mockito.Matchers.anyInt
import org.mockito.Mockito.verify
import org.springframework.data.elasticsearch.core.ElasticsearchOperations

class ElasticsearchRepositoryTest {

    private val client = mock<Client>()
    private val elasticsearchTemplate = mock<ElasticsearchOperations>()
    private val searchProvider = ElasticsearchRepository(client, INDEX_NAME, ObjectMapper(), elasticsearchTemplate)

    private val requestBuilder = mock<SearchRequestBuilder>()
    private val searchResponse = mock<SearchResponse>()

    @Test fun searches() {
        whenever(client.prepareSearch(INDEX_NAME)).thenReturn(requestBuilder)
        whenever(requestBuilder.setFrom(anyInt())).thenReturn(requestBuilder)
        whenever(requestBuilder.setSize(anyInt())).thenReturn(requestBuilder)
        whenever(requestBuilder.setFetchSource(true)).thenReturn(requestBuilder)
        whenever(requestBuilder.get(any(TimeValue::class.java))).thenReturn(searchResponse)

        assertThat(searchProvider.search()).isInstanceOf(JsonNode::class.java)
        verify(client).prepareSearch(INDEX_NAME)
    }

}
