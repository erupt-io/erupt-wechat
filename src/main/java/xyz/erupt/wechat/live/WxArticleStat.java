package xyz.erupt.wechat.live;

import lombok.Getter;
import lombok.Setter;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.bean.datacube.WxDataCubeArticleResult;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.EruptScope;
import xyz.erupt.annotation.config.QueryExpression;
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
 * Reads and shares of the articles sent each day. The last 7 days unless a range is searched.
 *
 * @author YuePeng
 */
@Getter
@Setter
@EruptScope(value = WxScope.class, bind = false)
@EruptDataProcessor(WxDataService.DATA_PROCESSOR)
@Erupt(name = "Article statistics", power = @Power(add = false, edit = false, delete = false), orderBy = "refDate desc")
public class WxArticleStat {

    @EruptField(views = @View(title = "ID", show = false))
    private String id;

    @EruptField(views = @View(title = "Date", sortable = true), edit = @Edit(title = "Date", type = EditType.DATE, search = @Search))
    private LocalDate refDate;

    @EruptField(views = @View(title = "Title"), edit = @Edit(title = "Title", search = @Search(operator = QueryExpression.LIKE)))
    private String title;

    @EruptField(views = @View(title = "Readers", sortable = true))
    private Integer readUser;

    @EruptField(views = @View(title = "Reads", sortable = true))
    private Integer readCount;

    @EruptField(views = @View(title = "Sharers"))
    private Integer shareUser;

    @EruptField(views = @View(title = "Shares", sortable = true))
    private Integer shareCount;

    @EruptField(views = @View(title = "Favorites"))
    private Integer favCount;

    @Component
    public static class Resource implements WxResource<WxArticleStat> {

        @Override
        public Class<WxArticleStat> type() {
            return WxArticleStat.class;
        }

        @Override
        public List<WxArticleStat> list(WxContext wx, EruptQuery query) throws WxErrorException {
            LocalDate[] range = WxStats.dateRange(WxDataService.condition(query, "refDate"), 30, 7);
            List<WxArticleStat> rows = new ArrayList<>();
            // one call spans a single day
            for (LocalDate day = range[0]; !day.isAfter(range[1]); day = day.plusDays(1)) {
                for (WxDataCubeArticleResult article : wx.mp().getDataCubeService().getArticleSummary(Wx.date(day), Wx.date(day))) {
                    WxArticleStat row = new WxArticleStat();
                    row.setRefDate(null == article.getRefDate() ? day : LocalDate.parse(article.getRefDate()));
                    row.setId(row.getRefDate() + "@" + article.getMsgId());
                    row.setTitle(article.getTitle());
                    row.setReadUser(article.getIntPageReadUser());
                    row.setReadCount(article.getIntPageReadCount());
                    row.setShareUser(article.getShareUser());
                    row.setShareCount(article.getShareCount());
                    row.setFavCount(article.getAddToFavCount());
                    rows.add(row);
                }
            }
            return rows;
        }
    }

}
