package io.terminus.parana.item.migrate;

import io.terminus.parana.migrate.api.Migrate;
import io.terminus.parana.migrate.flyway.FlywayMigrate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Mail: xiao@terminus.io <br>
 * Date: 2015-11-09 3:49 PM  <br>
 * Author: xiao
 */
@Component
public class ItemMigration {

    @Autowired
    private DataSource dataSource;

    public void init() {
        Migrate migrate = new FlywayMigrate(dataSource);
        ((FlywayMigrate)migrate).setBaselineVersionAsString("2_0_0");
        ((FlywayMigrate)migrate).setBaselineDescription("parana item installed");
        ((FlywayMigrate)migrate).setTable("schema_version_item");
        ((FlywayMigrate)migrate).setLocations("classpath:/migration/item");
        ((FlywayMigrate)migrate).setSqlMigrationPrefix("v");
        ((FlywayMigrate)migrate).setBaselineOnMigrate(true);
        ((FlywayMigrate)migrate).setDataSource(dataSource);
        migrate.migrate();
    }








}
