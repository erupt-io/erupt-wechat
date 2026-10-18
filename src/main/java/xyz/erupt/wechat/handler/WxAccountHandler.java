package xyz.erupt.wechat.handler;

import me.chanjar.weixin.mp.api.WxMpService;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.erupt.wechat.core.Wx;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.model.WxAccount;

import java.util.List;

/**
 * Account-level API calls: the IPs WeChat calls back from (for a firewall), and the quota reset.
 *
 * @author YuePeng
 */
@Component
public class WxAccountHandler implements OperationHandler<WxAccount, Void> {

    public static final String IPS = "ips";

    public static final String QUOTA = "quota";

    private final WxContext context;

    public WxAccountHandler(WxContext context) {
        this.context = context;
    }

    @Override
    public String exec(List<WxAccount> data, Void form, String[] param) {
        WxAccount account = data.get(0);
        WxMpService mp = context.mp(account.getId());
        return switch (param[0]) {
            case IPS -> "alert('" + String.join(", ", Wx.call(mp::getCallbackIP)) + "')";
            case QUOTA -> {
                Wx.run(() -> mp.clearQuota(account.getAppId()));
                yield "msg.success('OK')";
            }
            default -> null;
        };
    }

}
