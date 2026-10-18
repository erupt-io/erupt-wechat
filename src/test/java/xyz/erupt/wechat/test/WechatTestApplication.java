package xyz.erupt.wechat.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import xyz.erupt.core.annotation.EruptScan;

/**
 * The application the tests boot: erupt with upms on H2 plus this module, the way a user adds it.
 */
@EruptScan
@EntityScan
@SpringBootApplication
public class WechatTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(WechatTestApplication.class, args);
    }

}
