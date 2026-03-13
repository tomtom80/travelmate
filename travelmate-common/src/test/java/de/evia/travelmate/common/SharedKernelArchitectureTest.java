package de.evia.travelmate.common;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "de.evia.travelmate.common", importOptions = ImportOption.DoNotIncludeTests.class)
class SharedKernelArchitectureTest {

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
}
