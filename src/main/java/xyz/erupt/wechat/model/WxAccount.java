package xyz.erupt.wechat.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.config.QueryExpression;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.Readonly;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModel;
import xyz.erupt.wechat.handler.WxAccountHandler;
import xyz.erupt.wechat.handler.WxFansSyncHandler;
import xyz.erupt.wechat.proxy.WxAccountDataProxy;

/**
 * An Official Account's credentials. The only page outside the workspace: it is what the workspace
 * picks from.
 *
 * @author YuePeng
 */
@Getter
@Setter
@Entity
@Table(name = "e_wx_account", uniqueConstraints = @UniqueConstraint(columnNames = "app_id"))
@Erupt(name = "Official Accounts", dataProxy = WxAccountDataProxy.class, rowOperation = {
        @RowOperation(code = "syncFans", title = "Sync fans", icon = "fa fa-users", mode = RowOperation.Mode.SINGLE,
                operationHandler = WxFansSyncHandler.class),
        @RowOperation(code = "ips", title = "Callback IPs", icon = "fa fa-globe", mode = RowOperation.Mode.SINGLE, callHint = "",
                operationHandler = WxAccountHandler.class, operationParam = WxAccountHandler.IPS),
        @RowOperation(code = "quota", title = "Clear API quota", icon = "fa fa-eraser", mode = RowOperation.Mode.SINGLE,
                tip = "Resets today's API call counters; WeChat allows this 10 times a month", operationHandler = WxAccountHandler.class, operationParam = WxAccountHandler.QUOTA)
})
public class WxAccount extends MetaModel {

    @EruptField(views = @View(title = "Name"), edit = @Edit(title = "Name", notNull = true, search = @Search(operator = QueryExpression.LIKE)))
    private String name;

    @EruptField(views = @View(title = "AppID"), edit = @Edit(title = "AppID", notNull = true, search = @Search(operator = QueryExpression.LIKE), readonly = @Readonly(add = false)))
    private String appId;

    @EruptField(edit = @Edit(title = "AppSecret", notNull = true, type = EditType.PASSWORD))
    private String appSecret;

    @EruptField(edit = @Edit(title = "Token", desc = "The token of the WeChat console's server configuration"))
    private String token;

    @EruptField(edit = @Edit(title = "EncodingAESKey", type = EditType.PASSWORD, desc = "Required in safe mode, empty in plain mode"))
    private String aesKey;

    // The URL to paste into the WeChat console, built from the callback domain on fetch
    @Transient
    @EruptField(views = @View(title = "Server URL"))
    private String serverUrl;

}
