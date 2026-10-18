package xyz.erupt.wechat.proxy;

import org.springframework.stereotype.Component;
import xyz.erupt.annotation.fun.DataProxy;
import xyz.erupt.core.exception.EruptWebApiRuntimeException;
import xyz.erupt.core.i18n.I18nTranslate;
import xyz.erupt.jpa.dao.EruptDao;
import xyz.erupt.wechat.model.WxAccount;
import xyz.erupt.wechat.model.WxAutoReply;

/**
 * One enabled subscribe reply and one default reply per account.
 *
 * @author YuePeng
 */
@Component
public class WxAutoReplyDataProxy implements DataProxy<WxAutoReply> {

    private final EruptDao eruptDao;

    public WxAutoReplyDataProxy(EruptDao eruptDao) {
        this.eruptDao = eruptDao;
    }

    @Override
    public void beforeAdd(WxAutoReply reply) {
        this.check(reply);
    }

    @Override
    public void beforeUpdate(WxAutoReply reply) {
        this.check(reply);
    }

    private void check(WxAutoReply reply) {
        if (!Boolean.TRUE.equals(reply.getEnable())) return;
        if (!WxAutoReply.SUBSCRIBE.equals(reply.getType()) && !WxAutoReply.DEFAULT.equals(reply.getType())) return;
        long count = eruptDao.lambdaQuery(WxAutoReply.class)
                .with(WxAutoReply::getAccount).eq(WxAccount::getId, reply.getAccount().getId()).with()
                .eq(WxAutoReply::getType, reply.getType())
                .eq(WxAutoReply::getEnable, true)
                .ne(null != reply.getId(), WxAutoReply::getId, reply.getId())
                .count();
        if (count > 0) throw new EruptWebApiRuntimeException(I18nTranslate.$translate("wechat.reply_exists"));
    }

}
