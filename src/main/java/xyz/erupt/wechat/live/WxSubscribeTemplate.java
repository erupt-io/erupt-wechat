package xyz.erupt.wechat.live;

import lombok.Getter;
import lombok.Setter;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.EruptScope;
import xyz.erupt.annotation.config.QueryExpression;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
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
import xyz.erupt.wechat.form.WxSubscribeSendForm;
import xyz.erupt.wechat.handler.WxSubscribeSendHandler;

import java.util.List;

/**
 * Subscribe notification templates: fans opt in once per template, then one notification each.
 *
 * @author YuePeng
 */
@Getter
@Setter
@EruptScope(value = WxScope.class, bind = false)
@EruptDataProcessor(WxDataService.DATA_PROCESSOR)
@Erupt(name = "Subscribe notifications", primaryKeyCol = "priTmplId", power = @Power(edit = false),
        rowOperation = @RowOperation(code = "send", title = "Send", icon = "fa fa-bell", mode = RowOperation.Mode.SINGLE,
                eruptClass = WxSubscribeSendForm.class, operationHandler = WxSubscribeSendHandler.class))
public class WxSubscribeTemplate {

    @EruptField(views = @View(title = "Template ID"))
    private String priTmplId;

    @EruptField(views = @View(title = "Title"), edit = @Edit(title = "Title", search = @Search(operator = QueryExpression.LIKE)))
    private String title;

    @EruptField(views = @View(title = "Content"))
    private String content;

    @EruptField(views = @View(title = "Example"))
    private String example;

    @EruptField(views = @View(title = "Kind"))
    private String kind;

    @EruptField(edit = @Edit(title = "Library template ID", notNull = true, desc = "The tid of a public template"))
    private String tid;

    @EruptField(edit = @Edit(title = "Keyword IDs", notNull = true, desc = "The kids to use, comma separated, in display order"))
    private String kids;

    @EruptField(edit = @Edit(title = "Scene", desc = "Where the notification is used, up to 15 characters"))
    private String scene;

    @Component
    public static class Resource implements WxResource<WxSubscribeTemplate> {

        @Override
        public Class<WxSubscribeTemplate> type() {
            return WxSubscribeTemplate.class;
        }

        @Override
        public List<WxSubscribeTemplate> list(WxContext wx, EruptQuery query) throws WxErrorException {
            return wx.mp().getSubscribeMsgService().getTemplateList().stream().map(it -> {
                WxSubscribeTemplate row = new WxSubscribeTemplate();
                row.setPriTmplId(it.getPriTmplId());
                row.setTitle(it.getTitle());
                row.setContent(it.getContent());
                row.setExample(it.getExample());
                row.setKind(2 == it.getType() ? "One-time" : "Long-term");
                return row;
            }).toList();
        }

        @Override
        public void add(WxContext wx, WxSubscribeTemplate row) throws WxErrorException {
            List<Integer> kids = Wx.lines(row.getKids().replace(',', '\n')).stream().map(Integer::valueOf).toList();
            row.setPriTmplId(wx.mp().getSubscribeMsgService().addTemplate(row.getTid(), kids, row.getScene()));
        }

        @Override
        public void delete(WxContext wx, WxSubscribeTemplate row) throws WxErrorException {
            wx.mp().getSubscribeMsgService().delTemplate(row.getPriTmplId());
        }
    }

}
