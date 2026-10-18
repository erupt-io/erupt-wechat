package xyz.erupt.wechat.live;

import lombok.Getter;
import lombok.Setter;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.bean.result.WxMpUserBlacklistGetResult;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.EruptScope;
import xyz.erupt.annotation.config.QueryExpression;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.core.annotation.EruptDataProcessor;
import xyz.erupt.core.query.EruptQuery;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.core.WxScope;
import xyz.erupt.wechat.data.WxDataService;
import xyz.erupt.wechat.data.WxResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Blocked fans. Adding an openid blocks it, deleting unblocks.
 *
 * @author YuePeng
 */
@Getter
@Setter
@EruptScope(value = WxScope.class, bind = false)
@EruptDataProcessor(WxDataService.DATA_PROCESSOR)
@Erupt(name = "Blacklist", primaryKeyCol = "openid", power = @Power(edit = false))
public class WxBlacklist {

    @EruptField(views = @View(title = "OpenID"), edit = @Edit(title = "OpenID", notNull = true, search = @Search(operator = QueryExpression.LIKE)))
    private String openid;

    @Component
    public static class Resource implements WxResource<WxBlacklist> {

        private static final int MAX = 10_000;

        @Override
        public Class<WxBlacklist> type() {
            return WxBlacklist.class;
        }

        @Override
        public List<WxBlacklist> list(WxContext wx, EruptQuery query) throws WxErrorException {
            List<WxBlacklist> rows = new ArrayList<>();
            String next = null;
            WxMpUserBlacklistGetResult page;
            do {
                page = wx.mp().getBlackListService().getBlacklist(next);
                if (null != page.getOpenidList()) {
                    for (String openid : page.getOpenidList()) {
                        WxBlacklist row = new WxBlacklist();
                        row.setOpenid(openid);
                        rows.add(row);
                    }
                }
                next = page.getNextOpenid();
            } while (page.getCount() >= MAX && null != next && !next.isEmpty());
            return rows;
        }

        @Override
        public void add(WxContext wx, WxBlacklist row) throws WxErrorException {
            wx.mp().getBlackListService().pushToBlacklist(List.of(row.getOpenid()));
        }

        @Override
        public void delete(WxContext wx, WxBlacklist row) throws WxErrorException {
            wx.mp().getBlackListService().pullFromBlacklist(List.of(row.getOpenid()));
        }
    }

}
