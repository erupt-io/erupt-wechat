package xyz.erupt.wechat.form;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;

/**
 * @author YuePeng
 */
@Getter
@Setter
@Erupt(name = "Remark", primaryKeyCol = "remark")
public class WxRemarkForm {

    @EruptField(edit = @Edit(title = "Remark", notNull = true, desc = "Up to 30 characters, shown to the account's staff only"))
    private String remark;

}
