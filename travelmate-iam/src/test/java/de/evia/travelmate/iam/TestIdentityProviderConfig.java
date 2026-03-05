package de.evia.travelmate.iam;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import de.evia.travelmate.iam.domain.account.IdentityProviderService;

import static org.mockito.Mockito.mock;

@Configuration
@Profile("test")
public class TestIdentityProviderConfig {

    @Bean
    public IdentityProviderService identityProviderService() {
        return mock(IdentityProviderService.class);
    }
}
