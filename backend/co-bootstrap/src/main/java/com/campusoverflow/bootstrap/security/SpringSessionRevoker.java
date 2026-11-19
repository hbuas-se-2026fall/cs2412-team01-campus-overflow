package com.campusoverflow.bootstrap.security;

import com.campusoverflow.identity.application.SessionRevoker;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.stereotype.Component;

/**
 * 基于 Spring Session 主体名索引的强制下线（ADR-003）：主体名即登录用户名。
 * 需要 {@code spring.session.redis.repository-type=indexed}；未启用 Spring Session（如集成测试）时为空操作。
 */
@Component
public class SpringSessionRevoker implements SessionRevoker {

    private final ObjectProvider<FindByIndexNameSessionRepository<?>> repository;

    public SpringSessionRevoker(ObjectProvider<FindByIndexNameSessionRepository<?>> repository) {
        this.repository = repository;
    }

    @Override
    public int revokeAll(String username) {
        FindByIndexNameSessionRepository<?> sessions = repository.getIfAvailable();
        if (sessions == null) {
            return 0;
        }
        Set<String> ids = Set.copyOf(sessions.findByPrincipalName(username).keySet());
        ids.forEach(sessions::deleteById);
        return ids.size();
    }
}
