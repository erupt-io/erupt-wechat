package xyz.erupt.wechat.live;

import lombok.Getter;
import lombok.Setter;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.bean.draft.*;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.EruptScope;
import xyz.erupt.annotation.config.QueryExpression;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.ViewType;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.core.annotation.EruptDataProcessor;
import xyz.erupt.core.query.EruptQuery;
import xyz.erupt.wechat.core.Wx;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.core.WxScope;
import xyz.erupt.wechat.data.WxDataService;
import xyz.erupt.wechat.data.WxResource;
import xyz.erupt.wechat.handler.WxDraftPublishHandler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The draft box: one article per draft, written here and published from here. Multi-article
 * drafts made in the WeChat editor show their first article and keep the rest.
 *
 * @author YuePeng
 */
@Getter
@Setter
@EruptScope(value = WxScope.class, bind = false)
@EruptDataProcessor(WxDataService.DATA_PROCESSOR)
@Erupt(name = "Drafts", primaryKeyCol = "mediaId", rowOperation = @RowOperation(code = "publish", title = "Publish", icon = "fa fa-paper-plane",
        mode = RowOperation.Mode.SINGLE, tip = "Publishes the draft to the account's article list; it is not pushed to fans", operationHandler = WxDraftPublishHandler.class))
public class WxDraft {

    @EruptField(views = @View(title = "Media ID"))
    private String mediaId;

    @EruptField(views = @View(title = "Title"), edit = @Edit(title = "Title", notNull = true, search = @Search(operator = QueryExpression.LIKE)))
    private String title;

    @EruptField(views = @View(title = "Author"), edit = @Edit(title = "Author"))
    private String author;

    @EruptField(edit = @Edit(title = "Digest", type = EditType.TEXTAREA, desc = "Shown under the title in the feed; the first 54 characters when empty"))
    private String digest;

    @EruptField(edit = @Edit(title = "Cover image", notNull = true, type = EditType.CHOICE, choiceType = @ChoiceType(fetchHandler = WxMaterial.ImageOptions.class, validate = false)))
    private String thumbMediaId;

    @EruptField(edit = @Edit(title = "Content", notNull = true, type = EditType.HTML_EDITOR, desc = "Images must be WeChat URLs; the editor cannot upload them"))
    private String content;

    @EruptField(edit = @Edit(title = "Read more URL"))
    private String contentSourceUrl;

    @EruptField(views = @View(title = "Articles"))
    private Integer articles;

    @EruptField(views = @View(title = "Preview", type = ViewType.LINK))
    private String url;

    @EruptField(views = @View(title = "Updated at", sortable = true))
    private LocalDateTime updateTime;

    @Component
    public static class Resource implements WxResource<WxDraft> {

        private static final int PAGE = 20;

        private static final int MAX = 200;

        @Override
        public Class<WxDraft> type() {
            return WxDraft.class;
        }

        @Override
        public List<WxDraft> list(WxContext wx, EruptQuery query) throws WxErrorException {
            List<WxDraft> rows = new ArrayList<>();
            int offset = 0;
            WxMpDraftList page;
            do {
                page = wx.mp().getDraftService().listDraft(offset, PAGE, 0);
                if (null != page.getItems()) {
                    for (WxMpDraftItem item : page.getItems()) {
                        WxDraft row = new WxDraft();
                        row.setMediaId(item.getMediaId());
                        row.setUpdateTime(Wx.time(item.getUpdateTime()));
                        List<WxMpDraftArticles> articles = null == item.getContent() ? null : item.getContent().getNewsItem();
                        if (null != articles && !articles.isEmpty()) {
                            WxMpDraftArticles first = articles.get(0);
                            row.setTitle(first.getTitle());
                            row.setAuthor(first.getAuthor());
                            row.setDigest(first.getDigest());
                            row.setThumbMediaId(first.getThumbMediaId());
                            row.setContent(first.getContent());
                            row.setContentSourceUrl(first.getContentSourceUrl());
                            row.setUrl(first.getUrl());
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
        public void add(WxContext wx, WxDraft row) throws WxErrorException {
            row.setMediaId(wx.mp().getDraftService().addDraft(new WxMpAddDraft(List.of(this.article(row)))));
        }

        @Override
        public void edit(WxContext wx, WxDraft row) throws WxErrorException {
            wx.mp().getDraftService().updateDraft(new WxMpUpdateDraft(row.getMediaId(), 0, this.article(row)));
        }

        @Override
        public void delete(WxContext wx, WxDraft row) throws WxErrorException {
            wx.mp().getDraftService().delDraft(row.getMediaId());
        }

        private WxMpDraftArticles article(WxDraft row) {
            return WxMpDraftArticles.builder().title(row.getTitle()).author(row.getAuthor()).digest(row.getDigest())
                    .content(row.getContent()).contentSourceUrl(row.getContentSourceUrl()).thumbMediaId(row.getThumbMediaId()).build();
        }
    }

}
