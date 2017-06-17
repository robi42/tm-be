package net.robi42.tempmunger.predict.service

import net.robi42.tempmunger.config.MESSAGE_DESTINATION_PREFIX
import net.robi42.tempmunger.util.logger
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import reactor.bus.Event
import reactor.bus.EventBus
import reactor.bus.selector.Selectors.`$`
import reactor.fn.Consumer
import javax.annotation.PostConstruct

private const val MESSAGE_DESTINATION = "$MESSAGE_DESTINATION_PREFIX/$EVENT_OUTLIERS"

@Component class ReactiveOutlierDetectionEventPublisher(private val eventBus: EventBus,
                                                        private val messagingTemplate: SimpMessagingTemplate) {

    private val log by logger()

    @PostConstruct private fun init() {
        eventBus.on(`$`(EVENT_OUTLIERS), Consumer<Event<Set<String>>> {
            val eventData = it.data

            log.info("Publishing '$EVENT_OUTLIERS' event with: {}", eventData)

            messagingTemplate.convertAndSend(MESSAGE_DESTINATION, eventData)
        })
    }

}
