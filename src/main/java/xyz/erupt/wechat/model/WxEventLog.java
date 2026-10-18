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
 * Everything the portal receives: messages fans send and the events WeChat pushes.
 *
 * @author YuePeng
 */
@Getter
@Setter
@Entity
@Table(name = "e_wx_event_log", indexes = @Index(columnList = "openid"))
@EruptScope(WxScope.class)
@Erupt(name = "Inbox", power = @Power(add = false, edit = false), orderBy = "createTime desc")
public class WxEventLog extends BaseModel {

    @ManyToOne
    @EruptField(views = @View(title = "Official Account", column = "name"), edit = @Edit(title = "Official Account",
            type = EditType.REFERENCE_TABLE, referenceTableType = @ReferenceTableType(label = "name")))
    private WxAccount account;

    @EruptField(views = @View(title = "OpenID"), edit = @Edit(title = "OpenID", search = @Search(operator = QueryExpression.LIKE)))
    private String openid;

    @EruptField(views = @View(title = "Type"), edit = @Edit(title = "Type", search = @Search))
    private String msgType;

    @EruptField(views = @View(title = "Event"), edit = @Edit(title = "Event", search = @Search))
    private String event;

    @EruptField(views = @View(title = "Event key"))
    private String eventKey;

    @Column(length = 4000)
    @EruptField(views = @View(title = "Content"), edit = @Edit(title = "Content", search = @Search(operator = QueryExpression.LIKE)))
    private String content;

    @EruptField(views = @View(title = "Message ID"))
    private String msgId;

    @EruptField(views = @View(title = "Received at", sortable = true), edit = @Edit(title = "Received at", search = @Search))
    private LocalDateTime createTime;

}
