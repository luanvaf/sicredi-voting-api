package dev.luan.sicredi.voting.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "dev.luan.sicredi.voting",
    importOptions = ImportOption.DoNotIncludeTests.class)
class LayerDependencyArchitectureTest {

  private static final String DOMAIN_MODEL = "dev.luan.sicredi.voting.domainmodel..";
  private static final String ENGINE = "dev.luan.sicredi.voting.engine..";
  private static final String SERVICE = "dev.luan.sicredi.voting.service..";

  @ArchTest
  static final ArchRule domainModelsRemainFrameworkFree =
      classes()
          .that()
          .resideInAPackage(DOMAIN_MODEL)
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage("java..", DOMAIN_MODEL);

  @ArchTest
  static final ArchRule enginesRemainPure =
      classes()
          .that()
          .resideInAPackage(ENGINE)
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage("java..", DOMAIN_MODEL, ENGINE);

  @ArchTest
  static final ArchRule servicesDependOnlyOnBusinessPolicy =
      classes()
          .that()
          .resideInAPackage(SERVICE)
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage("java..", DOMAIN_MODEL, ENGINE, SERVICE);
}
