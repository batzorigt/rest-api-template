package rest.api;

import java.io.File;
import java.util.List;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;

/**
 * WARNING: change "Allow less secure apps" to ON to test
 */
public class MailTest {

    List<String> to = List.of("recipient1@mail.address", "recipient2@mail.address");

    // @Test TODO uncomment to test
    void sendTextMail() throws AddressException, MessagingException {
        Mail.send(to, null, null, "text mail", "This is test!");
    }

    // @Test TODO uncomment to test
    void sendMailWithAttachment() throws AddressException, MessagingException {
        Mail.send(to, null, null, "text mail with attachment", "I sent you query.sql!", new File(
                "./src/test/resources/query.sql"));
    }

}
