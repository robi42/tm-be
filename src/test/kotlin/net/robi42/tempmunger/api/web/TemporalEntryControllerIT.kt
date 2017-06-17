package net.robi42.tempmunger.api.web

import com.google.common.io.Resources.getResource
import com.google.common.io.Resources.toByteArray
import net.robi42.tempmunger.api.web.TemporalEntryController.Companion.BASE_PATH
import net.robi42.tempmunger.ApplicationTests
import org.junit.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

private const val FIELD_AVG_TEMP = "AvgTemp"
private const val FILE_PARAM = "file"

class TemporalEntryControllerIT : ApplicationTests() {

    @Test fun `application context loads`() {}

    @Test fun searches() {
        val payload = mapOf("aggs" to mapOf("agg" to mapOf("terms" to mapOf("field" to FIELD_AVG_TEMP))))
        val request = post("$BASE_PATH/search?sort={field}", FIELD_AVG_TEMP)
                .contentType(APPLICATION_JSON)
                .content(json(payload))

        mockMvc.perform(request)
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.hits").isMap)
                .andDo(document("search"))
    }

    @Test fun `gets schema`() {
        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isMap)
                .andDo(document("schema"))
    }

    @Test fun `uploads CSV`() {
        val csv = toByteArray(getResource("test.csv"))
        val request = fileUpload(BASE_PATH)
                .file(FILE_PARAM, csv)
                .param("separator", ",")

        mockMvc.perform(request)
                .andExpect(status().isCreated)
                .andDo(document("upload"))
    }

    @Test fun `validates CSV on upload`() {
        val csv = "header\n\n".toByteArray()
        val request = fileUpload(BASE_PATH)
                .file(FILE_PARAM, csv)

        mockMvc.perform(request)
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").isNotEmpty)
    }

}
