package xyz.erupt.wechat.handler;

import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.erupt.jpa.dao.EruptDao;
import xyz.erupt.wechat.core.Wx;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.form.WxKefuForm;
import xyz.erupt.wechat.form.WxRemarkForm;
import xyz.erupt.wechat.form.WxTagForm;
import xyz.erupt.wechat.model.WxFans;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Row operations on fans; each one is an API call on the workspace account and a local update.
 *
 * @author YuePeng
 */
@Component
public class WxFansHandler implements OperationHandler<WxFans, Object> {

    public static final String TAG = "tag";

    public static final String UNTAG = "untag";

    public static final String REMARK = "remark";

    public static final String MESSAGE = "message";

    public static final String BLOCK = "block";

    public static final String UNBLOCK = "unblock";

    // WeChat tags up to 50 fans per call, blocks up to 20
    private static final int TAG_BATCH = 50;

    private static final int BLOCK_BATCH = 20;

    private final EruptDao eruptDao;

    private final WxContext context;

    public WxFansHandler(EruptDao eruptDao, WxContext context) {
        this.eruptDao = eruptDao;
        this.context = context;
    }

    @Override
    @Transactional
    public String exec(List<WxFans> data, Object form, String[] param) {
        WxMpService mp = context.mp();
        List<WxFans> fans = data.stream().map(it -> eruptDao.find(WxFans.class, it.getId())).toList();
        List<String> openids = fans.stream().map(WxFans::getOpenid).toList();
        switch (param[0]) {
            case TAG, UNTAG -> {
                Long tag = ((WxTagForm) form).getTag();
                boolean tagging = TAG.equals(param[0]);
                for (int i = 0; i < openids.size(); i += TAG_BATCH) {
                    String[] batch = openids.subList(i, Math.min(i + TAG_BATCH, openids.size())).toArray(String[]::new);
                    Wx.run(() -> {
                        if (tagging) mp.getUserTagService().batchTagging(tag, batch);
                        else mp.getUserTagService().batchUntagging(tag, batch);
                    });
                }
                for (WxFans fan : fans) {
                    Set<Long> ids = WxFansSyncHandler.tagIds(fan.getTagIds());
                    if (tagging) ids.add(tag);
                    else ids.remove(tag);
                    fan.setTagIds(WxFansSyncHandler.tagIds(ids));
                    eruptDao.merge(fan);
                }
            }
            case REMARK -> {
                String remark = ((WxRemarkForm) form).getRemark();
                Wx.run(() -> mp.getUserService().userUpdateRemark(openids.get(0), remark));
                fans.get(0).setRemark(remark);
                eruptDao.merge(fans.get(0));
            }
            case MESSAGE -> {
                WxMpKefuMessage message = WxMpKefuMessage.TEXT().toUser(openids.get(0)).content(((WxKefuForm) form).getText()).build();
                Wx.run(() -> mp.getKefuService().sendKefuMessage(message));
            }
            case BLOCK, UNBLOCK -> {
                boolean block = BLOCK.equals(param[0]);
                for (int i = 0; i < openids.size(); i += BLOCK_BATCH) {
                    List<String> batch = new ArrayList<>(openids.subList(i, Math.min(i + BLOCK_BATCH, openids.size())));
                    Wx.run(() -> {
                        if (block) mp.getBlackListService().pushToBlacklist(batch);
                        else mp.getBlackListService().pullFromBlacklist(batch);
                    });
                }
            }
            default -> {
            }
        }
        return "msg.success('OK')";
    }

}
