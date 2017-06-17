package net.robi42.tempmunger.search.dao

import net.robi42.tempmunger.ApplicationTests
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import javax.inject.Inject

class ElasticsearchRepositoryIT : ApplicationTests() {

    @Inject private lateinit var repository: ElasticsearchRepository

    @Test fun `finds all`() {
        assertThat(repository.findAll()).isNotEmpty
    }

}
