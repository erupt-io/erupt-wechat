package xyz.erupt.wechat.live;

import lombok.Getter;
import lombok.Setter;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpKefuService;
import me.chanjar.weixin.mp.bean.kefu.result.WxMpKfInfo;
import me.chanjar.weixin.mp.bean.kefu.result.WxMpKfSession;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.EruptScope;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.core.annotation.EruptDataProcessor;
import xyz.erupt.core.query.EruptQuery;
import xyz.erupt.wechat.core.Wx;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.core.WxScope;
import xyz.erupt.wechat.data.WxDataService;
import xyz.erupt.wechat.data.WxResource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Open customer service sessions, and fans waiting for one. Adding a row opens a session for a
 * fan with an account; deleting closes it.
 *
 * @author YuePeng
 */
@Getter
@Setter
@EruptScope(value = WxScope.class, bind = false)
@EruptDataProcessor(WxDataService.DATA_PROCESSOR)
@Erupt(name = "Service sessions", primaryKeyCol = "openid", power = @Power(edit = false))
public class WxKfSession {

    @EruptField(views = @View(title = "OpenID"), edit = @Edit(title = "OpenID", notNull = true))
    private String openid;

    @EruptField(views = @View(title = "Service account"), edit = @Edit(title = "Service account", notNull = true, type = EditType.CHOICE,
            choiceType = @ChoiceType(fetchHandler = WxKfAccount.Options.class, validate = false)))
    private String kfAccount;

    @EruptField(views = @View(title = "Opened at", sortable = true))
    private LocalDateTime createTime;

    @EruptField(views = @View(title = "Last message at", sortable = true))
    private LocalDateTime latestTime;

    @Component
    public static class Resource implements WxResource<WxKfSession> {

        @Override
        public Class<WxKfSession> type() {
            return WxKfSession.class;
        }

        @Override
        public List<WxKfSession> list(WxContext wx, EruptQuery query) throws WxErrorException {
            WxMpKefuService kefu = wx.mp().getKefuService();
            List<WxKfSession> rows = new ArrayList<>();
            for (WxMpKfInfo kf : kefu.kfList().getKfList()) {
                List<WxMpKfSession> sessions = kefu.kfSessionList(kf.getAccount()).getKfSessionList();
                if (null != sessions) for (WxMpKfSession session : sessions) rows.add(this.row(session, kf.getAccount()));
            }
            List<WxMpKfSession> waiting = kefu.kfSessionGetWaitCase().getKfSessionWaitCaseList();
            if (null != waiting) for (WxMpKfSession session : waiting) rows.add(this.row(session, null));
            return rows;
        }

        private WxKfSession row(WxMpKfSession session, String kfAccount) {
            WxKfSession row = new WxKfSession();
            row.setOpenid(session.getOpenid());
            row.setKfAccount(kfAccount);
            row.setCreateTime(Wx.time(session.getCreateTime()));
            if (session.getLatestTime() > 0) row.setLatestTime(Wx.time(session.getLatestTime()));
            return row;
        }

        @Override
        public void add(WxContext wx, WxKfSession row) throws WxErrorException {
            wx.mp().getKefuService().kfSessionCreate(row.getOpenid(), row.getKfAccount());
        }

        @Override
        public void delete(WxContext wx, WxKfSession row) throws WxErrorException {
            if (null != row.getKfAccount()) wx.mp().getKefuService().kfSessionClose(row.getOpenid(), row.getKfAccount());
        }
    }

}
