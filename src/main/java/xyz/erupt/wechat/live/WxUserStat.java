package xyz.erupt.wechat.live;

import lombok.Getter;
import lombok.Setter;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpDataCubeService;
import me.chanjar.weixin.mp.bean.datacube.WxDataCubeUserCumulate;
import me.chanjar.weixin.mp.bean.datacube.WxDataCubeUserSummary;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.EruptScope;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.annotation.sub_field.sub_edit.VL;
import xyz.erupt.core.annotation.EruptDataProcessor;
import xyz.erupt.core.query.EruptQuery;
import xyz.erupt.wechat.core.Wx;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.core.WxScope;
import xyz.erupt.wechat.data.WxDataService;
import xyz.erupt.wechat.data.WxResource;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Fans gained and lost per day and source, with the running total. The last 7 days unless a range
 * is searched; WeChat keeps 30 days.
 *
 * @author YuePeng
 */
@Getter
@Setter
@EruptScope(value = WxScope.class, bind = false)
@EruptDataProcessor(WxDataService.DATA_PROCESSOR)
@Erupt(name = "Fan statistics", power = @Power(add = false, edit = false, delete = false), orderBy = "refDate desc")
public class WxUserStat {

    @EruptField(views = @View(title = "ID", show = false))
    private String id;

    @EruptField(views = @View(title = "Date", sortable = true), edit = @Edit(title = "Date", type = EditType.DATE, search = @Search))
    private LocalDate refDate;

    @EruptField(views = @View(title = "Source"), edit = @Edit(title = "Source", type = EditType.CHOICE, search = @Search, choiceType = @ChoiceType(vl = {
            @VL(value = "0", label = "Other"), @VL(value = "1", label = "Account search"), @VL(value = "17", label = "Name card"),
            @VL(value = "30", label = "QR code"), @VL(value = "43", label = "Article menu"), @VL(value = "51", label = "Article account name"),
            @VL(value = "57", label = "Article mention"), @VL(value = "75", label = "Article ad"), @VL(value = "78", label = "Moments ad")
    })))
    private String source;

    @EruptField(views = @View(title = "New"))
    private Integer newUser;

    @EruptField(views = @View(title = "Unsubscribed"))
    private Integer cancelUser;

    @EruptField(views = @View(title = "Total fans"))
    private Integer cumulateUser;

    @Component
    public static class Resource implements WxResource<WxUserStat> {

        // one call spans 7 days at most
        private static final int SPAN = 7;

        @Override
        public Class<WxUserStat> type() {
            return WxUserStat.class;
        }

        @Override
        public List<WxUserStat> list(WxContext wx, EruptQuery query) throws WxErrorException {
            LocalDate[] range = WxStats.dateRange(WxDataService.condition(query, "refDate"), 30, 7);
            WxMpDataCubeService cube = wx.mp().getDataCubeService();
            List<WxUserStat> rows = new ArrayList<>();
            Map<LocalDate, Integer> totals = new HashMap<>();
            for (LocalDate from = range[0]; !from.isAfter(range[1]); from = from.plusDays(SPAN)) {
                LocalDate to = from.plusDays(SPAN - 1).isAfter(range[1]) ? range[1] : from.plusDays(SPAN - 1);
                for (WxDataCubeUserCumulate cumulate : cube.getUserCumulate(Wx.date(from), Wx.date(to))) {
                    totals.put(day(cumulate.getRefDate()), cumulate.getCumulateUser());
                }
                for (WxDataCubeUserSummary summary : cube.getUserSummary(Wx.date(from), Wx.date(to))) {
                    WxUserStat row = new WxUserStat();
                    row.setRefDate(day(summary.getRefDate()));
                    row.setSource(String.valueOf(summary.getUserSource()));
                    row.setId(row.getRefDate() + "@" + row.getSource());
                    row.setNewUser(summary.getNewUser());
                    row.setCancelUser(summary.getCancelUser());
                    row.setCumulateUser(totals.get(row.getRefDate()));
                    rows.add(row);
                }
            }
            return rows;
        }

        private static LocalDate day(Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
    }

}
