package de.evia.travelmate.iam.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.iam.TenantDeleted;
import de.evia.travelmate.iam.application.command.CreateTenantCommand;
import de.evia.travelmate.iam.application.representation.TenantRepresentation;
import de.evia.travelmate.iam.domain.IamTestFixtures;
import de.evia.travelmate.iam.domain.account.AccountRepository;
import de.evia.travelmate.iam.domain.account.IdentityProviderService;
import de.evia.travelmate.iam.domain.dependent.DependentRepository;
import de.evia.travelmate.iam.domain.tenant.Tenant;
import de.evia.travelmate.iam.domain.tenant.TenantName;
import de.evia.travelmate.iam.domain.tenant.TenantRepository;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private DependentRepository dependentRepository;

    @Mock
    private IdentityProviderService identityProviderService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void createsTenant() {
        final CreateTenantCommand command = new CreateTenantCommand("Reisegruppe Alpen", "Eine Reisegruppe");
        when(tenantRepository.existsByName(any(TenantName.class))).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final TenantRepresentation result = tenantService.createTenant(command);

        assertThat(result.name()).isEqualTo("Reisegruppe Alpen");
        assertThat(result.description()).isEqualTo("Eine Reisegruppe");
        assertThat(result.tenantId()).isNotNull();
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    void rejectsDuplicateTenantName() {
        final CreateTenantCommand command = new CreateTenantCommand("Reisegruppe Alpen", null);
        when(tenantRepository.existsByName(any(TenantName.class))).thenReturn(true);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> tenantService.createTenant(command))
            .withMessageContaining("already exists");
    }

    @Test
    void findsTenantById() {
        final Tenant tenant = IamTestFixtures.tenant();
        when(tenantRepository.findById(IamTestFixtures.TENANT_ID)).thenReturn(Optional.of(tenant));

        final TenantRepresentation result = tenantService.findById(IamTestFixtures.TENANT_ID);

        assertThat(result.tenantId()).isEqualTo(IamTestFixtures.TENANT_ID.value());
        assertThat(result.name()).isEqualTo("Reisegruppe Alpen");
    }

    @Test
    void throwsWhenTenantNotFound() {
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThatIllegalArgumentException()
            .isThrownBy(() -> tenantService.findById(tenantId))
            .withMessageContaining("Tenant not found");
    }

    @Test
    void findsAllTenants() {
        when(tenantRepository.findAll()).thenReturn(List.of(IamTestFixtures.tenant()));

        final List<TenantRepresentation> result = tenantService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Reisegruppe Alpen");
    }

    @Test
    void deleteTenantPublishesEvent() {
        final Tenant tenant = IamTestFixtures.tenant();
        when(tenantRepository.findById(IamTestFixtures.TENANT_ID)).thenReturn(Optional.of(tenant));
        when(accountRepository.findAllByTenantId(IamTestFixtures.TENANT_ID)).thenReturn(List.of());

        tenantService.deleteTenant(IamTestFixtures.TENANT_ID);

        verify(eventPublisher).publishEvent(any(TenantDeleted.class));
        verify(tenantRepository).deleteById(IamTestFixtures.TENANT_ID);
    }
}
