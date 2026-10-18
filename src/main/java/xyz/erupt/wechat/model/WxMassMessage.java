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
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.*;
import xyz.erupt.jpa.model.MetaModel;
import xyz.erupt.wechat.core.WxScope;
import xyz.erupt.wechat.core.WxTagFetchHandler;
import xyz.erupt.wechat.form.WxPreviewForm;
import xyz.erupt.wechat.handler.WxMassMessageHandler;
import xyz.erupt.wechat.proxy.WxMassMessageDataProxy;

import java.time.LocalDateTime;

/**
 * A mass message: saving it sends it. The audience is the workspace tag, or openids typed in; both
 * empty sends to everyone. Needs a verified service account.
 *
 * @author YuePeng
 */
@Getter
@Setter
@Entity
@Table(name = "e_wx_mass_message", indexes = @Index(columnList = "msg_id"))
@EruptScope(WxScope.class)
@Erupt(name = "Mass messages", dataProxy = WxMassMessageDataProxy.class, power = @Power(edit = false), orderBy = "sendTime desc", rowOperation = {
        @RowOperation(code = "preview", title = "Preview", icon = "fa fa-eye", mode = RowOperation.Mode.SINGLE, tip = "Sends the message to one openid only",
                eruptClass = WxPreviewForm.class, operationHandler = WxMassMessageHandler.class, operationParam = WxMassMessageHandler.PREVIEW),
        @RowOperation(code = "status", title = "Refresh status", icon = "fa fa-refresh", mode = RowOperation.Mode.SINGLE, callHint = "",
                operationHandler = WxMassMessageHandler.class, operationParam = WxMassMessageHandler.STATUS),
        @RowOperation(code = "cancel", title = "Cancel", icon = "fa fa-times", mode = RowOperation.Mode.SINGLE, tip = "Withdraws a sent message within half an hour",
                operationHandler = WxMassMessageHandler.class, operationParam = WxMassMessageHandler.CANCEL)
})
public class WxMassMessage extends MetaModel {

    public static final String SENDING = "SENDING";

    @ManyToOne
    @EruptField(views = @View(title = "Official Account", column = "name"), edit = @Edit(title = "Official Account", notNull = true,
            type = EditType.REFERENCE_TABLE, referenceTableType = @ReferenceTableType(label = "name")))
    private WxAccount account;

    @EruptField(views = @View(title = "Tag"), edit = @Edit(title = "Tag", type = EditType.CHOICE, desc = "Empty: everyone, unless openids are given",
            choiceType = @ChoiceType(fetchHandler = WxTagFetchHandler.class, validate = false)))
    private Long tag;

    @Column(length = 4000)
    @EruptField(edit = @Edit(title = "OpenIDs", type = EditType.TEXTAREA, desc = "One per line, 2 to 10000; sends to these instead of the tag"))
    private String openids;

    @EruptField(views = @View(title = "Type"), edit = @Edit(title = "Type", notNull = true, type = EditType.CHOICE, choiceType = @ChoiceType(vl = {
            @VL(value = "text", label = "Text"),
            @VL(value = "mpnews", label = "Article"),
            @VL(value = "image", label = "Image"),
            @VL(value = "voice", label = "Voice"),
            @VL(value = "mpvideo", label = "Video")
    })))
    private String msgType = "text";

    @Column(length = 2000)
    @EruptField(views = @View(title = "Content"), edit = @Edit(title = "Content", type = EditType.TEXTAREA,
            dynamic = @Dynamic(dependField = "msgType", condition = "value === 'text'", match = Dynamic.Ctrl.NOTNULL)))
    private String content;

    @EruptField(views = @View(title = "Media ID"), edit = @Edit(title = "Media ID", desc = "A permanent material, or an article uploaded for mass sending",
            dynamic = @Dynamic(dependField = "msgType", condition = "value && value !== 'text'", match = Dynamic.Ctrl.NOTNULL)))
    private String mediaId;

    @EruptField(views = @View(title = "Message ID"))
    private String msgId;

    @EruptField(views = @View(title = "Status"), edit = @Edit(title = "Status", search = @Search))
    private String status;

    @EruptField(views = @View(title = "Total"))
    private Integer totalCount;

    @EruptField(views = @View(title = "Sent"))
    private Integer sentCount;

    @EruptField(views = @View(title = "Errors"))
    private Integer errorCount;

    @EruptField(views = @View(title = "Sent at", sortable = true))
    private LocalDateTime sendTime;

}
