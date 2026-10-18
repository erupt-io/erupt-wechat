package xyz.erupt.wechat.live;

import lombok.Getter;
import lombok.Setter;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.bean.tag.WxUserTag;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.EruptScope;
import xyz.erupt.annotation.config.QueryExpression;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.core.annotation.EruptDataProcessor;
import xyz.erupt.core.query.EruptQuery;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.core.WxScope;
import xyz.erupt.wechat.core.WxTagCache;
import xyz.erupt.wechat.data.WxDataService;
import xyz.erupt.wechat.data.WxResource;

import java.util.List;

/**
 * Fan tags, read from and written to WeChat as they are.
 *
 * @author YuePeng
 */
@Getter
@Setter
@EruptScope(value = WxScope.class, bind = false)
@EruptDataProcessor(WxDataService.DATA_PROCESSOR)
@Erupt(name = "Tags")
public class WxTag {

    @EruptField(views = @View(title = "ID"))
    private Long id;

    @EruptField(views = @View(title = "Name"), edit = @Edit(title = "Name", notNull = true, search = @Search(operator = QueryExpression.LIKE)))
    private String name;

    @EruptField(views = @View(title = "Fans", sortable = true))
    private Integer count;

    @Component
    public static class Resource implements WxResource<WxTag> {

        private final WxTagCache tagCache;

        public Resource(WxTagCache tagCache) {
            this.tagCache = tagCache;
        }

        @Override
        public Class<WxTag> type() {
            return WxTag.class;
        }

        @Override
        public List<WxTag> list(WxContext wx, EruptQuery query) {
            return tagCache.tags(wx.accountId()).stream().map(it -> {
                WxTag tag = new WxTag();
                tag.setId(it.getId());
                tag.setName(it.getName());
                tag.setCount(it.getCount());
                return tag;
            }).toList();
        }

        @Override
        public void add(WxContext wx, WxTag tag) throws WxErrorException {
            WxUserTag created = wx.mp().getUserTagService().tagCreate(tag.getName());
            tag.setId(created.getId());
            tag.setCount(0);
            tagCache.evict(wx.accountId());
        }

        @Override
        public void edit(WxContext wx, WxTag tag) throws WxErrorException {
            wx.mp().getUserTagService().tagUpdate(tag.getId(), tag.getName());
            tagCache.evict(wx.accountId());
        }

        @Override
        public void delete(WxContext wx, WxTag tag) throws WxErrorException {
            wx.mp().getUserTagService().tagDelete(tag.getId());
            tagCache.evict(wx.accountId());
        }
    }

}
