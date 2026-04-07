package com.magenc.platform;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Architecture tests powered by Spring Modulith.
 *
 * <p>{@link #verifyModuleStructure()} fails the build if any module
 * reaches across boundaries it should not.
 *
 * <p>{@link #generateDocumentation()} writes PlantUML diagrams to
 * {@code target/spring-modulith-docs} on every test run.
 */
class ApplicationModulesTest {

    private final ApplicationModules modules = ApplicationModules.of(MagencPlatformApplication.class);

    @Test
    void verifyModuleStructure() {
        modules.verify();
    }

    @Test
    void generateDocumentation() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }
}
