package xyz.erupt.wechat.core;

import me.chanjar.weixin.mp.api.WxMpService;
import org.springframework.stereotype.Component;
import xyz.erupt.core.scope.EruptScopes;
import xyz.erupt.jpa.dao.EruptDao;
import xyz.erupt.wechat.model.WxAccount;

/**
 * The account of the current request: the one picked in the workspace, already verified by erupt.
 *
 * @author YuePeng
 */
@Component
public class WxContext {

    private final WxMpServices services;

    private final EruptDao eruptDao;

    public WxContext(WxMpServices services, EruptDao eruptDao) {
        this.services = services;
        this.eruptDao = eruptDao;
    }

    public WxScope scope() {
        return EruptScopes.get(WxScope.class);
    }

    public Long accountId() {
        return this.scope().getAccount();
    }

    public WxAccount account() {
        return eruptDao.find(WxAccount.class, this.accountId());
    }

    public WxMpService mp() {
        return services.get(this.accountId());
    }

    public WxMpService mp(Long accountId) {
        return services.get(accountId);
    }

}
