package rest.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

public class RoleTest {

    @Test
    void userSatisfiesUserOnly() {
        assertTrue(Role.USER.satisfies(Set.of(Role.USER)));
        assertFalse(Role.USER.satisfies(Set.of(Role.MANAGER)));
        assertFalse(Role.USER.satisfies(Set.of(Role.ADMIN)));
    }

    @Test
    void managerSatisfiesManagerAndBelow() {
        assertTrue(Role.MANAGER.satisfies(Set.of(Role.USER)));
        assertTrue(Role.MANAGER.satisfies(Set.of(Role.MANAGER)));
        assertFalse(Role.MANAGER.satisfies(Set.of(Role.ADMIN)));
    }

    @Test
    void adminSatisfiesEverything() {
        assertTrue(Role.ADMIN.satisfies(Set.of(Role.USER)));
        assertTrue(Role.ADMIN.satisfies(Set.of(Role.MANAGER)));
        assertTrue(Role.ADMIN.satisfies(Set.of(Role.ADMIN)));
    }

    @Test
    void anyOfSatisfies() {
        assertTrue(Role.USER.satisfies(Set.of(Role.ADMIN, Role.MANAGER, Role.USER)));
        assertTrue(Role.ADMIN.satisfies(Set.of(Role.ADMIN, Role.MANAGER)));
        assertFalse(Role.MANAGER.satisfies(Set.of()));
    }

    @Test
    void parseKnownValues() {
        assertSame(Role.USER, Role.parse("USER"));
        assertSame(Role.MANAGER, Role.parse("manager"));
        assertSame(Role.ADMIN, Role.parse(" Admin "));
    }

    @Test
    void parseUnknownOrBlankFallsBackToUser() {
        assertSame(Role.USER, Role.parse(null));
        assertSame(Role.USER, Role.parse(""));
        assertSame(Role.USER, Role.parse("SUPERUSER"));
    }
}
