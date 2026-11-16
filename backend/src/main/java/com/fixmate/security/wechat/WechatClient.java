package com.fixmate.security.wechat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixmate.common.api.ErrorCode;
import com.fixmate.config.WechatProperties;
import com.fixmate.exception.BusinessException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class WechatClient {

    private static final Logger log = LoggerFactory.getLogger(WechatClient.class);

    private static final String HOST = "https://api.weixin.qq.com";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WechatProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private String apiToken;
    private Instant apiTokenExpiresAt = Instant.EPOCH;

    public WechatClient(WechatProperties properties, RestClient.Builder builder, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(TIMEOUT);
        this.restClient = builder.requestFactory(factory).baseUrl(HOST).build();
    }

    /**
     * 服务端换取 openid。session_key 不返回、不落库、不写普通日志。
     *
     * 微信该接口返回 Content-Type: text/plain，因此先取字符串再用 Jackson 解析；
     * 直接按对象反序列化会因为媒体类型不匹配而失败。
     */
    public WechatSession code2Session(String code) {
        if (!properties.isConfigured()) {
            throw new BusinessException(ErrorCode.Codes.WECHAT_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE,
                    "微信登录未配置");
        }
        String body;
        try {
            body = restClient.get()
                    .uri(uri -> uri.path("/sns/jscode2session")
                            .queryParam("appid", properties.getAppId())
                            .queryParam("secret", properties.getAppSecret())
                            .queryParam("js_code", code)
                            .queryParam("grant_type", "authorization_code")
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            // 不打印 URL：查询串里带 AppSecret
            log.warn("wechat code2session http status {}", ex.getStatusCode().value());
            throw unavailable();
        } catch (RuntimeException ex) {
            log.warn("wechat code2session failed: {}", ex.getClass().getSimpleName());
            throw unavailable();
        }

        SessionResponse response;
        try {
            response = objectMapper.readValue(body, SessionResponse.class);
        } catch (Exception ex) {
            log.warn("wechat code2session response unparsable");
            throw unavailable();
        }

        if (response.errcode() != null && response.errcode() != 0) {
            log.warn("wechat code2session errcode {}", response.errcode());
            if (isClientFault(response.errcode())) {
                throw BusinessException.validation("微信登录失败：code 无效或已使用");
            }
            throw unavailable();
        }
        if (response.openid() == null || response.openid().isBlank()) {
            log.warn("wechat code2session returned empty openid");
            throw unavailable();
        }
        return new WechatSession(response.openid(), response.unionid());
    }

    public String generateMiniProgramLink(String path, String query, Instant expiresAt) {
        if (!properties.isConfigured()) {
            throw invitationLinkUnavailable("后端未配置微信 AppID 或 AppSecret，请核对启动配置后重启后端");
        }
        JsonNode response = postWechat("/wxa/generate_urllink", accessToken(), Map.of(
                "path", path, "query", query, "env_version", "release",
                "expire_type", 0, "expire_time", expiresAt.getEpochSecond()));
        // A mini program without URL Link permission can still accept an invitation code.
        if (response.path("errcode").asInt() == 85407) return null;
        String url = response.path("url_link").asText();
        if (!url.startsWith("https://")) {
            throw invitationLinkUnavailable("微信未返回可用的邀请链接，请稍后重新签发");
        }
        return url;
    }

    private synchronized String accessToken() {
        if (Instant.now().isBefore(apiTokenExpiresAt)) {
            return apiToken;
        }
        JsonNode response = postWechat("/cgi-bin/stable_token", null, Map.of(
                "grant_type", "client_credential", "appid", properties.getAppId(),
                "secret", properties.getAppSecret(), "force_refresh", false));
        String token = response.path("access_token").asText();
        int expiresIn = response.path("expires_in").asInt();
        if (token.isBlank() || expiresIn <= 0) {
            throw invitationLinkUnavailable("微信未返回有效的接口凭证，请核对小程序配置");
        }
        apiToken = token;
        apiTokenExpiresAt = Instant.now().plusSeconds(Math.max(0, expiresIn - 60));
        return token;
    }

    private JsonNode postWechat(String path, String token, Map<String, Object> payload) {
        JsonNode response;
        try {
            byte[] bodyBytes = objectMapper.writeValueAsBytes(payload);
            String body = restClient.post().uri(uri -> {
                uri.path(path);
                if (token != null) uri.queryParam("access_token", token);
                return uri.build();
            }).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    // The WeChat endpoint rejects chunked JSON requests with HTTP 412.
                    .contentLength(bodyBytes.length).body(bodyBytes).retrieve().body(String.class);
            response = objectMapper.readTree(body);
        } catch (RestClientResponseException ex) {
            log.warn("wechat invitation API {} returned HTTP {}", path, ex.getStatusCode().value());
            String action = token == null ? "获取微信接口凭证" : "生成微信邀请链接";
            throw invitationLinkUnavailable(action + "失败（微信 HTTP " + ex.getStatusCode().value() + "），请稍后重试");
        } catch (Exception ex) {
            log.warn("wechat invitation API {} failed: {}", path, ex.getClass().getSimpleName());
            String action = token == null ? "获取微信接口凭证" : "生成微信邀请链接";
            throw invitationLinkUnavailable(action + "请求失败，请检查后端到微信接口的网络连接后重试");
        }
        if (response == null || !response.isObject()) {
            log.warn("wechat invitation API {} returned an invalid response", path);
            throw invitationLinkUnavailable("微信返回了无法识别的响应，请稍后重新签发");
        }
        int code = response.path("errcode").asInt();
        if (code != 0) {
            log.warn("wechat invitation API {} returned error {}", path, code);
            if (token == null || code != 85407) {
                throw invitationLinkUnavailable(invitationFailureMessage(token == null, code));
            }
        }
        return response;
    }

    static String invitationFailureMessage(boolean requestingToken, int code) {
        String message = requestingToken ? switch (code) {
            case 40001, 40013, 40125 -> "微信拒绝接口凭证请求，请核对后端配置的小程序 AppID 和 AppSecret";
            case 40164 -> "后端出口 IP 未获微信授权，请到小程序后台核对 IP 白名单";
            default -> "获取微信接口凭证失败，请核对微信接口状态和小程序配置";
        } : switch (code) {
            case 85079 -> "该小程序还没有线上正式版，请先发布包含维修身份申请页的版本";
            case 40002 -> "当前小程序没有生成 URL Link 的权限，请核对微信后台的主体和接口权限";
            case 40165 -> "微信找不到维修身份申请页，请核对已发布版本是否包含该页面";
            case 40001, 40014, 42001 -> "微信拒绝了接口凭证，请稍后重试";
            case 44990, 44993 -> "微信链接接口已达到调用限额，请稍后重试";
            default -> "微信生成邀请链接失败，请根据错误码核对接口状态";
        };
        return message + "（微信 " + code + "）";
    }

    private BusinessException invitationLinkUnavailable(String message) {
        return new BusinessException(ErrorCode.Codes.WECHAT_UNAVAILABLE, HttpStatus.UNPROCESSABLE_ENTITY,
                message);
    }

    /** 用户侧问题（code 无效/已使用/风控）返回 400；其余（appid、secret、系统繁忙）归为服务不可用。 */
    static boolean isClientFault(Integer errcode) {
        if (errcode == null) {
            return false;
        }
        return errcode == 40029 || errcode == 40163 || errcode == 40226 || errcode == 45011;
    }

    private BusinessException unavailable() {
        return new BusinessException(ErrorCode.Codes.WECHAT_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE,
                "微信接口不可用");
    }

    /**
     * 仅保留 openid/unionid；session_key 不进入任何上层对象。
     */
    public record WechatSession(String openid, String unionid) {
    }

    private record SessionResponse(String openid, String unionid, String session_key, Integer errcode, String errmsg) {
    }
}
