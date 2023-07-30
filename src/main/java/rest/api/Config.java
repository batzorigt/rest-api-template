package rest.api;

import java.net.URI;

import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Config.Sources;

@LoadPolicy(LoadType.MERGE)
@Sources({ "file:/rapit.config", "system:env" })
public interface Config extends Accessible {

    @DefaultValue("8080")
    int portNo();

    @DefaultValue("http://localhost:8080, http://localhost:4200, http://localhost:4201, http://batzorigt.com:4200")
    String[] allowedOrigins();

    @DefaultValue("1234567890123456")
    String encryptionKey();

    @DefaultValue("false")
    boolean isSecure();

    @DefaultValue("false")
    boolean xsrfProtectionEnabled();
    
    @DefaultValue("1024")
    long httpMaxRequestSize();
    
    @DefaultValue("5000")
    long httpAsyncTimeout();

    @DefaultValue("micro")
    String monitoringUsername();

    @DefaultValue("meter")
    String monitoringPassword();

    @DefaultValue("/v1/")
    String contextPath();

    @DefaultValue("local")
    String environment();

    @DefaultValue("jte-classes")
    URI jteClassesDir();

    @DefaultValue("smtp.gmail.com")
    String smtpHost();

    @DefaultValue("587")
    String smtpPort();

    @DefaultValue("any@email.address")
    String smtpUsername();

    @DefaultValue("anypassword")
    String smtpPassword();

    @DefaultValue("true")
    String smtpAuth();

    @DefaultValue("true")
    String smtpStartTls();
}
