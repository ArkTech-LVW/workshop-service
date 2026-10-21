package br.com.arktech.workshop;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.data.repository.Repository;

// Rules apply to every module (<module>/{api,application,domain,infrastructure}).
// allowEmptyShould keeps them passing until the first module exists.
@AnalyzeClasses(packages = "br.com.arktech.workshop", importOptions = DoNotIncludeTests.class)
class LayerDependencyTest {

    private static final String API = "br.com.arktech.workshop..api..";
    private static final String APPLICATION = "br.com.arktech.workshop..application..";
    private static final String DOMAIN = "br.com.arktech.workshop..domain..";
    private static final String INFRASTRUCTURE = "br.com.arktech.workshop..infrastructure..";

    @ArchTest
    static final ArchRule domainDependsOnNothingElse = noClasses()
            .that()
            .resideInAPackage(DOMAIN)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(API, APPLICATION, INFRASTRUCTURE, "org.springframework..", "org.hibernate..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule applicationDoesNotDependOnApiOrInfrastructure = noClasses()
            .that()
            .resideInAPackage(APPLICATION)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(API, INFRASTRUCTURE)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule apiDoesNotDependOnInfrastructure = noClasses()
            .that()
            .resideInAPackage(API)
            .should()
            .dependOnClassesThat()
            .resideInAPackage(INFRASTRUCTURE)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule springDataRepositoriesResideInInfrastructure = classes()
            .that()
            .areAssignableTo(Repository.class)
            .should()
            .resideInAPackage(INFRASTRUCTURE)
            .allowEmptyShould(true);
}
