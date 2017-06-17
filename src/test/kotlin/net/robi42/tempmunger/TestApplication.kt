package net.robi42.tempmunger

import net.robi42.tempmunger.api.service.SparkEsServiceProvider
import net.robi42.tempmunger.api.service.TemporalEntryService
import net.robi42.tempmunger.importexport.dao.CsvImporter
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean

class TestApplication : Application() {

    @Bean override fun init(importer: CsvImporter,
                            serviceProvider: SparkEsServiceProvider,
                            entryService: TemporalEntryService) = ApplicationRunner {
        // Noop.
    }

}
