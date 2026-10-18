package xyz.erupt.wechat.form;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.EruptScope;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.wechat.core.WxScope;
import xyz.erupt.wechat.core.WxTagFetchHandler;

/**
 * @author YuePeng
 */
@Getter
@Setter
@EruptScope(WxScope.class)
@Erupt(name = "Pick a tag", primaryKeyCol = "tag")
public class WxTagForm {

    @EruptField(edit = @Edit(title = "Tag", notNull = true, type = EditType.CHOICE, choiceType = @ChoiceType(fetchHandler = WxTagFetchHandler.class)))
    private Long tag;

}
