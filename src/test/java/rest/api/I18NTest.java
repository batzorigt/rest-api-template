package rest.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;

public class I18NTest {

    @Test
    void testAll() throws Exception {
        I18N.load("i18nTest", Locale.getDefault(), Locale.JAPAN);

        // example of parameterized message
        // when related locale message is not found,
        // then it should return message from Japanese locale
        // if there is no message in Japanese locale,
        // it should be search from default locale
        assertEquals("1 + 1 = 2", I18N.message("message.only.in.default.locale", Locale.GERMAN, "1", 2));

        // should return related locale message
        assertEquals("日本語", I18N.message("language", Locale.JAPAN));
        assertEquals("english", I18N.message("language", Locale.getDefault()));

        // when related locale is not found,
        // then it should return message from Japanese locale
        assertEquals("メッセージ", I18N.message("message.only.in.japanese.locale", Locale.JAPAN));
        assertEquals("メッセージ", I18N.message("message.only.in.japanese.locale", Locale.GERMAN));
    }

}
