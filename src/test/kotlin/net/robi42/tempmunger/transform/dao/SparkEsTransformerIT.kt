package net.robi42.tempmunger.transform.dao

import net.robi42.tempmunger.ApplicationTests
import net.robi42.tempmunger.search.dao.SearchRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Instant
import javax.inject.Inject

private const val MISSING_VALUE = "foo"
private const val TEMPORAL_FIELD = "Date"

class SparkEsTransformerIT : ApplicationTests() {

    @Inject private lateinit var sparkTransformer: SparkEsTransformer
    @Inject private lateinit var searchRepository: SearchRepository

    @Test fun `transforms missing value entries`() {
        assertThat(search()).contains(MISSING_VALUE)

        sparkTransformer.transformMissing(TEMPORAL_FIELD, to = Instant.now())

        assertThat(search()).doesNotContain(MISSING_VALUE)
    }

    private fun search(): String {
        val searchResult = searchRepository.search(
                from = 0,
                size = 500,
                sort = null,
                ids = emptySet(),
                query = mapOf("scriptFields" to listOf(TEMPORAL_FIELD)))

        return json(searchResult)
    }

}
