package xyz.erupt.wechat.live;

import lombok.Getter;
import lombok.Setter;
import me.chanjar.weixin.common.error.WxErrorException;
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
import xyz.erupt.wechat.form.WxTemplateSendForm;
import xyz.erupt.wechat.handler.WxTemplateSendHandler;
import xyz.erupt.wechat.model.WxTemplateLog;

import java.util.List;

/**
 * The account's message templates. Adding one takes a template from WeChat's library by its short id.
 *
 * @author YuePeng
 */
@Getter
@Setter
@EruptScope(value = WxScope.class, bind = false)
@EruptDataProcessor(WxDataService.DATA_PROCESSOR)
@Erupt(name = "Templates", primaryKeyCol = "templateId", power = @Power(edit = false),
        rowOperation = @RowOperation(code = "send", title = "Send", icon = "fa fa-paper-plane", mode = RowOperation.Mode.SINGLE,
                eruptClass = WxTemplateSendForm.class, operationHandler = WxTemplateSendHandler.class),
        drills = @Drill(title = "Log", icon = "fa fa-history", link = @Link(linkErupt = WxTemplateLog.class, column = "templateId", joinColumn = "templateId")))
public class WxTemplate {

    @EruptField(views = @View(title = "Template ID"))
    private String templateId;

    @EruptField(views = @View(title = "Title"), edit = @Edit(title = "Title", search = @Search(operator = QueryExpression.LIKE)))
    private String title;

    @EruptField(views = @View(title = "Content"))
    private String content;

    @EruptField(views = @View(title = "Example"))
    private String example;

    @EruptField(views = @View(title = "Industry"))
    private String industry;

    @EruptField(edit = @Edit(title = "Library template ID", notNull = true, desc = "The short id from WeChat's template library"))
    private String shortId;

    @EruptField(edit = @Edit(title = "Keywords", type = EditType.TEXTAREA, desc = "The keyword ids to take from the library template, one per line; empty takes them all"))
    private String keywords;

    @Component
    public static class Resource implements WxResource<WxTemplate> {

        @Override
        public Class<WxTemplate> type() {
            return WxTemplate.class;
        }

        @Override
        public List<WxTemplate> list(WxContext wx, EruptQuery query) throws WxErrorException {
            return wx.mp().getTemplateMsgService().getAllPrivateTemplate().stream().map(it -> {
                WxTemplate row = new WxTemplate();
                row.setTemplateId(it.getTemplateId());
                row.setTitle(it.getTitle());
                row.setContent(it.getContent());
                row.setExample(it.getExample());
                row.setIndustry(it.getPrimaryIndustry() + " / " + it.getDeputyIndustry());
                return row;
            }).toList();
        }

        @Override
        public void add(WxContext wx, WxTemplate row) throws WxErrorException {
            List<String> keywords = Wx.lines(row.getKeywords());
            row.setTemplateId(keywords.isEmpty() ? wx.mp().getTemplateMsgService().addTemplate(row.getShortId())
                    : wx.mp().getTemplateMsgService().addTemplate(row.getShortId(), keywords));
        }

        @Override
        public void delete(WxContext wx, WxTemplate row) throws WxErrorException {
            wx.mp().getTemplateMsgService().delPrivateTemplate(row.getTemplateId());
        }
    }

}
