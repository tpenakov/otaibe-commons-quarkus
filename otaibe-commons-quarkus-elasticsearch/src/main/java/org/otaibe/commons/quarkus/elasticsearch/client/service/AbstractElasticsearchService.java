package org.otaibe.commons.quarkus.elasticsearch.client.service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.sniff.Sniffer;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Should be overriden in the project where is used in order to properly init and shutdown the client
 */
@Getter
@Setter
@Slf4j
public abstract class AbstractElasticsearchService {
    @ConfigProperty(name = "service.elastic-search.hosts")
    String[] hosts;
    @ConfigProperty(name = "service.elastic-search.num-threads", defaultValue = "10")
    Optional<Integer> numThreads;

    private RestHighLevelClient restClient;
    private Sniffer sniffer;

    @PostConstruct
    public void init() {
        log.info("init started");
        List<HttpHost> httpHosts = Arrays.stream(hosts)
                .map(s -> StringUtils.split(s, ':'))
                .map(strings -> new HttpHost(strings[0], Integer.valueOf(strings[1])))
                .collect(Collectors.toList());
        RestClientBuilder builder = RestClient.builder(httpHosts.toArray(new HttpHost[httpHosts.size()]));
        getNumThreads().ifPresent(integer ->
                builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultIOReactorConfig(
                        IOReactorConfig
                                .custom()
                                .setIoThreadCount(integer)
                                .build())
                ));

        restClient = new RestHighLevelClient(builder);
        sniffer = Sniffer.builder(getRestClient().getLowLevelClient()).build();
        log.info("init completed");
    }

    public void shutdown() {
        log.info("shutdown started");
        getSniffer().close();
        try {
            getRestClient().close();
        } catch (IOException e) {
            log.error("unable to close the rest client", e);
        }
        log.info("shutdown completed");
    }

}
