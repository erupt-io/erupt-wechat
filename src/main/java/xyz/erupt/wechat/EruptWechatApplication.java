package xyz.erupt.wechat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

/**
 * Runs the module on its own: {@code mvn spring-boot:run}, then http://localhost:8080 with
 * erupt / erupt. The database is an H2 file under ./h2, so nothing has to be installed. A host
 * application that adds the module as a dependency never sees this class.
 *
 * @author YuePeng
 */
@SpringBootApplication
public class EruptWechatApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(EruptWechatApplication.class);
        // Defaults only: a config file or a command line argument still wins
        application.setDefaultProperties(Map.of(
                "spring.datasource.url", "jdbc:h2:file:./h2/erupt-wechat;AUTO_SERVER=TRUE;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.username", "sa",
                "spring.datasource.driver-class-name", "org.h2.Driver",
                "spring.jpa.generate-ddl", "true",
                "spring.jpa.properties.hibernate.globally_quoted_identifiers", "true",
                "erupt.wechat.callback-domain", "http://localhost:8080"
        ));
        application.run(args);
    }

}
