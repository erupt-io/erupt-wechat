package xyz.erupt.wechat;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import xyz.erupt.core.annotation.EruptScan;
import xyz.erupt.core.constant.MenuStatus;
import xyz.erupt.core.module.EruptModule;
import xyz.erupt.core.module.EruptModuleInvoke;
import xyz.erupt.core.module.MetaMenu;
import xyz.erupt.core.module.ModuleInfo;
import xyz.erupt.wechat.live.*;
import xyz.erupt.wechat.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * WeChat Official Account admin on erupt. Adding the dependency is the whole setup: the accounts
 * go in the Official Accounts page, the workspace in the header picks one, and every other page
 * follows it.
 *
 * @author YuePeng
 */
@Configuration
// The standalone launcher lives in this package too; a host application must not pick it up
@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = SpringBootApplication.class))
@EruptScan
@EntityScan
public class EruptWechatAutoConfiguration implements EruptModule {

    static {
        EruptModuleInvoke.addEruptModule(EruptWechatAutoConfiguration.class);
    }

    @Override
    public ModuleInfo info() {
        return ModuleInfo.builder().name("erupt-wechat").description("WeChat Official Account admin").build();
    }

    @Override
    public List<MetaMenu> initMenus() {
        List<MetaMenu> menus = new ArrayList<>();
        MetaMenu root = MetaMenu.createRootMenu("$wechat", "WeChat", "fa fa-weixin", 56);
        menus.add(root);
        int sort = 0;
        for (Class<?> model : List.of(WxAccount.class, WxFans.class, WxTag.class, WxBlacklist.class, WxMenu.class, WxAutoReply.class,
                WxMaterial.class, WxDraft.class, WxPublication.class, WxTemplate.class, WxSubscribeTemplate.class, WxMassMessage.class,
                WxKfAccount.class, WxKfSession.class, WxKfMessage.class, WxQrCode.class, WxEventLog.class,
                WxUserStat.class, WxArticleStat.class, WxInterfaceStat.class)) {
            menus.add(MetaMenu.createEruptClassMenu(model, root, sort += 10));
        }
        // Reached by drilling from a publication or a template
        menus.add(MetaMenu.createEruptClassMenu(WxComment.class, root, sort += 10, MenuStatus.HIDE));
        menus.add(MetaMenu.createEruptClassMenu(WxTemplateLog.class, root, sort += 10, MenuStatus.HIDE));
        return menus;
    }

}
