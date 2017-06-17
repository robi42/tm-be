package net.robi42.tempmunger

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.io.Resources.getResource
import com.google.common.io.Resources.toByteArray
import net.robi42.tempmunger.domain.model.TemporalEntry
import net.robi42.tempmunger.importexport.dao.CsvImporter
import net.robi42.tempmunger.transform.SparkEsContextProvider
import net.robi42.tempmunger.transform.dao.SparkTransformer
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import javax.inject.Inject

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@AutoConfigureMockMvc(print = NONE)
@AutoConfigureRestDocs("build/generated-snippets", uriHost = "api.temp-munger.robi42.net", uriPort = 80)
@SpringBootTest(classes = arrayOf(TestApplication::class), webEnvironment = RANDOM_PORT)
abstract class ApplicationTests {

    @Inject private lateinit var csvImporter: CsvImporter
    @Inject private lateinit var transformer: SparkTransformer

    @Inject private lateinit var elasticsearchTemplate: ElasticsearchOperations
    @Inject private lateinit var sparkContextProvider: SparkEsContextProvider

    @Inject private lateinit var objectMapper: ObjectMapper
    @Inject protected lateinit var mockMvc: MockMvc

    @Before fun setUp() {
        csvImporter.import(toByteArray(getResource("climate.csv")), separator = '\t')
        Thread.sleep(1500) // Wait for ES refresh.
        transformer.updateRdd()
    }

    @After fun tearDown() {
        elasticsearchTemplate.deleteIndex(TemporalEntry.INDEX_NAME)
        sparkContextProvider.context.cancelAllJobs()
    }

    protected fun json(payload: Any): String = objectMapper.writeValueAsString(payload)

}
