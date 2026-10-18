# erupt-wechat

A WeChat Official Account admin on [erupt](https://www.erupt.xyz). Add the dependency, register an account, and a **WeChat** menu appears with fans, tags, menus, auto replies, materials, drafts, publishing, template and mass messages, customer service and statistics. One account is picked in the page header and every page follows it.

```xml
<dependency>
    <groupId>xyz.erupt</groupId>
    <artifactId>erupt-wechat</artifactId>
    <version>${erupt.version}</version>
</dependency>
```

Needs erupt 2.4.0 or newer. H2 comes along as the zero-config database; set `spring.datasource.*` to use another. Built on [WxJava](https://github.com/binarywang/WxJava).

## What you get

| Menu | Source | Actions |
|---|---|---|
| Official Accounts | database | AppID, AppSecret, token and AES key; sync fans, callback IPs, clear API quota |
| Fans | database, synced | sync, tag / untag, remark, block / unblock, send a customer service message |
| Tags | WeChat | create, rename, delete |
| Blacklist | WeChat | block, unblock |
| Menus | database | two levels, click / URL / mini program; publish as the default menu plus one conditional menu per tag |
| Auto replies | database | keyword (exact or contains), on subscribe, default; text, image or article link |
| Materials | WeChat | image, voice and video; upload, delete |
| Drafts | WeChat | write and edit articles, publish |
| Publications | WeChat | take offline, open / close comments, drill into comments |
| Comments | WeChat | feature, reply, delete |
| Templates | WeChat | add from the library, delete, send with a log; delivery results arrive through the portal |
| Subscribe notifications | WeChat | add from the library, delete, send |
| Mass messages | database | saving sends it; preview, refresh status, cancel; results arrive through the portal |
| Service accounts / sessions / messages | WeChat | manage staff, open and close sessions, read the conversation log |
| QR codes | database | temporary and permanent parameter codes |
| Inbox | database | every message and event the portal received |
| Fan / Article / API statistics | WeChat | the last 7 days, or a searched range up to 30 days |

Pages marked *WeChat* read the API when opened and page in memory; nothing of theirs is stored. Pages marked *database* keep their rows in your database, bound to the account.

## The Workspace

The header shows a **Workspace** picker: an Official Account and, optionally, one of its fan tags.

- **Official Account** is required. Every page runs on it: database pages are filtered by it and new rows land in it; WeChat pages call its API.
- **Tag** narrows the Fans page and is the default audience of a new mass message. Leave it empty for everyone.

The pick travels with every request as plain JSON (`erupt-scope` header) and the server checks it against the options the user is offered, so an account or tag they were not offered is ignored. This is erupt's `@EruptScope` feature; `WxScope` is a plain erupt form. Each browser window keeps its own pick.

## Connecting an account

1. Add the account under **Official Accounts** with the AppID, AppSecret and the token of your choice.
2. Copy its **Server URL** (`{callback-domain}/wechat/portal/{appid}`) into the WeChat console, *Settings and development → Basic configuration → Server configuration*, with the same token. Plain and safe (AES) modes both work.
3. Add your server's outbound IP to the console's IP whitelist.

```yaml
erupt:
  wechat:
    callback-domain: https://admin.example.com   # shown in the Server URL column
    # api-host: https://proxy.example.com        # route API calls through a proxy
```

The access token is kept in memory per account, which suits one instance. A cluster swaps `WxMpDefaultConfigImpl` for WxJava's `WxMpRedisConfigImpl` in `WxMpServices#register`.

For the page signature of `wx.config`, the host application calls `GET /wechat/jssdk/{appid}?url=<page URL>`.

## What WeChat requires

- Mass messages, templates, customer service, comments, subscribe notifications and statistics need a **verified service account**. A sandbox test account has no permanent materials and cannot mass send.
- API calls are counted per day; the WeChat pages cache what they can (tags for 30 seconds) and the lists are capped (1000 materials and publications, 200 drafts).
- A customer service message reaches a fan within 48 hours of the fan's last message.
- Mass messages can be cancelled within half an hour; WeChat still counts them.

## Try it

```bash
mvn spring-boot:run
# http://localhost:8080  erupt / erupt
```

The module runs on its own with an H2 file database under `./h2`; nothing to install. It takes a sandbox test account from https://mp.weixin.qq.com/debug/cgi-bin/sandbox. WeChat has to reach the portal, so pair it with a tunnel such as ngrok and put the tunnel's URL in `callback-domain`.

## Tests

`mvn test` runs the module against a stub of the WeChat API served by the JDK's `HttpServer`, on an in-memory H2. Until erupt 2.4.0 is on Maven Central, install erupt locally first.
