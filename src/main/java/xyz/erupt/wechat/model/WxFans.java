package xyz.erupt.wechat.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.EruptScope;
import xyz.erupt.annotation.config.QueryExpression;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.BoolType;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;
import xyz.erupt.wechat.core.WxScope;
import xyz.erupt.wechat.form.WxKefuForm;
import xyz.erupt.wechat.form.WxRemarkForm;
import xyz.erupt.wechat.form.WxTagForm;
import xyz.erupt.wechat.handler.WxFansHandler;
import xyz.erupt.wechat.handler.WxFansSyncHandler;
import xyz.erupt.wechat.proxy.WxFansDataProxy;

import java.time.LocalDateTime;

/**
 * Fans, synced from WeChat and kept current by subscribe / unsubscribe events. The workspace tag
 * narrows the list.
 *
 * @author YuePeng
 */
@Getter
@Setter
@Entity
@Table(name = "e_wx_fans", indexes = @Index(columnList = "openid"))
@EruptScope(WxScope.class)
@Erupt(name = "Fans", dataProxy = WxFansDataProxy.class, power = @Power(add = false, edit = false), orderBy = "subscribeTime desc",
        rowOperation = {
                @RowOperation(code = "sync", title = "Sync", icon = "fa fa-refresh", mode = RowOperation.Mode.BUTTON,
                        tip = "Reads every fan of the account; large accounts take a while", operationHandler = WxFansSyncHandler.class),
                @RowOperation(code = "tag", title = "Tag", icon = "fa fa-tag", mode = RowOperation.Mode.MULTI,
                        eruptClass = WxTagForm.class, operationHandler = WxFansHandler.class, operationParam = WxFansHandler.TAG),
                @RowOperation(code = "untag", title = "Untag", icon = "fa fa-tag", mode = RowOperation.Mode.MULTI,
                        eruptClass = WxTagForm.class, operationHandler = WxFansHandler.class, operationParam = WxFansHandler.UNTAG),
                @RowOperation(code = "remark", title = "Remark", icon = "fa fa-pencil", mode = RowOperation.Mode.SINGLE,
                        eruptClass = WxRemarkForm.class, operationHandler = WxFansHandler.class, operationParam = WxFansHandler.REMARK),
                @RowOperation(code = "message", title = "Message", icon = "fa fa-comment", mode = RowOperation.Mode.SINGLE,
                        tip = "A customer service message: WeChat delivers it within 48 hours of the fan's last message",
                        eruptClass = WxKefuForm.class, operationHandler = WxFansHandler.class, operationParam = WxFansHandler.MESSAGE),
                @RowOperation(code = "block", title = "Block", icon = "fa fa-ban", mode = RowOperation.Mode.MULTI,
                        operationHandler = WxFansHandler.class, operationParam = WxFansHandler.BLOCK),
                @RowOperation(code = "unblock", title = "Unblock", icon = "fa fa-unlock", mode = RowOperation.Mode.MULTI,
                        operationHandler = WxFansHandler.class, operationParam = WxFansHandler.UNBLOCK)
        })
public class WxFans extends BaseModel {

    @ManyToOne
    @EruptField(views = @View(title = "Official Account", column = "name"), edit = @Edit(title = "Official Account", type = EditType.REFERENCE_TABLE,
            referenceTableType = @ReferenceTableType(label = "name")))
    private WxAccount account;

    @EruptField(views = @View(title = "OpenID"), edit = @Edit(title = "OpenID", search = @Search(operator = QueryExpression.LIKE)))
    private String openid;

    @EruptField(views = @View(title = "UnionID"))
    private String unionid;

    @EruptField(views = @View(title = "Subscribed"), edit = @Edit(title = "Subscribed", search = @Search,
            boolType = @BoolType(trueText = "Subscribed", falseText = "Unsubscribed")))
    private Boolean subscribe;

    @EruptField(views = @View(title = "Subscribed at", sortable = true))
    private LocalDateTime subscribeTime;

    @EruptField(views = @View(title = "Scene"))
    private String subscribeScene;

    @EruptField(views = @View(title = "Remark"), edit = @Edit(title = "Remark", search = @Search(operator = QueryExpression.LIKE)))
    private String remark;

    // Tag ids with a comma on both sides, ",1,2,", so one tag matches as ",1," and never as part of another id
    @EruptField(views = @View(title = "Tag IDs", show = false))
    private String tagIds;

    // Tag names, resolved on fetch
    @Transient
    @EruptField(views = @View(title = "Tags"))
    private String tags;

}
