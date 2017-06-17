package net.robi42.tempmunger.search

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.elasticsearch.core.EntityMapper
import org.springframework.stereotype.Component

@Component class CustomEntityMapper(private val objectMapper: ObjectMapper) : EntityMapper {

    override fun mapToString(obj: Any): String
            = objectMapper.writeValueAsString(obj)

    override fun <T> mapToObject(source: String, clazz: Class<T>): T
            = objectMapper.readValue(source, clazz)

}
