package com.campusoverflow.qa.interfaces;

import java.util.Optional;

import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 接口层测试用的 Actor 注入替身。
 *
 * <p>生产环境由 co-bootstrap 的 {@code ActorArgumentResolver} 从安全上下文解析；接口层模块
 * 不依赖 bootstrap，因此测试里用这个固定身份的解析器替换。</p>
 */
final class TestActorResolver implements HandlerMethodArgumentResolver {

    static final Actor ACTOR = new Actor(7L, "张博", Role.STUDENT);

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        Class<?> type = parameter.getParameterType();
        return type == Actor.class
                || (type == Optional.class && parameter.getNestedParameterType() == Actor.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        return parameter.getParameterType() == Optional.class ? Optional.of(ACTOR) : ACTOR;
    }
}
