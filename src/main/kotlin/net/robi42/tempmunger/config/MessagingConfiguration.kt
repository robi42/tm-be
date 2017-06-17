package net.robi42.tempmunger.config

import net.robi42.tempmunger.predict.service.EVENT_OUTLIERS
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import reactor.spring.context.config.EnableReactor

const val MESSAGE_DESTINATION_PREFIX = "/topic"

@EnableReactor
@EnableWebSocketMessageBroker
@Configuration class MessagingConfiguration : AbstractWebSocketMessageBrokerConfigurer() {

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.apply {
            enableSimpleBroker(MESSAGE_DESTINATION_PREFIX)
            setApplicationDestinationPrefixes("/app")
        }
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/$EVENT_OUTLIERS").setAllowedOrigins("*").withSockJS()
    }

}
