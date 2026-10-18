package xyz.erupt.wechat.live;

import lombok.Getter;
import lombok.Setter;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.bean.kefu.result.WxMpKfMsgRecord;
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
import xyz.erupt.wechat.core.Wx;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.core.WxScope;
import xyz.erupt.wechat.data.WxDataService;
import xyz.erupt.wechat.data.WxResource;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The customer service conversation log, a day at a time: the last 24 hours unless a range is searched.
 *
 * @author YuePeng
 */
@Getter
@Setter
@EruptScope(value = WxScope.class, bind = false)
@EruptDataProcessor(WxDataService.DATA_PROCESSOR)
@Erupt(name = "Service messages", power = @Power(add = false, edit = false, delete = false), orderBy = "time desc")
public class WxKfMessage {

    @EruptField(views = @View(title = "ID", show = false))
    private String id;

    @EruptField(views = @View(title = "OpenID"), edit = @Edit(title = "OpenID", search = @Search(operator = QueryExpression.LIKE)))
    private String openid;

    @EruptField(views = @View(title = "Service account"), edit = @Edit(title = "Service account", search = @Search(operator = QueryExpression.LIKE)))
    private String worker;

    @EruptField(views = @View(title = "Direction"))
    private String direction;

    @EruptField(views = @View(title = "Text"), edit = @Edit(title = "Text", search = @Search(operator = QueryExpression.LIKE)))
    private String text;

    @EruptField(views = @View(title = "Time", sortable = true), edit = @Edit(title = "Time", search = @Search, desc = "Up to 30 days back"))
    private LocalDateTime time;

    @Component
    public static class Resource implements WxResource<WxKfMessage> {

        private static final int MAX_DAYS = 30;

        @Override
        public Class<WxKfMessage> type() {
            return WxKfMessage.class;
        }

        @Override
        public List<WxKfMessage> list(WxContext wx, EruptQuery query) throws WxErrorException {
            LocalDateTime[] range = WxStats.dateTimeRange(WxDataService.condition(query, "time"), MAX_DAYS, LocalDateTime.now().minusDays(1));
            List<WxKfMessage> rows = new ArrayList<>();
            // one call covers 24 hours at most
            for (LocalDateTime from = range[0]; from.isBefore(range[1]); from = from.plusDays(1)) {
                LocalDateTime to = from.plusDays(1).isBefore(range[1]) ? from.plusDays(1) : range[1];
                List<WxMpKfMsgRecord> records = wx.mp().getKefuService().kfMsgList(toDate(from), toDate(to)).getRecords();
                if (null == records) continue;
                for (WxMpKfMsgRecord record : records) {
                    WxKfMessage row = new WxKfMessage();
                    row.setId(record.getOpenid() + "@" + record.getTime());
                    row.setOpenid(record.getOpenid());
                    row.setWorker(record.getWorker());
                    // 1000 the fan wrote, 2002 the account wrote, 2003 the session was closed, 1001 a note
                    row.setDirection(Integer.valueOf(1000).equals(record.getOperateCode()) ? "From fan" : "From account");
                    row.setText(record.getText());
                    row.setTime(Wx.time(record.getTime()));
                    rows.add(row);
                }
            }
            return rows;
        }

        private static Date toDate(LocalDateTime time) {
            return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
        }
    }

}
