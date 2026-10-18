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
@Erupt(name = "Preview", primaryKeyCol = "openid")
public class WxPreviewForm {

    @EruptField(edit = @Edit(title = "OpenID", notNull = true, desc = "The fan who receives the preview"))
    private String openid;

}
