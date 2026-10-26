package com.alvarengacarlos.unnamed.coreapi.testsupport;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import com.alvarengacarlos.unnamed.coreapi.OrganizationContext;

/**
 * Runs the @BeforeEach, @Test and @AfterEach methods of an {@link OrganizationScoped} test inside its organization.
 */
public class OrganizationContextExtension implements InvocationInterceptor {
    @Override
    public void interceptBeforeEachMethod(
        Invocation<Void> invocation,
        ReflectiveInvocationContext<Method> invocationContext,
        ExtensionContext extensionContext) throws Throwable {
        final Optional<UUID> organizationId = findOrganizationId(extensionContext);
        if (organizationId.isPresent()) {
            ScopedValue.where(OrganizationContext.ORGANIZATION, organizationId.get()).call(invocation::proceed);
        } else {
            invocation.proceed();
        }
    }

    @Override
    public void interceptTestMethod(
        Invocation<Void> invocation,
        ReflectiveInvocationContext<Method> invocationContext,
        ExtensionContext extensionContext) throws Throwable {
        final Optional<UUID> organizationId = findOrganizationId(extensionContext);
        if (organizationId.isPresent()) {
            ScopedValue.where(OrganizationContext.ORGANIZATION, organizationId.get()).call(invocation::proceed);
        } else {
            invocation.proceed();
        }
    }

    @Override
    public void interceptTestTemplateMethod(
        Invocation<Void> invocation,
        ReflectiveInvocationContext<Method> invocationContext,
        ExtensionContext extensionContext) throws Throwable {
        final Optional<UUID> organizationId = findOrganizationId(extensionContext);
        if (organizationId.isPresent()) {
            ScopedValue.where(OrganizationContext.ORGANIZATION, organizationId.get()).call(invocation::proceed);
        } else {
            invocation.proceed();
        }
    }

    @Override
    public void interceptAfterEachMethod(
        Invocation<Void> invocation,
        ReflectiveInvocationContext<Method> invocationContext,
        ExtensionContext extensionContext) throws Throwable {
        final Optional<UUID> organizationId = findOrganizationId(extensionContext);
        if (organizationId.isPresent()) {
            ScopedValue.where(OrganizationContext.ORGANIZATION, organizationId.get()).call(invocation::proceed);
        } else {
            invocation.proceed();
        }
    }

    private Optional<UUID> findOrganizationId(ExtensionContext extensionContext) {
        return extensionContext.getRequiredTestInstances()
            .getAllInstances()
            .stream()
            .filter(OrganizationScoped.class::isInstance)
            .map(OrganizationScoped.class::cast)
            .map(OrganizationScoped::organizationId)
            .findFirst();
    }
}
