package xyz.erupt.wechat.portal;

import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.web.bind.annotation.*;
import xyz.erupt.wechat.core.WxMpServices;

/**
 * Where WeChat calls back: {@code {callback-domain}/wechat/portal/{appid}}, the server URL of the
 * account's WeChat console configuration. Plain and safe (AES) modes both work.
 *
 * @author YuePeng
 */
@Slf4j
@RestController
@RequestMapping(WxPortalController.PATH + "/{appid}")
public class WxPortalController {

    public static final String PATH = "/wechat/portal";

    private static final String REFUSED = "Invalid signature";

    private final WxMpServices services;

    private final WxReplyService replyService;

    public WxPortalController(WxMpServices services, WxReplyService replyService) {
        this.services = services;
        this.replyService = replyService;
    }

    // WeChat verifies the URL once with a GET
    @GetMapping(produces = "text/plain;charset=utf-8")
    public String check(@PathVariable("appid") String appid, @RequestParam("signature") String signature,
                        @RequestParam("timestamp") String timestamp, @RequestParam("nonce") String nonce, @RequestParam("echostr") String echostr) {
        Long accountId = services.idOf(appid);
        return null != accountId && services.get(accountId).checkSignature(timestamp, nonce, signature) ? echostr : REFUSED;
    }

    @PostMapping(produces = "application/xml;charset=utf-8")
    public String receive(@PathVariable("appid") String appid, @RequestBody String body, @RequestParam("signature") String signature,
                          @RequestParam("timestamp") String timestamp, @RequestParam("nonce") String nonce,
                          @RequestParam(value = "encrypt_type", required = false) String encryptType,
                          @RequestParam(value = "msg_signature", required = false) String msgSignature) {
        Long accountId = services.idOf(appid);
        if (null == accountId) return REFUSED;
        WxMpService mp = services.get(accountId);
        if (!mp.checkSignature(timestamp, nonce, signature)) return REFUSED;
        boolean aes = "aes".equalsIgnoreCase(encryptType);
        WxMpXmlMessage in = aes ? WxMpXmlMessage.fromEncryptedXml(body, mp.getWxMpConfigStorage(), timestamp, nonce, msgSignature) : WxMpXmlMessage.fromXml(body);
        try {
            WxMpXmlOutMessage out = replyService.receive(accountId, in);
            // "success" tells WeChat the message arrived and nothing is to be sent back
            if (null == out) return "success";
            return aes ? out.toEncryptedXml(mp.getWxMpConfigStorage()) : out.toXml();
        } catch (RuntimeException e) {
            log.error("WeChat message of {} not handled", appid, e);
            return "success";
        }
    }

}
