package com.alvarengacarlos.unnamed.coreapi.testsupport;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Empties the test database before each test, so every test starts from a known state.
 */
public class DatabaseCleanerExtension implements BeforeEachCallback {
    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        SpringExtension.getApplicationContext(extensionContext)
            .getBean(Database.class)
            .clean();
    }
}
