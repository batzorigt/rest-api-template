package rest.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;

public class I18NJapaneseOnlyTest {

    @Test
    void testAll() throws Exception {
        I18N.load("only-japan", Locale.JAPAN);

        // should return related locale property
        assertEquals("日本語", I18N.message("language", Locale.JAPAN));

        // "text" should be only in Japanese locale
        assertEquals("テキスト", I18N.message("text", Locale.JAPAN));
        assertEquals("テキスト", I18N.message("text", Locale.GERMAN));
        assertEquals("テキスト", I18N.message("text", Locale.getDefault()));
    }

}
