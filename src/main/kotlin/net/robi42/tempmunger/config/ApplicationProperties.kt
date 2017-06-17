package net.robi42.tempmunger.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties
@Component class ApplicationProperties(val spring: Spring) {
    @Component class Spring(val application: App) {
        @Component class App(var name: String? = null)
    }
}
