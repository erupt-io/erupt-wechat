package xyz.erupt.wechat.live;

import lombok.Getter;
import lombok.Setter;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.bean.freepublish.WxMpFreePublishArticles;
import me.chanjar.weixin.mp.bean.freepublish.WxMpFreePublishItem;
import me.chanjar.weixin.mp.bean.freepublish.WxMpFreePublishList;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.EruptScope;
import xyz.erupt.annotation.config.QueryExpression;
import xyz.erupt.annotation.sub_erupt.Drill;
import xyz.erupt.annotation.sub_erupt.Link;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.ViewType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.core.annotation.EruptDataProcessor;
import xyz.erupt.core.query.EruptQuery;
import xyz.erupt.wechat.core.Wx;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.core.WxScope;
import xyz.erupt.wechat.data.WxDataService;
import xyz.erupt.wechat.data.WxResource;
import xyz.erupt.wechat.handler.WxPublicationHandler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Published articles. Deleting one takes it offline.
 *
 * @author YuePeng
 */
@Getter
@Setter
@EruptScope(value = WxScope.class, bind = false)
@EruptDataProcessor(WxDataService.DATA_PROCESSOR)
@Erupt(name = "Publications", primaryKeyCol = "articleId", power = @Power(add = false, edit = false),
        rowOperation = {
                @RowOperation(code = "openComments", title = "Open comments", icon = "fa fa-comments", mode = RowOperation.Mode.MULTI,
                        operationHandler = WxPublicationHandler.class, operationParam = WxPublicationHandler.OPEN_COMMENTS),
                @RowOperation(code = "closeComments", title = "Close comments", icon = "fa fa-comments-o", mode = RowOperation.Mode.MULTI,
                        operationHandler = WxPublicationHandler.class, operationParam = WxPublicationHandler.CLOSE_COMMENTS)
        },
        drills = @Drill(title = "Comments", icon = "fa fa-comments", link = @Link(linkErupt = WxComment.class, column = "articleId", joinColumn = "msgDataId")))
public class WxPublication {

    @EruptField(views = @View(title = "Article ID"))
    private String articleId;

    @EruptField(views = @View(title = "Title"), edit = @Edit(title = "Title", search = @Search(operator = QueryExpression.LIKE)))
    private String title;

    @EruptField(views = @View(title = "Author"))
    private String author;

    @EruptField(views = @View(title = "Articles"))
    private Integer articles;

    @EruptField(views = @View(title = "Link", type = ViewType.LINK))
    private String url;

    @EruptField(views = @View(title = "Published at", sortable = true))
    private LocalDateTime updateTime;

    @Component
    public static class Resource implements WxResource<WxPublication> {

        private static final int PAGE = 20;

        private static final int MAX = 1000;

        @Override
        public Class<WxPublication> type() {
            return WxPublication.class;
        }

        @Override
        public List<WxPublication> list(WxContext wx, EruptQuery query) throws WxErrorException {
            List<WxPublication> rows = new ArrayList<>();
            int offset = 0;
            WxMpFreePublishList page;
            do {
                page = wx.mp().getFreePublishService().getPublicationRecords(offset, PAGE, 1);
                if (null != page.getItems()) {
                    for (WxMpFreePublishItem item : page.getItems()) {
                        WxPublication row = new WxPublication();
                        row.setArticleId(item.getArticleId());
                        if (StringUtils.isNumeric(item.getUpdateTime())) row.setUpdateTime(Wx.time(Long.parseLong(item.getUpdateTime())));
                        List<WxMpFreePublishArticles> articles = null == item.getContent() ? null : item.getContent().getNewsItem();
                        if (null != articles && !articles.isEmpty()) {
                            row.setTitle(articles.get(0).getTitle());
                            row.setAuthor(articles.get(0).getAuthor());
                            row.setUrl(articles.get(0).getUrl());
                            row.setArticles(articles.size());
                        }
                        rows.add(row);
                    }
                }
                offset += PAGE;
            } while (null != page.getTotalCount() && offset < page.getTotalCount() && offset < MAX);
            return rows;
        }

        @Override
        public void delete(WxContext wx, WxPublication row) throws WxErrorException {
            wx.mp().getFreePublishService().deletePushAllArticle(row.getArticleId());
        }
    }

}
