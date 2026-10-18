package xyz.erupt.wechat.core;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;

/**
 * The workspace every WeChat page follows: one Official Account and, optionally, one of its fan
 * tags. Picked once in the page header. The account id binds the JPA models by their {@code account}
 * field; the tag narrows fans and is the default audience of a mass message.
 *
 * @author YuePeng
 */
@Getter
@Setter
@Erupt(name = "Workspace", primaryKeyCol = "account")
public class WxScope {

    @EruptField(edit = @Edit(title = "Official Account", type = EditType.CHOICE, notNull = true,
            choiceType = @ChoiceType(fetchHandler = WxAccountFetchHandler.class)))
    private Long account;

    // Left empty: every fan
    @EruptField(edit = @Edit(title = "Tag", type = EditType.CHOICE,
            choiceType = @ChoiceType(fetchHandler = WxScopeTagFetchHandler.class, dependField = "account")))
    private Long tag;

}
