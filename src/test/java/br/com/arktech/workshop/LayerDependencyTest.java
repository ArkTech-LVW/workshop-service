package br.com.arktech.workshop;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.conditions.ArchConditions.implement;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.application.VoidUseCase;
import br.com.arktech.workshop.shared.domain.BaseEntity;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;

import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(packages = "br.com.arktech.workshop", importOptions = DoNotIncludeTests.class)
class LayerDependencyTest {

    private static final String API = "br.com.arktech.workshop..api..";
    private static final String APPLICATION = "br.com.arktech.workshop..application..";
    private static final String DOMAIN = "br.com.arktech.workshop..domain..";
    private static final String INFRASTRUCTURE = "br.com.arktech.workshop..infrastructure..";
    private static final String SHARED = "br.com.arktech.workshop.shared..";

    @ArchTest
    static final ArchRule domainDependsOnNothingElse = noClasses()
            .that()
            .resideInAPackage(DOMAIN)
            .should()
            .dependOnClassesThat(
                    resideInAnyPackage(API, APPLICATION, INFRASTRUCTURE, "org.springframework..", "org.hibernate..")
                            .and(not(resideInAPackage("org.hibernate.annotations.."))))
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
    static final ArchRule modulesDependOnlyOnThemselvesAndShared = slices()
            .matching("br.com.arktech.workshop.(*)..")
            .should()
            .notDependOnEachOther()
            .ignoreDependency(alwaysTrue(), resideInAPackage(SHARED))
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule springDataRepositoriesResideInInfrastructure = classes()
            .that()
            .areAssignableTo(Repository.class)
            .should()
            .resideInAPackage(INFRASTRUCTURE)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule jpaEntitiesResideInDomain = classes()
            .that()
            .areAnnotatedWith(Entity.class)
            .or()
            .areAnnotatedWith(MappedSuperclass.class)
            .should()
            .resideInAPackage(DOMAIN)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule jpaEntitiesExtendBaseEntity = classes()
            .that()
            .areAnnotatedWith(Entity.class)
            .should()
            .beAssignableTo(BaseEntity.class)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule useCasesImplementUseCase = classes()
            .that()
            .haveSimpleNameEndingWith("UseCase")
            .and()
            .areNotInterfaces()
            .should(implement(UseCase.class).or(implement(VoidUseCase.class)))
            .andShould()
            .resideInAPackage(APPLICATION)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllersImplementTheirInterface = classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .and()
            .resideOutsideOfPackage(SHARED)
            .should(implementTheirInterface())
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllersKeepMappingsInTheInterface = noMethods()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(RestController.class)
            .and()
            .areDeclaredInClassesThat()
            .resideOutsideOfPackage(SHARED)
            .should()
            .beMetaAnnotatedWith(RequestMapping.class)
            .orShould()
            .beAnnotatedWith(RequestMapping.class)
            .allowEmptyShould(true);

    private static ArchCondition<JavaClass> implementTheirInterface() {
        return new ArchCondition<>("implement an interface named I<ControllerName>") {
            @Override
            public void check(JavaClass controller, ConditionEvents events) {
                String expected = "I" + controller.getSimpleName();
                boolean implemented = controller.getRawInterfaces().stream()
                        .anyMatch(implementedInterface -> implementedInterface.getSimpleName().equals(expected));
                events.add(new SimpleConditionEvent(
                        controller, implemented, controller.getName() + " does not implement " + expected));
            }
        };
    }
}
