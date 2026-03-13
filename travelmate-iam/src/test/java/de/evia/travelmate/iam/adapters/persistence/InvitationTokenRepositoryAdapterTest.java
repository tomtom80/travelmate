package de.evia.travelmate.iam.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.iam.domain.IamTestFixtures;
import de.evia.travelmate.iam.domain.account.Account;
import de.evia.travelmate.iam.domain.account.AccountId;
import de.evia.travelmate.iam.domain.account.AccountRepository;
import de.evia.travelmate.iam.domain.registration.InvitationToken;
import de.evia.travelmate.iam.domain.registration.InvitationTokenRepository;
import de.evia.travelmate.iam.domain.tenant.Tenant;
import de.evia.travelmate.iam.domain.tenant.TenantName;
import de.evia.travelmate.iam.domain.tenant.TenantRepository;

@SpringBootTest
@ActiveProfiles("test")
class InvitationTokenRepositoryAdapterTest {

    @Autowired
    private InvitationTokenRepository tokenRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private InvitationTokenJpaRepository jpaRepository;

    private AccountId savedAccountId;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
        final TenantId tenantId = IamTestFixtures.TENANT_ID;
        if (tenantRepository.findById(tenantId).isEmpty()) {
            tenantRepository.save(new Tenant(tenantId, new TenantName("Test"), null));
        }
        final Account account = IamTestFixtures.registeredAccount();
        final Account saved = accountRepository.save(account);
        savedAccountId = saved.accountId();
    }

    @Test
    void savesAndFindsToken() {
        final InvitationToken token = InvitationToken.generate(savedAccountId);

        tokenRepository.save(token);

        final Optional<InvitationToken> found = tokenRepository.findByTokenValue(token.tokenValue());
        assertThat(found).isPresent();
        assertThat(found.get().accountId()).isEqualTo(savedAccountId);
        assertThat(found.get().isUsed()).isFalse();
        assertThat(found.get().expiresAt()).isEqualTo(token.expiresAt());
    }

    @Test
    void findsNothingForUnknownToken() {
        final Optional<InvitationToken> found = tokenRepository.findByTokenValue("nonexistent");

        assertThat(found).isEmpty();
    }

    @Test
    void savesUsedState() {
        final InvitationToken token = InvitationToken.generate(savedAccountId);
        tokenRepository.save(token);

        token.use();
        tokenRepository.save(token);

        final Optional<InvitationToken> found = tokenRepository.findByTokenValue(token.tokenValue());
        assertThat(found).isPresent();
        assertThat(found.get().isUsed()).isTrue();
    }
}
