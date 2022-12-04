package rest.api;

import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Config.Sources;

/**
 * Sample using {@link Accessible}
 * 
 * @author Batzorigt Rentsen
 *
 */
@LoadPolicy(LoadType.MERGE)
@Sources({"system:env", "classpath:app-config.properties"})
public interface AppConfig extends Accessible {

    String USER();

    @DefaultValue("name")
    String name();

    @Key("print.age")
    String age(String name, int old);

    @DefaultValue("1")
    int one();

    @DefaultValue("2")
    int two();

    @DefaultValue("${one} is less than ${two}.")
    String oneIsLessThanTwo();

    String readFromClassPathConfigFile();

}
