package net.robi42.tempmunger.importexport.dao

import net.robi42.tempmunger.ApplicationTests
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import javax.inject.Inject

class ElasticsearchCsvExporterIT : ApplicationTests() {

    @Inject private lateinit var exporter: ElasticsearchCsvExporter

    @Test fun exports() {
        val csv = exporter.export()
        assertThat(csv).isNotEmpty()
    }

}
