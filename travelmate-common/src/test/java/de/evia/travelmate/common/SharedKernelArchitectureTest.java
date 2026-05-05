package de.evia.travelmate.common;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import de.evia.travelmate.common.domain.DomainEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "de.evia.travelmate.common", importOptions = ImportOption.DoNotIncludeTests.class)
class SharedKernelArchitectureTest {

    // --- Rule 1: No Framework Dependencies ---

    @ArchTest
    static final ArchRule shared_kernel_must_not_depend_on_spring =
        noClasses()
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..")
            .because("travelmate-common is a plain JAR and must not depend on Spring");

    @ArchTest
    static final ArchRule shared_kernel_must_not_depend_on_jakarta_persistence =
        noClasses()
            .should().dependOnClassesThat()
            .resideInAnyPackage("jakarta.persistence..")
            .because("travelmate-common is a plain JAR and must not depend on JPA");

    @ArchTest
    static final ArchRule shared_kernel_must_not_depend_on_jakarta_transaction =
        noClasses()
            .should().dependOnClassesThat()
            .resideInAnyPackage("jakarta.transaction..")
            .because("travelmate-common is a plain JAR and must not depend on JTA");

    // --- Rule 2: ADR-0025 — Domain Event Naming Convention ---

    // AccommodationPriceSet is grandfathered until it is renamed to AccommodationPriceUpdated
    @ArchTest
    static final ArchRule domain_event_names_must_not_use_deprecated_set_suffix =
        classes()
            .that().implement(DomainEvent.class)
            .should(new ArchCondition<JavaClass>("not use the deprecated *Set name suffix") {
                @Override
                public void check(final JavaClass item, final ConditionEvents events) {
                    if (item.getSimpleName().endsWith("Set")
                        && !"AccommodationPriceSet".equals(item.getSimpleName())) {
                        events.add(SimpleConditionEvent.violated(item,
                            item.getSimpleName() + " uses deprecated *Set suffix; rename to past-tense per ADR-0025 §3"));
                    }
                }
            })
            .because("domain event names must use past-tense suffixes per ADR-0025 §3; *Set is deprecated");

    // --- Rule 3: ADR-0025 — Domain Event Required Fields ---

    @ArchTest
    static final ArchRule domain_events_must_declare_tenant_id =
        classes()
            .that().implement(DomainEvent.class)
            .should(haveDeclaredField("tenantId"))
            .because("all domain events must include tenantId per ADR-0025 §4");

    @ArchTest
    static final ArchRule domain_events_must_declare_occurred_on =
        classes()
            .that().implement(DomainEvent.class)
            .should(haveDeclaredField("occurredOn"))
            .because("all domain events must include occurredOn per ADR-0025 §4");

    private static ArchCondition<JavaClass> haveDeclaredField(final String fieldName) {
        return new ArchCondition<>("have a declared field named '" + fieldName + "'") {
            @Override
            public void check(final JavaClass item, final ConditionEvents events) {
                final boolean hasField = item.getFields().stream()
                    .anyMatch(f -> f.getName().equals(fieldName));
                if (!hasField) {
                    events.add(SimpleConditionEvent.violated(item,
                        item.getSimpleName() + " does not declare a field named '" + fieldName + "'"));
                }
            }
        };
    }
}
