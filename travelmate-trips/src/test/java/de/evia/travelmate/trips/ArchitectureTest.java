package de.evia.travelmate.trips;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "de.evia.travelmate.trips", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    // --- Rule 1: Domain Independence ---

    @ArchTest
    static final ArchRule domain_must_not_depend_on_spring =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..")
            .because("domain layer must remain free of Spring framework dependencies");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_jpa =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("jakarta.persistence..")
            .because("domain layer must remain free of JPA dependencies");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_jakarta_transaction =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("jakarta.transaction..")
            .because("domain layer must remain free of JTA dependencies");

    // --- Rule 2: Layered Access ---

    @ArchTest
    static final ArchRule domain_must_not_access_application =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..application..")
            .because("domain must not depend on the application layer");

    @ArchTest
    static final ArchRule domain_must_not_access_adapters =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapters..")
            .because("domain must not depend on the adapters layer");

    @ArchTest
    static final ArchRule application_must_not_access_adapters =
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..adapters.web..",
                "..adapters.persistence..",
                "..adapters.messaging..",
                "..adapters.mail.."
            )
            .because("application layer must not depend on adapter implementations");

    // --- Rule 3: Adapter Isolation ---

    @ArchTest
    static final ArchRule domain_must_not_access_persistence_adapters =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapters.persistence..")
            .because("domain must not know about persistence adapter implementations");

    @ArchTest
    static final ArchRule domain_must_not_access_web_adapters =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapters.web..")
            .because("domain must not know about web adapter implementations");

    @ArchTest
    static final ArchRule domain_must_not_access_messaging_adapters =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapters.messaging..")
            .because("domain must not know about messaging adapter implementations");

    // --- Rule 4: No Circular Dependencies ---

    @ArchTest
    static final ArchRule no_circular_package_dependencies =
        SlicesRuleDefinition.slices()
            .matching("de.evia.travelmate.trips.(*)..")
            .should().beFreeOfCycles()
            .because("there must be no circular dependencies between top-level packages");
}
