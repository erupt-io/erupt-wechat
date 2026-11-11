# AGENTS.md

This file provides guidance to coding agents (Claude Code, Codex, ...) when working with code in this repository.

## Commands

```bash
# Build and run the tests (what CI runs, .github/workflows/maven.yml, JDK 17)
mvn -B verify

# Tests only / a single test method (there is one test class)
mvn test
mvn test -Dtest=WechatTest#tagsFollowTheWorkspace

# Run the module on its own: http://localhost:8080, erupt / erupt
mvn spring-boot:run
```

Java 17, system `mvn` (no wrapper). The parent pom is `spring-boot-starter-parent` (the Spring Boot version
erupt is built on), not `xyz.erupt:erupt`: the erupt parent published to Maven Central is flattened and
carries no dependency management. The erupt version is the `erupt.version` property; erupt 2.4.0 is on
Maven Central, so a fresh clone builds without the erupt checkout.

## What this is

A library, not an application: a WeChat Official Account admin for erupt, built on WxJava
(`weixin-java-mp`). A host application adds the dependency, registers an account and gets a **WeChat**
menu: accounts, fans, tags, blacklist, menus, auto replies, materials, drafts, publications and comments,
templates, subscribe notifications, mass messages, customer service, QR codes, the inbox and statistics.
`erupt-spring-boot-starter` is `provided` (the host brings the erupt stack); `spring-boot-maven-plugin`
only serves `spring-boot:run`, repackaging is off.

Sibling repos: `erupt-miniapp` (Mini Programs) has the same structure. `erupt-wx` is a separate, older
Official Account module on erupt 2.1.0 (package `xyz.erupt.wx`, portal `/wx/portal/{appid}`); several of
its entities map to the same table names as this one (`e_wx_account`, `e_wx_fans`, `e_wx_menu`,
`e_wx_auto_reply`, `e_wx_qr_code`).

## Architecture

- **Registration.** `EruptWechatAutoConfiguration` is listed in
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, implements
  `EruptModule` and builds the menus in `initMenus()` (root `$wechat`; `WxComment` and `WxTemplateLog` are
  hidden menus reached by drilling). Its `@ComponentScan` excludes `@SpringBootApplication` classes,
  because the standalone launcher `EruptWechatApplication` lives in the same package. The launcher sets
  default properties only: an H2 file database under `./h2` and `erupt.wechat.callback-domain`.
- **Workspace.** `core/WxScope` is an `@EruptScope` form (an Official Account, optionally one fan tag)
  picked in the page header and sent with each request in the `erupt-scope` header. `WxContext` resolves
  the account of the current request and its `WxMpService`. The tag narrows the Fans page and is the
  default audience of a new mass message. `WxTagCache` keeps an account's tags for 30 seconds, because
  every request checks its workspace against them.
- **Two kinds of models.**
  - `model/`: JPA entities (`e_wx_*` tables) declared with `@EruptScope(WxScope.class)`; their `account`
    reference is bound to the scope, so lists are filtered and new rows land in the picked account. Side
    effects against WeChat happen in `proxy/` `DataProxy` classes.
  - `live/`: models read from the WeChat API on every open, nothing stored. Each declares
    `@EruptScope(value = WxScope.class, bind = false)` and `@EruptDataProcessor(WxDataService.DATA_PROCESSOR)`
    and is backed by a `WxResource<T>` (`list`, optional `add`/`edit`/`delete`). `data/WxDataService`
    dispatches by model class and filters, sorts and pages in memory.
- **WeChat clients.** `core/WxMpServices` keeps one `WxMpService` per account row, registered at startup
  and whenever an account is saved (`proxy/WxAccountDataProxy`). The access token lives in memory
  (`WxMpDefaultConfigImpl`); a cluster swaps in `WxMpRedisConfigImpl` in `WxMpServices#register`.
- **Endpoints without an erupt session (`portal/`).** `WxPortalController` is WeChat's callback at
  `/wechat/portal/{appid}` (plain and AES modes); `WxReplyService` logs the message or event, keeps fans
  and send results current and answers with the matching auto reply. `WxJsSdkController` serves the
  `wx.config` page signature at `GET /wechat/jssdk/{appid}?url=`.
- **Row operations.** `handler/` holds the `OperationHandler`s (sync fans, publish menus, send templates,
  mass messages, ...), `form/` their input forms.
- **Configuration.** `EruptWechatProperties` (`erupt.wechat.callback-domain`, `erupt.wechat.api-host`).
  AppSecret, token and AES key are fields of `WxAccount`, never configuration: refer to them by field name.
- **i18n.** English keys, translated in `src/main/resources/i18n/erupt-wechat.i18n.csv`; runtime error
  messages use `wechat.*` keys through `I18nTranslate.$translate`.

## Tests

`WechatTest` is a `@SpringBootTest` on a random port and an in-memory H2 (`src/test/resources/application.yml`). `WxApiStub`
stands in for the WeChat API with the JDK's `HttpServer` and is wired through `erupt.wechat.api-host`.

## Behaviour worth knowing (from README.md)

- Mass messages, templates, customer service, comments, subscribe notifications and statistics need a
  verified service account; a sandbox test account has no permanent materials and cannot mass send.
- API calls are counted per day, so live lists are capped (1000 materials and publications, 200 drafts).
- Running locally against WeChat needs a tunnel, with its URL in `erupt.wechat.callback-domain`.
