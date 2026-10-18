package xyz.erupt.wechat.core;

import org.springframework.stereotype.Component;
import xyz.erupt.annotation.fun.ChoiceFetchHandler;
import xyz.erupt.annotation.fun.VLModel;
import xyz.erupt.jpa.dao.EruptDao;
import xyz.erupt.wechat.model.WxAccount;

import java.util.List;

/**
 * The accounts to pick from in the workspace.
 *
 * @author YuePeng
 */
@Component
public class WxAccountFetchHandler implements ChoiceFetchHandler<WxScope> {

    private final EruptDao eruptDao;

    public WxAccountFetchHandler(EruptDao eruptDao) {
        this.eruptDao = eruptDao;
    }

    @Override
    public List<VLModel> fetch(String[] params) {
        return eruptDao.lambdaQuery(WxAccount.class).orderBy(WxAccount::getName).list().stream()
                .map(it -> new VLModel(String.valueOf(it.getId()), it.getName())).toList();
    }

}
