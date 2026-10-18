package xyz.erupt.wechat.prop;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * <pre>
 * erupt:
 *   wechat:
 *     callback-domain: https://admin.example.com   # shown as the server URL to paste into the WeChat console
 *     api-host: https://api.weixin.qq.com          # override to go through a proxy, or a stub in tests
 * </pre>
 *
 * @author YuePeng
 */
@Getter
@Setter
@Component
@ConfigurationProperties("erupt.wechat")
public class EruptWechatProperties {

    private String callbackDomain = "";

    private String apiHost = "";

}
