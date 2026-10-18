package xyz.erupt.wechat.form;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;

/**
 * @author YuePeng
 */
@Getter
@Setter
@Erupt(name = "Send a message", primaryKeyCol = "text")
public class WxKefuForm {

    @EruptField(edit = @Edit(title = "Text", notNull = true, type = EditType.TEXTAREA))
    private String text;

}
