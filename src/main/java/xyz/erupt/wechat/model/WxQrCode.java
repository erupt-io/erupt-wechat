package xyz.erupt.wechat.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.EruptScope;
import xyz.erupt.annotation.config.QueryExpression;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.ViewType;
import xyz.erupt.annotation.sub_field.sub_edit.*;
import xyz.erupt.jpa.model.MetaModel;
import xyz.erupt.wechat.core.WxScope;
import xyz.erupt.wechat.proxy.WxQrCodeDataProxy;

/**
 * Parameter QR codes: a scan pushes the scene to the portal, so each code can be told apart.
 *
 * @author YuePeng
 */
@Getter
@Setter
@Entity
@Table(name = "e_wx_qr_code")
@EruptScope(WxScope.class)
@Erupt(name = "QR codes", dataProxy = WxQrCodeDataProxy.class, power = @Power(edit = false), orderBy = "createTime desc")
public class WxQrCode extends MetaModel {

    public static final String TEMP = "TEMP";

    public static final String PERMANENT = "PERMANENT";

    @ManyToOne
    @EruptField(views = @View(title = "Official Account", column = "name"), edit = @Edit(title = "Official Account", notNull = true,
            type = EditType.REFERENCE_TABLE, referenceTableType = @ReferenceTableType(label = "name")))
    private WxAccount account;

    @EruptField(views = @View(title = "Type"), edit = @Edit(title = "Type", notNull = true, search = @Search, type = EditType.CHOICE, choiceType = @ChoiceType(vl = {
            @VL(value = TEMP, label = "Temporary"),
            @VL(value = PERMANENT, label = "Permanent")
    })))
    private String type = TEMP;

    @EruptField(views = @View(title = "Scene"), edit = @Edit(title = "Scene", notNull = true, search = @Search(operator = QueryExpression.LIKE),
            desc = "Carried in the scan event; up to 64 characters"))
    private String scene;

    @EruptField(edit = @Edit(title = "Expires in (seconds)", type = EditType.NUMBER, desc = "Up to 30 days",
            dynamic = @Dynamic(dependField = "type", condition = "value === 'TEMP'", match = Dynamic.Ctrl.NOTNULL)))
    private Integer expireSeconds = 2592000;

    @EruptField(views = @View(title = "QR code", type = ViewType.IMAGE))
    private String picUrl;

    @EruptField(views = @View(title = "URL"))
    private String url;

    private String ticket;

}
