package de.evia.travelmate.expense;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "de.evia.travelmate.expense", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    // --- Rule 1: Domain Independence ---
    // Note: allowEmptyShould(true) is used because the expense module is still being
    // built out and some packages may not yet contain classes.

    @ArchTest
    static final ArchRule domain_must_not_depend_on_spring =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..")
            .allowEmptyShould(true)
            .because("domain layer must remain free of Spring framework dependencies");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_jpa =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("jakarta.persistence..")
            .allowEmptyShould(true)
            .because("domain layer must remain free of JPA dependencies");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_jakarta_transaction =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("jakarta.transaction..")
            .allowEmptyShould(true)
            .because("domain layer must remain free of JTA dependencies");

    // --- Rule 2: Layered Access ---

    @ArchTest
    static final ArchRule domain_must_not_access_application =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..application..")
            .allowEmptyShould(true)
            .because("domain must not depend on the application layer");

    @ArchTest
    static final ArchRule domain_must_not_access_adapters =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapters..")
            .allowEmptyShould(true)
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
            .allowEmptyShould(true)
            .because("application layer must not depend on adapter implementations");

    // --- Rule 3: Adapter Isolation ---

    @ArchTest
    static final ArchRule domain_must_not_access_persistence_adapters =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapters.persistence..")
            .allowEmptyShould(true)
            .because("domain must not know about persistence adapter implementations");

    @ArchTest
    static final ArchRule domain_must_not_access_web_adapters =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapters.web..")
            .allowEmptyShould(true)
            .because("domain must not know about web adapter implementations");

    @ArchTest
    static final ArchRule domain_must_not_access_messaging_adapters =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapters.messaging..")
            .allowEmptyShould(true)
            .because("domain must not know about messaging adapter implementations");
}
