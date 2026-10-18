package xyz.erupt.wechat.form;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.CodeEditorType;

/**
 * @author YuePeng
 */
@Getter
@Setter
@Erupt(name = "Send subscribe notification", primaryKeyCol = "openid")
public class WxSubscribeSendForm {

    @EruptField(edit = @Edit(title = "To", notNull = true, desc = "The OpenID of a fan who subscribed to this notification"))
    private String openid;

    @EruptField(edit = @Edit(title = "Page", desc = "Opened when the fan taps the notification; optional"))
    private String page;

    @EruptField(edit = @Edit(title = "Data", type = EditType.CODE_EDITOR, codeEditType = @CodeEditorType(language = "json"),
            desc = "{\"thing1\":\"Hello\",\"time2\":\"2026-01-01\"}"))
    private String data;

}
