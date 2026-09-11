package rest.api;

import java.io.File;
import java.util.List;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Manual SMTP integration examples. Configure a disposable SMTP account and
 * recipient addresses before enabling.
 */
@Disabled("Requires external SMTP credentials and recipient addresses")
public class MailTest {

    List<String> to = List.of("recipient1@mail.address", "recipient2@mail.address");

    @Test
    void sendTextMail() throws AddressException, MessagingException {
        Mail.send(to, null, null, "text mail", "This is test!");
    }

    @Test
    void sendMailWithAttachment() throws AddressException, MessagingException {
        Mail.send(to, null, null, "text mail with attachment", "I sent you query.sql!", new File(
                "./src/test/resources/query.sql"));
    }

}
