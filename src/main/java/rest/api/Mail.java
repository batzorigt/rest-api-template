package rest.api;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

public interface Mail {

    static void send(List<String> to,
                     List<String> cc,
                     List<String> bcc,
                     String subject,
                     String text) throws AddressException, MessagingException {
        Message message = messageWithoutBody(to, cc, bcc, subject, text);
        message.setText(text);
        Transport.send(message);
    }

    static void send(List<String> to,
                     List<String> cc,
                     List<String> bcc,
                     String subject,
                     String text,
                     File attachment) throws AddressException, MessagingException { // TODO multiple attachments
        Message message = messageWithoutBody(to, cc, bcc, subject, text);

        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(text, "text/plain");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);

        mimeBodyPart = new MimeBodyPart();
        DataSource source = new FileDataSource(attachment);

        mimeBodyPart.setDataHandler(new DataHandler(source));
        mimeBodyPart.setFileName(attachment.getName());

        multipart.addBodyPart(mimeBodyPart);
        message.setContent(multipart);

        Transport.send(message);
    }

    private static Message messageWithoutBody(List<String> to,
                                              List<String> cc,
                                              List<String> bcc,
                                              String subject,
                                              String text) throws AddressException, MessagingException {
        if (CollectionUtils.isEmpty(to)) {
            throw new IllegalArgumentException("'to' is empty!");
        }

        if (StringUtils.isBlank(subject)) {
            throw new IllegalArgumentException("'subject' is empty!");
        }

        if (StringUtils.isBlank(text)) {
            throw new IllegalArgumentException("'text' is empty!");
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", API.cfg.smtpHost());
        props.put("mail.smtp.port", API.cfg.smtpPort());
        props.put("mail.smtp.auth", API.cfg.smtpAuth());
        props.put("mail.smtp.starttls.enable", API.cfg.smtpStartTls());

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(API.cfg.smtpUsername(), API.cfg.smtpPassword());
            }
        });

        Message message = new MimeMessage(session);
        message.setSubject(subject);
        InternetAddress[] toAddresses = InternetAddress.parse(to.toString().replace("[", "").replace("]", ""));
        message.setRecipients(Message.RecipientType.TO, toAddresses);

        if (CollectionUtils.isNotEmpty(cc)) {
            InternetAddress[] ccAddresses = InternetAddress.parse(cc.toString().replace("[", "").replace("]", ""));
            message.setRecipients(Message.RecipientType.CC, ccAddresses);

        }

        if (CollectionUtils.isNotEmpty(bcc)) {
            InternetAddress[] bccAddresses = InternetAddress.parse(bcc.toString().replace("[", "").replace("]", ""));
            message.setRecipients(Message.RecipientType.BCC, bccAddresses);
        }

        return message;
    }

}
