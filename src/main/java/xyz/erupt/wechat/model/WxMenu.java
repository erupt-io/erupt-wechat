package xyz.erupt.wechat.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.EruptScope;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.*;
import xyz.erupt.jpa.model.MetaModel;
import xyz.erupt.wechat.core.WxScope;
import xyz.erupt.wechat.core.WxTagFetchHandler;
import xyz.erupt.wechat.handler.WxMenuPublishHandler;

/**
 * The custom menu, two levels deep, kept here and published to WeChat as a whole. A top-level menu
 * with a tag belongs to that tag's conditional menu; the rest form the menu everyone sees.
 *
 * @author YuePeng
 */
@Getter
@Setter
@Entity
@Table(name = "e_wx_menu")
@EruptScope(WxScope.class)
@Erupt(name = "Menus", orderBy = "sort", rowOperation = {
        @RowOperation(code = "publish", title = "Publish", icon = "fa fa-cloud-upload", mode = RowOperation.Mode.BUTTON,
                tip = "Replaces the account's menus on WeChat with the menus here", operationHandler = WxMenuPublishHandler.class, operationParam = WxMenuPublishHandler.PUBLISH),
        @RowOperation(code = "unpublish", title = "Remove from WeChat", icon = "fa fa-cloud-download", mode = RowOperation.Mode.BUTTON,
                tip = "Deletes every menu on WeChat; the menus here stay", operationHandler = WxMenuPublishHandler.class, operationParam = WxMenuPublishHandler.DELETE)
})
public class WxMenu extends MetaModel {

    public static final String NONE = "NONE";

    public static final String CLICK = "CLICK";

    public static final String VIEW = "VIEW";

    public static final String MINIPROGRAM = "MINIPROGRAM";

    @ManyToOne
    @EruptField(views = @View(title = "Official Account", column = "name"), edit = @Edit(title = "Official Account", notNull = true,
            type = EditType.REFERENCE_TABLE, referenceTableType = @ReferenceTableType(label = "name")))
    private WxAccount account;

    @ManyToOne
    @EruptField(views = @View(title = "Parent", column = "name"), edit = @Edit(title = "Parent", type = EditType.REFERENCE_TREE,
            desc = "Empty for a top-level menu", referenceTreeType = @ReferenceTreeType(pid = "parent.id", dependField = "account", dependColumn = "account.id")))
    private WxMenu parent;

    @EruptField(views = @View(title = "Name"), edit = @Edit(title = "Name", notNull = true, search = @Search))
    private String name;

    @EruptField(views = @View(title = "Type"), edit = @Edit(title = "Type", notNull = true, type = EditType.CHOICE, choiceType = @ChoiceType(vl = {
            @VL(value = NONE, label = "Folder (has sub menus)"),
            @VL(value = CLICK, label = "Click event"),
            @VL(value = VIEW, label = "Open URL"),
            @VL(value = MINIPROGRAM, label = "Mini program")
    })))
    private String type;

    @EruptField(views = @View(title = "Event key"), edit = @Edit(title = "Event key", desc = "The key WeChat pushes when the menu is clicked",
            dynamic = @Dynamic(dependField = "type", condition = "value === 'CLICK'", match = Dynamic.Ctrl.NOTNULL)))
    private String menuKey;

    @EruptField(views = @View(title = "URL"), edit = @Edit(title = "URL", desc = "For a mini program, the fallback page on old clients",
            dynamic = @Dynamic(dependField = "type", condition = "value === 'VIEW' || value === 'MINIPROGRAM'", match = Dynamic.Ctrl.NOTNULL)))
    private String url;

    @EruptField(edit = @Edit(title = "Mini program AppID", dynamic = @Dynamic(dependField = "type", condition = "value === 'MINIPROGRAM'", match = Dynamic.Ctrl.NOTNULL)))
    private String miniAppId;

    @EruptField(edit = @Edit(title = "Mini program page", dynamic = @Dynamic(dependField = "type", condition = "value === 'MINIPROGRAM'", match = Dynamic.Ctrl.NOTNULL)))
    private String miniPagePath;

    @EruptField(views = @View(title = "Only for tag"), edit = @Edit(title = "Only for tag", type = EditType.CHOICE,
            desc = "Top-level menus only: fans with this tag get these menus instead of the default ones",
            choiceType = @ChoiceType(fetchHandler = WxTagFetchHandler.class, validate = false)))
    // Not named "tag": that would bind it to the workspace tag, which narrows fans, not menus
    private Long forTag;

    @EruptField(views = @View(title = "Sort", sortable = true), edit = @Edit(title = "Sort", type = EditType.NUMBER))
    private Integer sort = 10;

}
