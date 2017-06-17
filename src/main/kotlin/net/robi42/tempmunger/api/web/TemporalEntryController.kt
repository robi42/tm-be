package net.robi42.tempmunger.api.web

import net.robi42.tempmunger.api.service.TemporalEntryService
import net.robi42.tempmunger.domain.dto.TransformDto
import net.robi42.tempmunger.domain.dto.TransformTimestampDto
import net.robi42.tempmunger.domain.model.TemporalEntry
import net.robi42.tempmunger.domain.model.TemporalFormat
import net.robi42.tempmunger.domain.model.TemporalScale
import net.robi42.tempmunger.util.MediaTypes.TEXT_CSV
import net.robi42.tempmunger.util.logger
import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.util.*

@CrossOrigin
@RestController
@RequestMapping(TemporalEntryController.BASE_PATH)
class TemporalEntryController(private val entryService: TemporalEntryService) {

    private val log by logger()

    companion object {
        const val BASE_PATH = "/temporal-entries"
    }

    @PostMapping
    @ResponseStatus(CREATED)
    fun upload(@RequestParam(defaultValue = "\t") separator: Char,
               file: MultipartFile) {
        val bytes = file.bytes
        log.info("Uploading file of size {}", bytes.size)
        entryService.create(bytes, separator)
    }

    @PostMapping("/export")
    fun export(@RequestParam(defaultValue = "ISO_DATE_TIME") temporalFormat: TemporalFormat): ResponseEntity<String> =
            ResponseEntity.ok().contentType(TEXT_CSV)
                    .header(CONTENT_DISPOSITION, "attachment; filename=export.csv")
                    .body(entryService.export(temporalFormat))

    @GetMapping fun schema() = entryService.schema()

    @PostMapping("/search")
    fun search(@RequestParam(defaultValue = "0") from: Int,
               @RequestParam(defaultValue = "20") size: Int,
               @RequestParam("id", defaultValue = "", required = false) ids: Set<UUID>,
               @RequestParam(defaultValue = "", required = false) sort: String?,
               @RequestBody query: Map<String, Any>)
            = entryService.search(from, size, ids, sort, query)

    @PutMapping("/{id}")
    fun update(@PathVariable id: UUID,
               @RequestBody source: Map<String, Any>)
            = entryService.update(TemporalEntry(id, source)).source

    @PutMapping("/missing")
    @ResponseStatus(NO_CONTENT)
    fun transform(@RequestParam field: String,
                  @RequestBody dto: TransformTimestampDto) {
        entryService.transformMissing(temporalField = field, to = dto.to)
    }

    @PutMapping
    @ResponseStatus(NO_CONTENT)
    fun transform(@RequestParam field: String,
                  @RequestParam value: Instant,
                  @RequestParam scale: TemporalScale,
                  @RequestBody dto: TransformDto) {
        if (dto.toTimestamp != null) {
            entryService.transform(field, value, scale, dto.toTimestamp)
        }
        if (dto.toYearMonth != null) {
            entryService.transform(field, value, scale, dto.toYearMonth)
        }
        if (dto.toYear != null) {
            entryService.transform(field, value, scale, dto.toYear)
        }
    }

    @PostMapping("/merge")
    @ResponseStatus(NO_CONTENT)
    fun merge(@RequestParam sourceField: String,
              @RequestParam targetField: String) {
        entryService.merge(sourceField, targetField)
    }

    @DeleteMapping
    @ResponseStatus(NO_CONTENT)
    fun delete(@RequestParam("id") ids: Set<UUID>) {
        entryService.delete(ids)
    }

    @DeleteMapping("/query")
    @ResponseStatus(NO_CONTENT)
    fun delete(@RequestParam field: String,
               @RequestParam value: String,
               @RequestParam(defaultValue = "") scale: TemporalScale?,
               @RequestParam filter: String,
               @RequestParam("id", defaultValue = "", required = false) ids: Set<UUID>) {
        entryService.delete(field, value, scale, filter, ids)
    }

    @DeleteMapping("/missing")
    @ResponseStatus(NO_CONTENT)
    fun delete(@RequestParam field: String) {
        entryService.deleteMissing(field)
    }

}
