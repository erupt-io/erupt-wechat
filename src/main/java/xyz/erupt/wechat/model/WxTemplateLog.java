package xyz.erupt.wechat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.EruptScope;
import xyz.erupt.annotation.config.QueryExpression;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;
import xyz.erupt.wechat.core.WxScope;

import java.time.LocalDateTime;

/**
 * One template message sent, then its delivery result when WeChat reports it to the portal.
 *
 * @author YuePeng
 */
@Getter
@Setter
@Entity
@Table(name = "e_wx_template_log", indexes = @Index(columnList = "msg_id"))
@EruptScope(WxScope.class)
@Erupt(name = "Template message log", power = @Power(add = false, edit = false), orderBy = "sendTime desc")
public class WxTemplateLog extends BaseModel {

    public static final String SENT = "SENT";

    public static final String DELIVERED = "DELIVERED";

    public static final String FAILED = "FAILED";

    @ManyToOne
    @EruptField(views = @View(title = "Official Account", column = "name"), edit = @Edit(title = "Official Account",
            type = EditType.REFERENCE_TABLE, referenceTableType = @ReferenceTableType(label = "name")))
    private WxAccount account;

    @EruptField(views = @View(title = "Template"), edit = @Edit(title = "Template", search = @Search(operator = QueryExpression.LIKE)))
    private String templateId;

    @EruptField(views = @View(title = "Title"))
    private String title;

    @EruptField(views = @View(title = "To"), edit = @Edit(title = "To", search = @Search(operator = QueryExpression.LIKE)))
    private String toOpenid;

    @Column(length = 4000)
    @EruptField(views = @View(title = "Data"))
    private String data;

    @EruptField(views = @View(title = "URL"))
    private String url;

    @EruptField(views = @View(title = "Message ID"))
    private String msgId;

    @EruptField(views = @View(title = "Status"), edit = @Edit(title = "Status", search = @Search))
    private String status;

    @EruptField(views = @View(title = "Error"))
    private String errorMsg;

    @EruptField(views = @View(title = "Sent at", sortable = true))
    private LocalDateTime sendTime;

}
