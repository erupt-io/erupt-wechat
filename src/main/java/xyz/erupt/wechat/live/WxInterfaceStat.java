package xyz.erupt.wechat.live;

import lombok.Getter;
import lombok.Setter;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.bean.datacube.WxDataCubeInterfaceResult;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.EruptScope;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.core.annotation.EruptDataProcessor;
import xyz.erupt.core.query.EruptQuery;
import xyz.erupt.wechat.core.Wx;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.core.WxScope;
import xyz.erupt.wechat.data.WxDataService;
import xyz.erupt.wechat.data.WxResource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Calls the account made to the WeChat API per day. The last 7 days unless a range is searched.
 *
 * @author YuePeng
 */
@Getter
@Setter
@EruptScope(value = WxScope.class, bind = false)
@EruptDataProcessor(WxDataService.DATA_PROCESSOR)
@Erupt(name = "API statistics", primaryKeyCol = "refDate", power = @Power(add = false, edit = false, delete = false), orderBy = "refDate desc")
public class WxInterfaceStat {

    @EruptField(views = @View(title = "Date", sortable = true), edit = @Edit(title = "Date", type = EditType.DATE, search = @Search))
    private LocalDate refDate;

    @EruptField(views = @View(title = "Calls"))
    private Integer callbackCount;

    @EruptField(views = @View(title = "Failures"))
    private Integer failCount;

    @EruptField(views = @View(title = "Total time (ms)"))
    private Integer totalTimeCost;

    @EruptField(views = @View(title = "Slowest (ms)"))
    private Integer maxTimeCost;

    @Component
    public static class Resource implements WxResource<WxInterfaceStat> {

        @Override
        public Class<WxInterfaceStat> type() {
            return WxInterfaceStat.class;
        }

        @Override
        public List<WxInterfaceStat> list(WxContext wx, EruptQuery query) throws WxErrorException {
            LocalDate[] range = WxStats.dateRange(WxDataService.condition(query, "refDate"), 30, 7);
            List<WxInterfaceStat> rows = new ArrayList<>();
            for (WxDataCubeInterfaceResult result : wx.mp().getDataCubeService().getInterfaceSummary(Wx.date(range[0]), Wx.date(range[1]))) {
                WxInterfaceStat row = new WxInterfaceStat();
                row.setRefDate(LocalDate.parse(result.getRefDate()));
                row.setCallbackCount(result.getCallbackCount());
                row.setFailCount(result.getFailCount());
                row.setTotalTimeCost(result.getTotalTimeCost());
                row.setMaxTimeCost(result.getMaxTimeCost());
                rows.add(row);
            }
            return rows;
        }
    }

}
