package com.socialmedia.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.socialmedia.client.twitter.response.TwitterDateDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.time.Duration;
import java.time.Instant;

@Configuration
public class WebClientConfig {

    @Value("${twitter.api.base-url}")
    private String twitterApiBaseUrl;

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // Register custom deserializer for Instant fields (for Twitter date format)
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Instant.class, new TwitterDateDeserializer());
        objectMapper.registerModule(module);
        
        return objectMapper;
    }

    @Bean
    public WebClient webClient(ObjectMapper objectMapper) {
        Jackson2JsonEncoder encoder = new Jackson2JsonEncoder(objectMapper);
        Jackson2JsonDecoder decoder = new Jackson2JsonDecoder(objectMapper);

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().jackson2JsonEncoder(encoder);
                    configurer.defaultCodecs().jackson2JsonDecoder(decoder);
                })
                .build();

        // Configure HTTP client with timeouts
        // Increased timeouts for Twitter API which can be slow
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(120)) // 2 minutes for response
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000) // 30 seconds to connect
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(120)) // 2 minutes read timeout
                );

        return WebClient.builder()
                .baseUrl(twitterApiBaseUrl)
                .exchangeStrategies(strategies)
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient))
                .build();
    }
}



