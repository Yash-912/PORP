package com.porp;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Verifies the modular structure of the application.
 *
 * <p>Fails the build if:
 * <ul>
 *   <li>A module accesses another module's internal package</li>
 *   <li>There is a cyclic dependency between modules</li>
 *   <li>A module depends on a non-existent module</li>
 * </ul>
 */
class ModularityTests {

    private static final ApplicationModules MODULES =
        ApplicationModules.of(PorpApplication.class);

    @Test
    void verifiesModularStructure() {
        MODULES.verify();
    }

    @Test
    void printsModuleStructure() {
        MODULES.forEach(System.out::println);
    }
}