package net.robi42.tempmunger.domain.model

import java.util.*

data class TemporalEntry(val id: UUID, val source: Map<String, Any>) {

    companion object {
        const val INDEX_NAME = "temporal_entries"
        const val TYPE_NAME = "temporal_entry"
    }

}
