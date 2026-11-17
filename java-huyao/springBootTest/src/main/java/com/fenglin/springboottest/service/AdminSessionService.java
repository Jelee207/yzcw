package com.fenglin.springboottest.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 后台登录会话：管理员登录后签发一枚 token，后续后台接口凭此放行。
 * 纯内存保存，服务重启即失效，需重新登录；够本项目后台使用。
 */
@Service
public class AdminSessionService {

    /** token -> 登录道号 */
    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    /** 签发 token */
    public String issue(String username) {
        String token = "adm_" + UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, username);
        return token;
    }

    /** 校验 token 是否有效，返回登录道号；无效返回 null */
    public String verify(String token) {
        if (token == null || token.isBlank()) return null;
        return sessions.get(token);
    }

    /** 退出登录 */
    public void invalidate(String token) {
        if (token != null) sessions.remove(token);
    }
}
