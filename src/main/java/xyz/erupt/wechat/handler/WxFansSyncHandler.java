package xyz.erupt.wechat.handler;

import me.chanjar.weixin.mp.api.WxMpUserService;
import me.chanjar.weixin.mp.bean.result.WxMpUser;
import me.chanjar.weixin.mp.bean.result.WxMpUserList;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.erupt.jpa.dao.EruptDao;
import xyz.erupt.wechat.core.Wx;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.model.WxAccount;
import xyz.erupt.wechat.model.WxFans;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reads every fan of an account: the openid list (10000 a page), then details 100 at a time.
 * Runs from an account row, or from the Fans page's button for the workspace account.
 *
 * @author YuePeng
 */
@Component
public class WxFansSyncHandler implements OperationHandler<Object, Void> {

    private static final int DETAIL_BATCH = 100;

    private final EruptDao eruptDao;

    private final WxContext context;

    public WxFansSyncHandler(EruptDao eruptDao, WxContext context) {
        this.eruptDao = eruptDao;
        this.context = context;
    }

    @Override
    @Transactional
    public String exec(List<Object> data, Void form, String[] param) {
        Long accountId = null != data && !data.isEmpty() && data.get(0) instanceof WxAccount account ? account.getId() : context.accountId();
        return "msg.success('" + this.sync(accountId) + "')";
    }

    public int sync(Long accountId) {
        WxAccount account = eruptDao.find(WxAccount.class, accountId);
        WxMpUserService users = context.mp(accountId).getUserService();
        Map<String, WxFans> existing = eruptDao.lambdaQuery(WxFans.class)
                .with(WxFans::getAccount).eq(WxAccount::getId, accountId).with()
                .list().stream().collect(Collectors.toMap(WxFans::getOpenid, Function.identity()));
        int total = 0;
        String next = null;
        WxMpUserList page;
        do {
            String cursor = next;
            page = Wx.call(() -> users.userList(cursor));
            List<String> openids = null == page.getOpenids() ? List.of() : page.getOpenids();
            for (int i = 0; i < openids.size(); i += DETAIL_BATCH) {
                List<String> batch = openids.subList(i, Math.min(i + DETAIL_BATCH, openids.size()));
                for (WxMpUser user : Wx.call(() -> users.userInfoList(batch))) {
                    this.upsert(account, existing, user);
                    total++;
                }
            }
            next = page.getNextOpenid();
        } while (page.getCount() >= 10000 && null != next && !next.isEmpty());
        return total;
    }

    private void upsert(WxAccount account, Map<String, WxFans> existing, WxMpUser user) {
        WxFans fans = existing.get(user.getOpenId());
        if (null == fans) {
            fans = new WxFans();
            fans.setAccount(account);
            fans.setOpenid(user.getOpenId());
        }
        fans.setUnionid(user.getUnionId());
        fans.setSubscribe(!Boolean.FALSE.equals(user.getSubscribe()));
        fans.setSubscribeTime(Wx.time(user.getSubscribeTime()));
        fans.setSubscribeScene(user.getSubscribeScene());
        fans.setRemark(user.getRemark());
        fans.setTagIds(tagIds(null == user.getTagIds() ? List.of() : Arrays.asList(user.getTagIds())));
        eruptDao.merge(fans);
    }

    // ",1,2," so one id matches as ",1," and never inside another
    public static String tagIds(Collection<Long> ids) {
        return ids.isEmpty() ? null : "," + ids.stream().map(String::valueOf).collect(Collectors.joining(",")) + ",";
    }

    public static Set<Long> tagIds(String stored) {
        Set<Long> ids = new LinkedHashSet<>();
        if (null != stored) for (String id : stored.split(",")) if (!id.isEmpty()) ids.add(Long.valueOf(id));
        return ids;
    }

}
