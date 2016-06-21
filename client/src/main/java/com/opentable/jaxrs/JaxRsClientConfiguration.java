package com.opentable.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertyResolver;

import com.opentable.spring.SpecializedConfigFactory;

@Configuration
public class JaxRsClientConfiguration {

    @Bean
    SpecializedConfigFactory<JaxRsClientConfig> jaxRsConfigFactory(PropertyResolver pr) {
        return SpecializedConfigFactory.create(pr, JaxRsClientConfig.class, "jaxrs.client.${name}");
    }

    @Bean
    JaxRsClientFactory jaxrsClientFactory(SpecializedConfigFactory<JaxRsClientConfig> config) {
        return new JaxRsClientFactory(config);
    }

    @Bean
    JaxRsFeatureBinding dataUriHandler() {
        return JaxRsFeatureBinding.bindToAllGroups(new DataUriFeature());
    }

    @Bean
    JaxRsFeatureBinding jsonFeature(ObjectMapper mapper) {
        return JaxRsFeatureBinding.bindToAllGroups(JsonClientFeature.forMapper(mapper));
    }
}
