package xyz.erupt.wechat.proxy;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.fun.DataProxy;
import xyz.erupt.core.exception.EruptWebApiRuntimeException;
import xyz.erupt.core.i18n.I18nTranslate;
import xyz.erupt.jpa.dao.EruptDao;
import xyz.erupt.wechat.core.WxMpServices;
import xyz.erupt.wechat.model.WxAccount;
import xyz.erupt.wechat.portal.WxPortalController;
import xyz.erupt.wechat.prop.EruptWechatProperties;

import java.util.Collection;
import java.util.Map;

/**
 * Keeps the service registry in step with the account rows.
 *
 * @author YuePeng
 */
@Component
public class WxAccountDataProxy implements DataProxy<WxAccount> {

    private final EruptDao eruptDao;

    private final EruptWechatProperties properties;

    private final WxMpServices services;

    public WxAccountDataProxy(EruptDao eruptDao, EruptWechatProperties properties, WxMpServices services) {
        this.eruptDao = eruptDao;
        this.properties = properties;
        this.services = services;
    }

    @Override
    public void beforeAdd(WxAccount account) {
        if (eruptDao.lambdaQuery(WxAccount.class).eq(WxAccount::getAppId, account.getAppId()).count() > 0) {
            throw new EruptWebApiRuntimeException(I18nTranslate.$translate("wechat.appid_exists"));
        }
    }

    @Override
    public void beforeUpdate(WxAccount account) {
        // A password field comes back empty when left alone
        WxAccount old = eruptDao.find(WxAccount.class, account.getId());
        if (StringUtils.isBlank(account.getAppSecret())) account.setAppSecret(old.getAppSecret());
        if (StringUtils.isBlank(account.getAesKey())) account.setAesKey(old.getAesKey());
    }

    @Override
    public void afterAdd(WxAccount account) {
        services.register(account);
    }

    @Override
    public void afterUpdate(WxAccount account) {
        services.register(account);
    }

    @Override
    public void afterDelete(WxAccount account) {
        services.remove(account);
    }

    @Override
    public void afterFetch(Collection<Map<String, Object>> list) {
        list.forEach(it -> it.put("serverUrl", properties.getCallbackDomain() + WxPortalController.PATH + "/" + it.get("appId")));
    }

}
