/*
 * DukeOps - iJUG Self-Service Portal
 * Copyright (C) Marcus Fihlon and the individual contributors to DukeOps.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.ijug.dukeops;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import eu.ijug.dukeops.config.AppConfig;
import eu.ijug.dukeops.test.BrowserTest;
import eu.ijug.dukeops.test.KaribuTest;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

class ArchitectureTest {

    private final @NotNull JavaClasses allClasses = new ClassFileImporter()
            .importPackages("eu.ijug.dukeops");
    private final @NotNull JavaClasses classesWithoutTests = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("eu.ijug.dukeops");
    private final @NotNull JavaClasses onlyTests = new ClassFileImporter()
            .withImportOption(new ImportOption.OnlyIncludeTests())
            .importPackages("eu.ijug.dukeops");

    @Test
    void jooqClassesShouldOnlyBeAccessedByServiceLayer() {
        noClasses()
                .that()
                .resideOutsideOfPackages(
                        "eu.ijug.dukeops.service..", // service layer
                        "eu.ijug.dukeops.db..") // jOOQ generated classes
                .should()
                .accessClassesThat()
                .resideInAnyPackage("eu.ijug.dukeops.db..")
                .because("only the service layer and jOOQ code should access jOOQ types directly")
                .check(classesWithoutTests);
    }

    @Test
    void servicesShouldNotReturnStreams() {
        methods()
                .that().areDeclaredInClassesThat().resideInAPackage("eu.ijug.dukeops.service..")
                .and().arePublic()
                .and().areNotDeclaredIn(Object.class)
                .should().notHaveRawReturnType(Stream.class)
                .because("returning Stream from service methods may lead to unclosed JDBC resources")
                .check(classesWithoutTests);
    }

    @Test
    void dtosShouldBeRecordsOrEnums() {
        ArchCondition<JavaClass> beRecordOrEnum = new ArchCondition<>("be a record or enum") {
            @Override
            public void check(final @NotNull JavaClass clazz, final @NotNull ConditionEvents events) {
                final var isRecord = clazz.isRecord();
                final var isEnum = clazz.isEnum();

                if (!isRecord && !isEnum) {
                    final var message = clazz.getSimpleName() + " is neither a record nor an enum";
                    events.add(SimpleConditionEvent.violated(clazz, message));
                }
            }
        };

        classes()
                .that()
                .resideInAPackage("eu.ijug.dukeops.entity..")
                .and().haveSimpleNameEndingWith("Dto")
                .should(beRecordOrEnum)
                .because("DTOs should be implemented as Java records or enums to ensure immutability and clarity")
                .check(classesWithoutTests);
    }

    @Test
    void forbiddenDateTimeTypesShouldNotBeUsed() {
        final var forbiddenTypes = Set.of(
                Calendar.class.getName(),
                Date.class.getName()
        );
        final var forbiddenTypeList = forbiddenTypes.stream()
                .map(fqn -> fqn.substring(fqn.lastIndexOf('.') + 1))
                .sorted()
                .collect(Collectors.joining(", "));

        final var notDependOnForbiddenDateTimeTypes = new ArchCondition<JavaClass>(
                "not depend on " + forbiddenTypeList) {
            @Override
            public void check(@NotNull JavaClass clazz, @NotNull ConditionEvents events) {
                clazz.getDirectDependenciesFromSelf().forEach(dependency -> {
                    var targetName = dependency.getTargetClass().getFullName();
                    if (forbiddenTypes.contains(targetName)) {
                        events.add(SimpleConditionEvent.violated(
                                dependency,
                                "Class " + clazz.getName() + " depends on forbidden type: " + targetName
                        ));
                    }
                });
            }
        };

        classes()
                .should(notDependOnForbiddenDateTimeTypes)
                .because("only new date and time classes are allowed")
                .check(classesWithoutTests);
    }

    @Test
    void junit4AssertionsShouldNotBeUsed() {
        ArchRule rule = noClasses()
                .should()
                .accessClassesThat()
                .resideInAnyPackage("org.junit")
                .andShould()
                .accessClassesThat()
                .haveSimpleName("Assert")
                .because("only AssertJ should be used for assertions");
        rule.check(onlyTests);
    }

    @Test
    void junit5AssertionsShouldNotBeUsed() {
        ArchRule rule = noClasses()
                .should()
                .accessClassesThat()
                .resideInAnyPackage("org.junit.jupiter.api")
                .andShould()
                .accessClassesThat()
                .haveSimpleName("Assertions")
                .because("only AssertJ should be used for assertions");
        rule.check(onlyTests);
    }

    @Test
    void hamcrestShouldNotBeUsed() {
        ArchRule rule = noClasses()
                .should()
                .accessClassesThat()
                .resideInAnyPackage("org.hamcrest..")
                .because("Hamcrest matchers should not be used");
        rule.check(onlyTests);
    }

    @Test
    void onlyIntegrationTestShouldUseSpringBootTest() {
        ArchRule rule = noClasses()
                .that().doNotHaveSimpleName("IntegrationTest")
                .should().beAnnotatedWith(SpringBootTest.class);

        rule.check(onlyTests);
    }

    @Test
    void karibuTestsShouldHaveSuffixKT() {
        ArchRule rule = classes()
                .that()
                    .areAssignableTo(KaribuTest.class)
                .and().doNotHaveModifier(ABSTRACT)
                .should().haveSimpleNameEndingWith("KT");

        rule.check(onlyTests);
    }

    @Test
    void browserTestsShouldHaveSuffixBT() {
        ArchRule rule = classes()
                .that()
                    .areAssignableTo(BrowserTest.class)
                .and().doNotHaveModifier(ABSTRACT)
                .should().haveSimpleNameEndingWith("BT");

        rule.check(onlyTests);
    }

    @Test
    void utilityClassesShouldHavePrivateConstructorsThatThrowExceptions() {
        for (final JavaClass javaClass : allClasses) {
            if (javaClass.getSimpleName().endsWith("Util")) {
                final Class<?> clazz = javaClass.reflect();
                for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                    assertThat(Modifier.isPrivate(constructor.getModifiers()))
                            .as("Constructor %s of %s should be private", constructor, clazz.getSimpleName())
                            .isTrue();
                    assertThatThrownBy(() -> {
                        constructor.setAccessible(true);
                        constructor.newInstance();
                    })
                            .isInstanceOf(InvocationTargetException.class)
                            .extracting(Throwable::getCause)
                            .isInstanceOf(IllegalStateException.class)
                            .extracting(Throwable::getMessage, as(STRING))
                            .isEqualTo("Utility class");
                }
            }
        }
    }

    @Test
    void localeGetLanguageShouldOnlyBeUsedInLocaleUtil() {
        final var forbiddenMethod = "getLanguage";
        final var allowedClass = "eu.ijug.dukeops.util.LocaleUtil";

        final var onlyLocaleUtilMayCallGetLanguage = new ArchCondition<JavaClass>(
                "only LocaleUtil may call Locale.getLanguage()") {
            @Override
            public void check(final @NotNull JavaClass clazz, final @NotNull ConditionEvents events) {
                if (clazz.getName().equals(allowedClass)) {
                    return;
                }

                clazz.getMethodCallsFromSelf().forEach(call -> {
                    final var target = call.getTarget();
                    if (target.getOwner().isEquivalentTo(Locale.class)
                            && target.getName().equals(forbiddenMethod)
                            && target.getRawParameterTypes().isEmpty()) {
                        events.add(SimpleConditionEvent.violated(
                                call,
                                "Forbidden call to Locale.getLanguage() in class " + clazz.getName() +
                                        ": use LocaleUtil.getLanguageCode(Locale) instead"));
                    }
                });
            }
        };

        classes()
                .should(onlyLocaleUtilMayCallGetLanguage)
                .because("Locale.getLanguage() should not be used directly – use LocaleUtil.getLanguageCode() instead")
                .check(allClasses);
    }

    @Test
    void appConfigBaseUrlShouldOnlyBeUsedInLinkUtil() {
        final var forbiddenMethod = "baseUrl";
        final var allowedClass = "eu.ijug.dukeops.web.init.LinkUtilInitializer";

        final var onlyLinkUtilMayCallBaseUrl = new ArchCondition<JavaClass>(
                "only LinkUtil may call AppConfig.baseUrl()") {
            @Override
            public void check(final @NotNull JavaClass clazz, final @NotNull ConditionEvents events) {
                if (clazz.getName().equals(allowedClass)) {
                    return;
                }

                clazz.getMethodCallsFromSelf().forEach(call -> {
                    final var target = call.getTarget();
                    if (target.getOwner().isEquivalentTo(AppConfig.class)
                            && target.getName().equals(forbiddenMethod)
                            && target.getRawParameterTypes().isEmpty()) {
                        events.add(SimpleConditionEvent.violated(
                                call,
                                "Forbidden call to AppConfig.baseUrl() in class " + clazz.getName() +
                                        ": use LinkUtil.getBaseUrl() instead"));
                    }
                });
            }
        };

        classes()
                .should(onlyLinkUtilMayCallBaseUrl)
                .because("base URL handling must be centralized in LinkUtil")
                .check(classesWithoutTests);
    }

    @Test
    void uiNavigateShouldOnlyBeUsedInNavigator() {
        final var forbiddenMethod = "navigate";
        final var allowedClass = "eu.ijug.dukeops.web.infra.Navigator";

        final var onlyNavigatorMayCallUiNavigate = new ArchCondition<JavaClass>(
                "only Navigator may call UI.navigate(..)") {
            @Override
            public void check(final @NotNull JavaClass clazz, final @NotNull ConditionEvents events) {
                if (clazz.getName().equals(allowedClass)) {
                    return;
                }

                clazz.getMethodCallsFromSelf().forEach(call -> {
                    final var target = call.getTarget();
                    if (target.getOwner().isEquivalentTo(UI.class)
                            && target.getName().equals(forbiddenMethod)) {
                        events.add(SimpleConditionEvent.violated(
                                call,
                                "Forbidden call to UI.navigate(..) in class " + clazz.getName() +
                                        ": use " + allowedClass + " instead"));
                    }
                });
            }
        };

        classes()
                .should(onlyNavigatorMayCallUiNavigate)
                .because("navigation must be centralized in Navigator to keep views testable")
                .check(classesWithoutTests);
    }

    @Test
    void uiGetCurrentShouldNotBeUsed() {
        final var forbiddenMethod = "getCurrent";

        final var noOneMayCallUiGetCurrent = new ArchCondition<JavaClass>(
                "not call UI.getCurrent()") {
            @Override
            public void check(final @NotNull JavaClass clazz, final @NotNull ConditionEvents events) {
                clazz.getMethodCallsFromSelf().forEach(call -> {
                    final var target = call.getTarget();
                    if (target.getOwner().isEquivalentTo(UI.class)
                            && target.getName().equals(forbiddenMethod)
                            && target.getRawParameterTypes().isEmpty()) {
                        events.add(SimpleConditionEvent.violated(
                                call,
                                "Forbidden call to UI.getCurrent() in class " + clazz.getName() +
                                        ": use the UI from a view or an event (BeforeEnterEvent / ClickEvent) instead"));
                    }
                });
            }
        };

        classes()
                .should(noOneMayCallUiGetCurrent)
                .because("UI.getCurrent() is global state and makes code harder to test")
                .check(classesWithoutTests);
    }

    @Test
    void routesAndAliasesMustHaveExactlyOneSecurityAnnotation() {
        final var allowed = Set.of(
                AnonymousAllowed.class.getName(),
                PermitAll.class.getName(),
                RolesAllowed.class.getName()
        );

        final ArchCondition<JavaClass> haveExactlyOneOfAllowedSecurityAnnotations =
                new ArchCondition<>("have exactly one of @AnonymousAllowed, @PermitAll, @RolesAllowed") {
                    @Override
                    public void check(final @NotNull JavaClass clazz, final @NotNull ConditionEvents events) {
                        final var present = clazz.getAnnotations().stream()
                                .map(a -> a.getRawType().getName())
                                .filter(allowed::contains)
                                .collect(Collectors.toSet());

                        if (present.isEmpty()) {
                            events.add(SimpleConditionEvent.violated(
                                    clazz,
                                    clazz.getName() + " is a Vaadin route (@Route/@RouteAlias) but has no security " +
                                            "annotation (@AnonymousAllowed/@PermitAll/@RolesAllowed)"));
                        } else if (present.size() > 1) {
                            events.add(SimpleConditionEvent.violated(
                                    clazz,
                                    clazz.getName() + " is a Vaadin route (@Route/@RouteAlias) but has multiple " +
                                            "security annotations: " + present));
                        }
                    }
                };

        classes()
                .that().areAnnotatedWith(Route.class)
                .or().areAnnotatedWith(RouteAlias.class)
                .and().doNotHaveModifier(ABSTRACT)
                .should(haveExactlyOneOfAllowedSecurityAnnotations)
                .because("every navigable Vaadin route (including aliases) must declare its access level explicitly")
                .check(classesWithoutTests);
    }

}
