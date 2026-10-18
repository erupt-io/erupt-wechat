package xyz.erupt.wechat.live;

import lombok.Getter;
import lombok.Setter;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.bean.kefu.request.WxMpKfAccountRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.EruptScope;
import xyz.erupt.annotation.fun.ChoiceFetchHandler;
import xyz.erupt.annotation.fun.VLModel;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.Readonly;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.ViewType;
import xyz.erupt.core.annotation.EruptDataProcessor;
import xyz.erupt.core.query.EruptQuery;
import xyz.erupt.wechat.core.Wx;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.core.WxScope;
import xyz.erupt.wechat.data.WxDataService;
import xyz.erupt.wechat.data.WxResource;

import java.util.List;

/**
 * Customer service accounts. A new one is bound to a staff member by inviting their WeChat id.
 *
 * @author YuePeng
 */
@Getter
@Setter
@EruptScope(value = WxScope.class, bind = false)
@EruptDataProcessor(WxDataService.DATA_PROCESSOR)
@Erupt(name = "Service accounts", primaryKeyCol = "account")
public class WxKfAccount {

    @EruptField(views = @View(title = "Account"), edit = @Edit(title = "Account", notNull = true, readonly = @Readonly(add = false),
            desc = "account@your-account-id, up to 10 characters before the @"))
    private String account;

    @EruptField(views = @View(title = "Nickname"), edit = @Edit(title = "Nickname", notNull = true))
    private String nick;

    @EruptField(views = @View(title = "Avatar", type = ViewType.AVATAR))
    private String headImgUrl;

    @EruptField(views = @View(title = "Invited WeChat"), edit = @Edit(title = "Invite WeChat ID", desc = "The staff member's WeChat id; they accept in the WeChat app"))
    private String inviteWx;

    @EruptField(views = @View(title = "Invite status"))
    private String inviteStatus;

    @Component
    public static class Resource implements WxResource<WxKfAccount> {

        @Override
        public Class<WxKfAccount> type() {
            return WxKfAccount.class;
        }

        @Override
        public List<WxKfAccount> list(WxContext wx, EruptQuery query) throws WxErrorException {
            return wx.mp().getKefuService().kfList().getKfList().stream().map(it -> {
                WxKfAccount row = new WxKfAccount();
                row.setAccount(it.getAccount());
                row.setNick(it.getNick());
                row.setHeadImgUrl(it.getHeadImgUrl());
                row.setInviteWx(it.getInviteWx());
                row.setInviteStatus(it.getInviteStatus());
                return row;
            }).toList();
        }

        @Override
        public void add(WxContext wx, WxKfAccount row) throws WxErrorException {
            wx.mp().getKefuService().kfAccountAdd(WxMpKfAccountRequest.builder().kfAccount(row.getAccount()).nickName(row.getNick()).build());
            if (StringUtils.isNotBlank(row.getInviteWx())) {
                wx.mp().getKefuService().kfAccountInviteWorker(WxMpKfAccountRequest.builder().kfAccount(row.getAccount()).inviteWx(row.getInviteWx()).build());
            }
        }

        @Override
        public void edit(WxContext wx, WxKfAccount row) throws WxErrorException {
            wx.mp().getKefuService().kfAccountUpdate(WxMpKfAccountRequest.builder().kfAccount(row.getAccount()).nickName(row.getNick()).build());
        }

        @Override
        public void delete(WxContext wx, WxKfAccount row) throws WxErrorException {
            wx.mp().getKefuService().kfAccountDel(row.getAccount());
        }
    }

    @Component
    public static class Options implements ChoiceFetchHandler<Object> {

        private final WxContext context;

        private final Resource resource;

        public Options(WxContext context, Resource resource) {
            this.context = context;
            this.resource = resource;
        }

        @Override
        public List<VLModel> fetch(String[] params) {
            return Wx.call(() -> resource.list(context, EruptQuery.builder().build())).stream().map(it -> new VLModel(it.getAccount(), it.getNick())).toList();
        }
    }

}
