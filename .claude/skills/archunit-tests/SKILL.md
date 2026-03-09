---
name: ArchUnit Tests
description: "Create architecture fitness functions using ArchUnit to enforce hexagonal architecture, DDD rules, and coding conventions"
user-invocable: false
---

# ArchUnit Tests Skill

Create architecture fitness functions using https://www.archunit.org/ for the Travelmate project.

## Maven Dependency (add to each SCS pom.xml)
```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.4.1</version>
    <scope>test</scope>
</dependency>
```

## Test Location
`src/test/java/de/evia/travelmate/<context>/architecture/ArchitectureTest.java`

## Architecture Rules to Enforce

### 1. Hexagonal Architecture (Layer Dependencies)
```java
@ArchTest
static final ArchRule domain_should_not_depend_on_adapters =
    noClasses().that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAPackage("..adapters..");

@ArchTest
static final ArchRule domain_should_not_depend_on_spring =
    noClasses().that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "org.springframework..",
            "jakarta.persistence..",
            "jakarta.transaction.."
        );

@ArchTest
static final ArchRule application_should_not_depend_on_adapters =
    noClasses().that().resideInAPackage("..application..")
        .should().dependOnClassesThat().resideInAPackage("..adapters..");
```

### 2. DDD Aggregate Rules
```java
@ArchTest
static final ArchRule aggregate_roots_should_extend_base =
    classes().that().haveSimpleNameEndingWith("AggregateRoot")
        .or().areAnnotatedWith(/* custom marker */)
        .should().beAssignableTo(AggregateRoot.class);

@ArchTest
static final ArchRule value_objects_should_be_records =
    classes().that().resideInAPackage("..domain..")
        .and().haveSimpleNameNotEndingWith("Repository")
        .and().haveSimpleNameNotEndingWith("Service")
        .and().doNotExtend(AggregateRoot.class)
        .should().beRecords();
```

### 3. Repository Rules
```java
@ArchTest
static final ArchRule repository_interfaces_in_domain =
    classes().that().haveSimpleNameEndingWith("Repository")
        .and().areInterfaces()
        .should().resideInAPackage("..domain..");

@ArchTest
static final ArchRule repository_impls_in_adapters =
    classes().that().haveSimpleNameEndingWith("RepositoryImpl")
        .or().haveSimpleNameContaining("JpaRepository")
        .should().resideInAPackage("..adapters.persistence..");
```

### 4. No Wildcard Imports
```java
@ArchTest
static final ArchRule no_wildcard_imports =
    noClasses().should().accessClassesThat()
        .haveFullyQualifiedName("*.*"); // Use source-level check instead
```

### 5. Cycle Detection
```java
@ArchTest
static final ArchRule no_package_cycles =
    slices().matching("de.evia.travelmate.(*)..")
        .should().beFreeOfCycles();
```

### 6. Event Rules
```java
@ArchTest
static final ArchRule events_should_be_records =
    classes().that().resideInAPackage("..events..")
        .should().beRecords();

@ArchTest
static final ArchRule events_should_implement_domain_event =
    classes().that().resideInAPackage("..events..")
        .should().implement(DomainEvent.class);
```

### 7. Controller Rules
```java
@ArchTest
static final ArchRule controllers_in_web_adapter =
    classes().that().areAnnotatedWith(Controller.class)
        .or().areAnnotatedWith(RestController.class)
        .should().resideInAPackage("..adapters.web..");
```

### 8. Onion Architecture (Library API — PREFERRED)
```java
// Use ArchUnit's built-in onion architecture check — directly fits Travelmate's hexagonal style
@ArchTest
static final ArchRule onion_architecture =
    Architectures.onionArchitecture()
        .domainModels("..domain..")
        .domainServices("..domain..")
        .applicationServices("..application..")
        .adapter("web", "..adapters.web..")
        .adapter("persistence", "..adapters.persistence..")
        .adapter("messaging", "..adapters.messaging..");
```

### 9. Layered Architecture (Library API — alternative)
```java
@ArchTest
static final ArchRule layered_architecture =
    Architectures.layeredArchitecture().consideringAllDependencies()
        .layer("Domain").definedBy("..domain..")
        .layer("Application").definedBy("..application..")
        .layer("Adapters").definedBy("..adapters..")
        .whereLayer("Domain").mayNotAccessAnyLayer()
        .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapters")
        .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapters");
```

### 10. General Coding Rules (Predefined)
```java
// Import ArchUnit's predefined rules for common checks
@ArchTest
static final ArchRule no_field_injection =
    GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

@ArchTest
static final ArchRule no_system_out =
    GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
```

### 11. FreezingArchRule (Incremental Adoption)
```java
// Use FreezingArchRule to adopt rules incrementally — existing violations are frozen,
// only NEW violations will fail the build. Stored in archunit_store/ directory.
@ArchTest
static final ArchRule domain_purity_frozen =
    FreezingArchRule.freeze(
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..")
    );
```

### 12. PlantUML-as-Contract (Architecture Diagram Validation)
```java
// Validate code against PlantUML component diagrams — closes the loop between
// documentation and implementation. The agent maintains the diagram, ArchUnit enforces it.
@ArchTest
static final ArchRule matches_plantuml_diagram =
    classes().should(adhereToPlantUmlDiagram(
        getClass().getResource("/architecture.puml"),
        consideringOnlyDependenciesInDiagram()
    ));
```

## Advanced Patterns

### Architecture Drift Detection Loop
1. Run ArchUnit tests → collect violations
2. Cross-reference violations against arc42 docs and ADRs
3. If architecture docs are authoritative → propose code fixes
4. If code change was intentional → propose documentation update + new ADR

### SCS Independence Check
```java
// Ensure no direct compile-time dependencies between SCS modules
@ArchTest
static final ArchRule scs_independence =
    slices().matching("de.evia.travelmate.(*)..").should().beFreeOfCycles();
```

## Code Style Conventions
- `final` on all test locals and ArchRule fields
- `static final` for ArchRule declarations
- Use `@AnalyzeClasses(packages = "de.evia.travelmate.<context>")` at class level
- Group rules by concern (layers, ddd, naming, cycles)
- Prefer Library API (`onionArchitecture()`) over manual layer rules
- Use `FreezingArchRule` for incremental adoption in existing codebases
