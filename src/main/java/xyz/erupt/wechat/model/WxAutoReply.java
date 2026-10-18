package xyz.erupt.wechat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.EruptScope;
import xyz.erupt.annotation.config.QueryExpression;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.*;
import xyz.erupt.jpa.model.MetaModel;
import xyz.erupt.wechat.core.WxScope;
import xyz.erupt.wechat.live.WxMaterial;
import xyz.erupt.wechat.proxy.WxAutoReplyDataProxy;

/**
 * Passive replies the portal sends back: by keyword, on subscribe, or by default.
 *
 * @author YuePeng
 */
@Getter
@Setter
@Entity
@Table(name = "e_wx_auto_reply")
@EruptScope(WxScope.class)
@Erupt(name = "Auto replies", dataProxy = WxAutoReplyDataProxy.class)
public class WxAutoReply extends MetaModel {

    public static final String KEYWORD_EXACT = "KEYWORD_EXACT";

    public static final String KEYWORD_CONTAIN = "KEYWORD_CONTAIN";

    public static final String SUBSCRIBE = "SUBSCRIBE";

    public static final String DEFAULT = "DEFAULT";

    public static final String TEXT = "TEXT";

    public static final String IMAGE = "IMAGE";

    public static final String NEWS = "NEWS";

    @ManyToOne
    @EruptField(views = @View(title = "Official Account", column = "name"), edit = @Edit(title = "Official Account", notNull = true,
            type = EditType.REFERENCE_TABLE, referenceTableType = @ReferenceTableType(label = "name")))
    private WxAccount account;

    @EruptField(views = @View(title = "Trigger"), edit = @Edit(title = "Trigger", notNull = true, search = @Search, type = EditType.CHOICE, choiceType = @ChoiceType(vl = {
            @VL(value = KEYWORD_EXACT, label = "Keyword (exact)"),
            @VL(value = KEYWORD_CONTAIN, label = "Keyword (contains)"),
            @VL(value = SUBSCRIBE, label = "On subscribe"),
            @VL(value = DEFAULT, label = "Default")
    })))
    private String type;

    @EruptField(views = @View(title = "Keyword"), edit = @Edit(title = "Keyword", search = @Search(operator = QueryExpression.LIKE),
            dynamic = @Dynamic(dependField = "type", condition = "value === 'KEYWORD_EXACT' || value === 'KEYWORD_CONTAIN'", match = Dynamic.Ctrl.NOTNULL)))
    private String keyword;

    @EruptField(views = @View(title = "Reply"), edit = @Edit(title = "Reply", notNull = true, search = @Search, type = EditType.CHOICE, choiceType = @ChoiceType(vl = {
            @VL(value = TEXT, label = "Text"),
            @VL(value = IMAGE, label = "Image"),
            @VL(value = NEWS, label = "Article link")
    })))
    private String replyType = TEXT;

    @Column(length = 2000)
    @EruptField(views = @View(title = "Content"), edit = @Edit(title = "Content", type = EditType.TEXTAREA,
            dynamic = @Dynamic(dependField = "replyType", condition = "value === 'TEXT'", match = Dynamic.Ctrl.NOTNULL)))
    private String content;

    @EruptField(views = @View(title = "Image"), edit = @Edit(title = "Image", type = EditType.CHOICE, desc = "A permanent image material of the account",
            choiceType = @ChoiceType(fetchHandler = WxMaterial.ImageOptions.class, validate = false),
            dynamic = @Dynamic(dependField = "replyType", condition = "value === 'IMAGE'", match = Dynamic.Ctrl.NOTNULL)))
    private String mediaId;

    @EruptField(views = @View(title = "Title"), edit = @Edit(title = "Title",
            dynamic = @Dynamic(dependField = "replyType", condition = "value === 'NEWS'", match = Dynamic.Ctrl.NOTNULL)))
    private String newsTitle;

    @EruptField(edit = @Edit(title = "Description", dynamic = @Dynamic(dependField = "replyType", condition = "value === 'NEWS'")))
    private String newsDescription;

    @EruptField(edit = @Edit(title = "Cover image URL", desc = "A public URL; WeChat loads it from the fan's phone",
            dynamic = @Dynamic(dependField = "replyType", condition = "value === 'NEWS'")))
    private String newsPicUrl;

    @EruptField(edit = @Edit(title = "Link", dynamic = @Dynamic(dependField = "replyType", condition = "value === 'NEWS'", match = Dynamic.Ctrl.NOTNULL)))
    private String newsUrl;

    @EruptField(views = @View(title = "Enabled", sortable = true), edit = @Edit(title = "Enabled", notNull = true, search = @Search,
            boolType = @BoolType(trueText = "Enabled", falseText = "Disabled")))
    private Boolean enable = true;

}
