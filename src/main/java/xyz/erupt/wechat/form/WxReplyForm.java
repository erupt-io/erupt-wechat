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
@Erupt(name = "Reply", primaryKeyCol = "content")
public class WxReplyForm {

    @EruptField(edit = @Edit(title = "Reply", notNull = true, type = EditType.TEXTAREA))
    private String content;

}
