package rest.api;

import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.internal.engine.messageinterpolation.DefaultLocaleResolver;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.metadata.ConstraintDescriptor;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

public abstract class Validators {

    private static final Locale defaultLocale = Locale.JAPANESE;
    private static final Set<Locale> allowedLocales = Set.of(defaultLocale, Locale.ENGLISH);
    private static final ValidatorFactory factory = Validation.byDefaultProvider().configure().messageInterpolator(
            new ParameterMessageInterpolator(allowedLocales, defaultLocale, new DefaultLocaleResolver(), false))
            .buildValidatorFactory();

    private static Validator validator = factory.getValidator();

    public static boolean notBlank(String value) {
        return StringUtils.isNotBlank(value);
    }

    public static boolean lengthInRange(String value, int min, int max) {
        return min <= value.length() && value.length() <= max;
    }

    public static <T> T validate(Context ctx, Class<T> type) {
        T input = ctx.bodyAsClass(type);
        Set<ConstraintViolation<Object>> violations = validator.validate(input);

        if (violations.isEmpty()) {
            return input;
        }

        JSONObject errors = new JSONObject();

        violations.forEach(violation -> {
            JSONArray messages;
            String fieldName = violation.getPropertyPath().toString();

            if (errors.has(fieldName)) {
                messages = (JSONArray) errors.get(fieldName);
            } else {
                messages = new JSONArray();
            }

            messages.put(message(ctx, violation));
            errors.put(fieldName, messages);
        });

        throw new BadRequestResponse(errors.toString());
    }

    private static String message(Context ctx, ConstraintViolation<Object> violation) {
        Locale locale = ctx.req().getLocale();
        if (!allowedLocales.contains(locale) || defaultLocale.equals(locale)) {
            return violation.getMessage();
        }

        return factory.getMessageInterpolator().interpolate(violation.getMessageTemplate(),
                new MessageInterpolator.Context() {

                    @Override
                    public ConstraintDescriptor<?> getConstraintDescriptor() {
                        return violation.getConstraintDescriptor();
                    }

                    @Override
                    public Object getValidatedValue() {
                        return I18N.fieldName(violation.getPropertyPath().toString(), ctx);
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public <T> T unwrap(Class<T> type) {
                        return (T) violation.getRootBean();
                    }
                }, locale);
    }

}
