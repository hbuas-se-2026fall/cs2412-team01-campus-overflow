package com.campusoverflow.identity.application;

import com.campusoverflow.identity.domain.User;
import com.campusoverflow.identity.domain.UserRepository;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用例：本人修改学院 / 班级（PB-41）。
 * <p>只接受当前会话的 {@link Actor}，不接受路径里的 userId，避免越权修改他人资料。
 * 学号、昵称、邮箱是身份凭据，不在本用例范围内；管理员改角色 / 认证走 {@link AdminUserService}。</p>
 */
@Service
public class UserProfileService {

    private final UserRepository users;
    private final UserQueryService query;

    public UserProfileService(UserRepository users, UserQueryService query) {
        this.users = users;
        this.query = query;
    }

    @Transactional
    public MeView updateProfile(Actor actor, UpdateProfileCommand cmd) {
        User user = users.findById(actor.userId())
                .orElseThrow(() -> new NotFoundException("ID-4001", "用户不存在"));

        // null = 保持原值；空串 = 清空（领域方法内部会把空白归一化为 null）
        String college = cmd.college() != null ? cmd.college() : user.college();
        String className = cmd.className() != null ? cmd.className() : user.className();

        user.updateProfile(college, className);
        users.save(user);
        return query.me(user.id());
    }
}
