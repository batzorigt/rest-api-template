package rest.api;

import io.ebean.annotation.Platform;
import io.ebean.dbmigration.DbMigration;

public class GenerateDbMigration {

    public static void main(String[] args) throws Exception {
        DbMigration dbMigration = DbMigration.create();
        dbMigration.setPlatform(Platform.POSTGRES);
        dbMigration.generateMigration();
    }
}
