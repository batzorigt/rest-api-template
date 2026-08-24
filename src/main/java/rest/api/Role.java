package rest.api;

import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import io.javalin.security.RouteRole;

public enum Role implements RouteRole {

    USER(0),
    MANAGER(10),
    ADMIN(20);

    public static final String claim = "role";

    private final int level;

    Role(int level) {
        this.level = level;
    }

    public boolean satisfies(Set<? extends RouteRole> required) {
        return required.stream().filter(Role.class::isInstance).map(Role.class::cast)
                .anyMatch(requiredRole -> level >= requiredRole.level);
    }

    public static Role parse(String value) {
        if (StringUtils.isBlank(value)) {
            return USER;
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknownRole) {
            return USER;
        }
    }
}
