package com.opentable.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;

import com.opentable.jaxrs.referrer.ClientReferrerConfiguration;
import com.opentable.spring.SpecializedConfigFactory;

@Configuration
@Import(ClientReferrerConfiguration.class)
public class JaxRsClientConfiguration {

    @Bean
    SpecializedConfigFactory<JaxRsClientConfig> jaxRsConfigFactory(ConfigurableEnvironment environment) {
        return SpecializedConfigFactory.create(environment, JaxRsClientConfig.class, "jaxrs.client.${name}");
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
