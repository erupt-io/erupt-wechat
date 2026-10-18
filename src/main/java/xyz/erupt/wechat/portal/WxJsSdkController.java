package xyz.erupt.wechat.portal;

import me.chanjar.weixin.common.bean.WxJsapiSignature;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.erupt.core.exception.EruptWebApiRuntimeException;
import xyz.erupt.core.i18n.I18nTranslate;
import xyz.erupt.wechat.core.Wx;
import xyz.erupt.wechat.core.WxMpServices;

/**
 * The {@code wx.config} signature for a page of the host application:
 * {@code GET /wechat/jssdk/{appid}?url=<the page's URL>}.
 *
 * @author YuePeng
 */
@RestController
public class WxJsSdkController {

    private final WxMpServices services;

    public WxJsSdkController(WxMpServices services) {
        this.services = services;
    }

    @GetMapping("/wechat/jssdk/{appid}")
    public WxJsapiSignature signature(@PathVariable("appid") String appid, @RequestParam("url") String url) {
        Long accountId = services.idOf(appid);
        if (null == accountId) throw new EruptWebApiRuntimeException(I18nTranslate.$translate("wechat.account_unknown") + " → " + appid);
        return Wx.call(() -> services.get(accountId).createJsapiSignature(url));
    }

}
