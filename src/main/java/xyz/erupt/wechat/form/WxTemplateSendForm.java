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
@Erupt(name = "Send template message", primaryKeyCol = "openids")
public class WxTemplateSendForm {

    @EruptField(edit = @Edit(title = "To", notNull = true, type = EditType.TEXTAREA, desc = "OpenIDs, one per line"))
    private String openids;

    @EruptField(edit = @Edit(title = "URL", desc = "Opened when the fan taps the message; optional"))
    private String url;

    @EruptField(edit = @Edit(title = "Data", type = EditType.CODE_EDITOR, codeEditType = @CodeEditorType(language = "json"),
            desc = "{\"first\":\"Hello\"} or {\"first\":{\"value\":\"Hello\",\"color\":\"#ff0000\"}}"))
    private String data;

}
