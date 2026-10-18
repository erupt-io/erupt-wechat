package xyz.erupt.wechat.core;

import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.config.WxMpHostConfig;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import xyz.erupt.core.exception.EruptWebApiRuntimeException;
import xyz.erupt.core.i18n.I18nTranslate;
import xyz.erupt.jpa.dao.EruptDao;
import xyz.erupt.wechat.model.WxAccount;
import xyz.erupt.wechat.prop.EruptWechatProperties;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One {@link WxMpService} per account, keyed by the account row. The access token lives in memory
 * ({@code WxMpDefaultConfigImpl}); a cluster swaps in {@code WxMpRedisConfigImpl} here.
 *
 * @author YuePeng
 */
@Component
public class WxMpServices {

    private final Map<Long, WxMpService> services = new ConcurrentHashMap<>();

    private final Map<String, Long> idsByAppId = new ConcurrentHashMap<>();

    private final EruptDao eruptDao;

    private final EruptWechatProperties properties;

    public WxMpServices(EruptDao eruptDao, EruptWechatProperties properties) {
        this.eruptDao = eruptDao;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        eruptDao.lambdaQuery(WxAccount.class).list().forEach(this::register);
    }

    public void register(WxAccount account) {
        WxMpDefaultConfigImpl config = new WxMpDefaultConfigImpl();
        config.setAppId(account.getAppId());
        config.setSecret(account.getAppSecret());
        config.setToken(account.getToken());
        config.setAesKey(account.getAesKey());
        if (StringUtils.isNotBlank(properties.getApiHost())) {
            WxMpHostConfig host = new WxMpHostConfig();
            host.setApiHost(properties.getApiHost());
            config.setHostConfig(host);
        }
        WxMpService service = new WxMpServiceImpl();
        service.setWxMpConfigStorage(config);
        services.put(account.getId(), service);
        idsByAppId.put(account.getAppId(), account.getId());
    }

    public void remove(WxAccount account) {
        services.remove(account.getId());
        idsByAppId.remove(account.getAppId());
    }

    public WxMpService get(Long accountId) {
        WxMpService service = null == accountId ? null : services.get(accountId);
        if (null == service) throw new EruptWebApiRuntimeException(I18nTranslate.$translate("wechat.account_unknown") + " → " + accountId);
        return service;
    }

    public Long idOf(String appId) {
        return idsByAppId.get(appId);
    }

}
