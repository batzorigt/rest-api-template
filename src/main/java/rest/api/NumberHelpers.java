package rest.api;

public interface NumberHelpers {

    static <T extends Number> T nullIfZero(T value) {
        if (value == null) {
            return null;
        }

        if (value.intValue() == 0) {
            return null;
        }

        return value;
    }
}
