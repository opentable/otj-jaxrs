package com.opentable.jaxrs.referrer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.opentable.jaxrs.JaxRsFeatureBinding;
import com.opentable.jaxrs.StandardFeatureGroup;

/**
 * Spring Configuration that enables automatic inclusion of referrer-related headers on JAX-RS requests; note that
 * this does not currently include the {@code Referer} header.
 *
 * <p>
 * If not using Spring, you would construct and set up the {@link ClientReferrerFeature} and
 * {@link ClientReferrerFilter} yourself.
 */
@Configuration
@Import(ClientReferrerFeature.class)
public class ClientReferrerConfiguration {
    static final String SERVICE_HEADER_NAME = "OT-Referring-Service";
    static final String HOST_HEADER_NAME = "OT-Referring-Host";

    @Bean
    public JaxRsFeatureBinding getClientReferrerFeatureBinding(final ClientReferrerFeature feature) {
        return JaxRsFeatureBinding.bind(StandardFeatureGroup.PLATFORM_INTERNAL, feature);
    }
}
