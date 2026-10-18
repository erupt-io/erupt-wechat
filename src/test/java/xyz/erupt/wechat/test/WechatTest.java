package xyz.erupt.wechat.test;

import com.google.gson.JsonObject;
import jakarta.annotation.Resource;
import me.chanjar.weixin.common.util.crypto.SHA1;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import xyz.erupt.core.context.MetaContext;
import xyz.erupt.core.context.MetaErupt;
import xyz.erupt.core.context.MetaUser;
import xyz.erupt.core.exception.EruptWebApiRuntimeException;
import xyz.erupt.core.module.MetaMenu;
import xyz.erupt.core.module.MetaUserinfo;
import xyz.erupt.core.scope.EruptScopeService;
import xyz.erupt.core.service.EruptCoreService;
import xyz.erupt.core.service.EruptModifyService;
import xyz.erupt.core.service.EruptService;
import xyz.erupt.core.util.Erupts;
import xyz.erupt.core.view.EruptModel;
import xyz.erupt.core.view.TableQuery;
import xyz.erupt.jpa.dao.EruptDao;
import xyz.erupt.upms.service.EruptTokenService;
import xyz.erupt.wechat.core.WxMpServices;
import xyz.erupt.wechat.handler.WxFansSyncHandler;
import xyz.erupt.wechat.handler.WxMenuPublishHandler;
import xyz.erupt.wechat.live.WxTag;
import xyz.erupt.wechat.model.*;
import xyz.erupt.wechat.portal.WxPortalController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The module against a stub of the WeChat API: the workspace picks the account, live pages read
 * through it, JPA pages are bound to it, and the portal keeps fans and logs current.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WechatTest {

    private static final WxApiStub stub = new WxApiStub();

    @DynamicPropertySource
    static void apiHost(DynamicPropertyRegistry registry) {
        registry.add("erupt.wechat.api-host", stub::url);
    }

    @Resource
    private EruptDao eruptDao;

    @Resource
    private WxMpServices services;

    @Resource
    private EruptService eruptService;

    @Resource
    private EruptModifyService eruptModifyService;

    @Resource
    private EruptTokenService eruptTokenService;

    @Resource
    private WxFansSyncHandler fansSyncHandler;

    @Resource
    private WxMenuPublishHandler menuPublishHandler;

    @Autowired
    private TestRestTemplate rest;

    @Resource
    private PlatformTransactionManager transactionManager;

    private static WxAccount account;

    private String token;

    @AfterAll
    static void stopStub() {
        stub.stop();
    }

    @BeforeEach
    void setUp() {
        stub.reset();
        stub.on("/cgi-bin/tags/get", "{\"tags\":[{\"id\":2,\"name\":\"VIP\",\"count\":1},{\"id\":3,\"name\":\"New\",\"count\":0}]}");
        if (null == account) {
            account = new WxAccount();
            account.setName("Demo");
            account.setAppId("wx-demo");
            account.setAppSecret("secret");
            account.setToken("portal-token");
            this.persist(account);
            services.register(account);
        }
        List<MetaMenu> menus = new ArrayList<>();
        for (Class<?> model : List.of(WxTag.class, WxFans.class, WxAutoReply.class, WxMenu.class)) {
            menus.add(MetaMenu.createEruptClassMenu(model, null, 1));
        }
        MetaUserinfo user = new MetaUserinfo();
        user.setId(-40L);
        user.setAccount("wechat-user");
        user.setUsername("wechat-user");
        token = Erupts.generateCode(22);
        eruptTokenService.loginToken(user, menus, token, 5);
        this.workspace(account.getId(), null);
    }

    @AfterEach
    void tearDown() {
        MetaContext.remove();
        RequestContextHolder.resetRequestAttributes();
    }

    // A fresh request in the workspace a page would send; a null account sends no pick at all
    private void workspace(Long accountId, Long tag) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (null != accountId) {
            JsonObject form = new JsonObject();
            form.addProperty("account", String.valueOf(accountId));
            if (null != tag) form.addProperty("tag", String.valueOf(tag));
            request.addHeader(EruptScopeService.HEADER, URLEncoder.encode("{\"WxScope\":" + form + "}", StandardCharsets.UTF_8));
        }
        request.addHeader("token", token);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MetaContext.remove();
        MetaContext.register(new MetaUser(-40L, "wechat-user", "wechat-user"));
        MetaContext.registerToken(token);
        MetaContext.register(new MetaErupt(WxTag.class.getSimpleName()));
    }

    private void persist(Object entity) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> eruptDao.persist(entity));
    }

    private static EruptModel model(Class<?> clazz) {
        return EruptCoreService.getErupt(clazz.getSimpleName());
    }

    // Through EruptService, the path the table page takes: scope conditions apply there
    private List<Map<String, Object>> list(Class<?> clazz) {
        MetaContext.register(new MetaErupt(clazz.getSimpleName()));
        TableQuery query = new TableQuery();
        query.setPageIndex(1);
        query.setPageSize(100);
        return new ArrayList<>(eruptService.getEruptData(model(clazz), query, null).getList());
    }

    @Test
    void tagsFollowTheWorkspace() {
        assertEquals(List.of("VIP", "New"), list(WxTag.class).stream().map(it -> it.get("name")).toList());
        this.workspace(null, null);
        assertThrows(EruptWebApiRuntimeException.class, () -> list(WxTag.class), "no account picked: nothing is guessed");
    }

    @Test
    void addingATagGoesToWeChat() {
        stub.on("/cgi-bin/tags/create", "{\"tag\":{\"id\":101,\"name\":\"Partners\"}}");
        JsonObject tag = new JsonObject();
        tag.addProperty("name", "Partners");
        MetaContext.register(new MetaErupt(WxTag.class.getSimpleName()));
        eruptModifyService.insertEruptData(model(WxTag.class), tag);
        assertTrue(stub.calls("/cgi-bin/tags/create").get(0).body().contains("Partners"));
        // the cached list is dropped, so the next page shows what WeChat now has
        stub.on("/cgi-bin/tags/get", "{\"tags\":[{\"id\":2,\"name\":\"VIP\",\"count\":1},{\"id\":3,\"name\":\"New\",\"count\":0},{\"id\":101,\"name\":\"Partners\",\"count\":0}]}");
        assertEquals(3, list(WxTag.class).size());
    }

    @Test
    void fansAreSyncedAndNarrowedByTheWorkspaceTag() {
        stub.on("/cgi-bin/user/get", "{\"total\":2,\"count\":2,\"data\":{\"openid\":[\"o1\",\"o2\"]},\"next_openid\":\"o2\"}");
        stub.on("/cgi-bin/user/info/batchget", "{\"user_info_list\":[" +
                "{\"subscribe\":1,\"openid\":\"o1\",\"subscribe_time\":1700000000,\"remark\":\"Ann\",\"tagid_list\":[2],\"subscribe_scene\":\"ADD_SCENE_QR_CODE\"}," +
                "{\"subscribe\":1,\"openid\":\"o2\",\"subscribe_time\":1700000001,\"tagid_list\":[]}]}");
        fansSyncHandler.exec(List.of(), null, new String[0]);
        assertTrue(list(WxFans.class).stream().map(it -> it.get("openid")).toList().containsAll(List.of("o1", "o2")));

        this.workspace(account.getId(), 2L);
        List<Map<String, Object>> vip = list(WxFans.class);
        assertEquals(1, vip.size());
        assertEquals("o1", vip.get(0).get("openid"));
        assertEquals("VIP", vip.get(0).get("tags"), "tag ids are shown by name");

        JsonObject form = new JsonObject();
        form.addProperty("account", String.valueOf(account.getId()));
        form.addProperty("tag", "99");
        assertThrows(EruptWebApiRuntimeException.class, () -> new EruptScopeService().accept(xyz.erupt.wechat.core.WxScope.class, form), "only the account's tags");
    }

    @Test
    void menusArePublishedAsDefaultAndConditional() {
        stub.on("/cgi-bin/menu/addconditional", "{\"menuid\":\"208379533\"}");
        WxMenu home = this.menu("Home", WxMenu.CLICK, null, null);
        this.menu("About", WxMenu.VIEW, home, null);
        this.menu("VIP zone", WxMenu.VIEW, null, 2L);
        menuPublishHandler.exec(List.of(), null, new String[]{WxMenuPublishHandler.PUBLISH});
        assertEquals(1, stub.calls("/cgi-bin/menu/delete").size());
        String menu = stub.calls("/cgi-bin/menu/create").get(0).body();
        assertTrue(menu.contains("\"Home\"") && menu.contains("\"About\"") && !menu.contains("VIP zone"), menu);
        String conditional = stub.calls("/cgi-bin/menu/addconditional").get(0).body();
        assertTrue(conditional.contains("VIP zone") && conditional.contains("\"tag_id\":\"2\""), conditional);
    }

    private WxMenu menu(String name, String type, WxMenu parent, Long tag) {
        WxMenu menu = new WxMenu();
        menu.setAccount(account);
        menu.setName(name);
        menu.setType(type);
        menu.setParent(parent);
        menu.setForTag(tag);
        menu.setMenuKey("key-" + name);
        menu.setUrl("https://example.com/" + name);
        this.persist(menu);
        return menu;
    }

    @Test
    void anAutoReplyIsBoundToTheWorkspaceAccount() {
        JsonObject reply = new JsonObject();
        reply.addProperty("type", WxAutoReply.KEYWORD_EXACT);
        reply.addProperty("keyword", "hi");
        reply.addProperty("replyType", WxAutoReply.TEXT);
        reply.addProperty("content", "Hello there");
        reply.addProperty("enable", true);
        MetaContext.register(new MetaErupt(WxAutoReply.class.getSimpleName()));
        Object id = eruptModifyService.insertEruptData(model(WxAutoReply.class), reply);
        assertEquals(account.getId(), eruptDao.find(WxAutoReply.class, id).getAccount().getId(), "the account comes from the workspace, not the form");
    }

    @Test
    void thePortalRepliesAndKeepsFansAndTheInboxCurrent() {
        WxAutoReply welcome = new WxAutoReply();
        welcome.setAccount(account);
        welcome.setType(WxAutoReply.SUBSCRIBE);
        welcome.setReplyType(WxAutoReply.TEXT);
        welcome.setContent("Welcome aboard");
        welcome.setEnable(true);
        this.persist(welcome);

        String timestamp = "1700000000", nonce = "n1";
        String signature = SHA1.gen("portal-token", timestamp, nonce);
        String url = WxPortalController.PATH + "/wx-demo?signature=" + signature + "&timestamp=" + timestamp + "&nonce=" + nonce;
        String xml = "<xml><ToUserName><![CDATA[gh_demo]]></ToUserName><FromUserName><![CDATA[o3]]></FromUserName>"
                + "<CreateTime>1700000000</CreateTime><MsgType><![CDATA[event]]></MsgType><Event><![CDATA[subscribe]]></Event>"
                + "<EventKey><![CDATA[qrscene_spring]]></EventKey></xml>";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        String response = rest.postForEntity(url, new HttpEntity<>(xml, headers), String.class).getBody();
        assertNotNull(response);
        assertTrue(response.contains("Welcome aboard"), response);

        WxFans fan = eruptDao.lambdaQuery(WxFans.class).eq(WxFans::getOpenid, "o3").one();
        assertTrue(fan.getSubscribe());
        assertEquals("qrscene_spring", fan.getSubscribeScene());
        assertEquals(account.getId(), fan.getAccount().getId());
        assertEquals(1, eruptDao.lambdaQuery(WxEventLog.class).eq(WxEventLog::getOpenid, "o3").count());

        assertEquals("Invalid signature", rest.postForEntity(url.replace(signature, "bad"), new HttpEntity<>(xml, headers), String.class).getBody());
    }

}
