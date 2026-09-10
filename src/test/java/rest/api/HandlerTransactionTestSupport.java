package rest.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.ebean.DB;
import io.ebean.Transaction;
import io.ebean.annotation.Transactional;
import io.javalin.http.Context;

public abstract class HandlerTransactionTestSupport {

    protected Context ctx;
    protected Transaction transaction;

    @BeforeEach
    void prepareContext() {
        assertNull(DB.currentTransaction(), "Tests must enter handlers without an enclosing transaction");
        I18N.load(Locale.JAPAN);
        ctx = mock(Context.class, RETURNS_DEEP_STUBS);
        when(ctx.req().getLocale()).thenReturn(Locale.JAPAN);
        transaction = null;
    }

    protected void captureTransaction(boolean readOnly) {
        captureTransaction();
        assertEquals(readOnly, transaction.isReadOnly());
    }

    protected void captureTransaction() {
        Transaction current = DB.currentTransaction();
        assertNotNull(current, "The enhanced handler must start a transaction");
        assertTrue(current.isActive());
        if (transaction != null) {
            assertSame(transaction, current, "The handler must keep its work in the same transaction");
        }
        transaction = current;
    }

    protected void assertReadOnlyTransactionDeclared(Class<?> handlerType, String methodName) {
        var method = assertDoesNotThrow(() -> handlerType.getDeclaredMethod(methodName, Context.class));
        Transactional annotation = method.getAnnotation(Transactional.class);
        assertNotNull(annotation, "The handler method must declare @Transactional");
        assertTrue(annotation.readOnly(), "The handler method must declare a read-only transaction");
    }

    protected void assertTransactionClosed() {
        assertNotNull(transaction, "The handler must reach the transaction assertion");
        assertFalse(transaction.isActive(), "The handler must complete its transaction");
        assertNull(DB.currentTransaction(), "The handler must clear its thread-local transaction");
    }

    @AfterEach
    protected void checkTransactionCleanup() {
        try {
            assertNull(DB.currentTransaction(), "The handler must not leak a transaction to the next test");
        } finally {
            Transaction current = DB.currentTransaction();
            if (current != null) {
                current.end();
            }
        }
    }
}
