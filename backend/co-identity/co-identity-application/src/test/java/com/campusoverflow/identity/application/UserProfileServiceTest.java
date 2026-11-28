package com.campusoverflow.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.identity.application.fake.IdentityTestFixtures.InMemoryCourses;
import com.campusoverflow.identity.application.fake.IdentityTestFixtures.InMemoryUsers;
import com.campusoverflow.identity.domain.User;
import com.campusoverflow.identity.domain.UserStatus;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 自助修改学院 / 班级（PB-41）：只改本人、只改这两项。 */
class UserProfileServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-28T03:00:00Z");
    private static final long MING_ID = 1L;

    private final InMemoryUsers users = new InMemoryUsers();
    private final InMemoryCourses courses = new InMemoryCourses();
    private final UserProfileService service = new UserProfileService(users,
            new UserQueryService(users, new CourseService(courses, users, Clock.fixed(NOW, ZoneOffset.UTC))));

    private final Actor ming = new Actor(MING_ID, "2025001", Role.STUDENT);

    @BeforeEach
    void setUp() {
        users.save(User.reconstitute(MING_ID, "2025001", "小明", "ming@campus.edu.cn", "hash", Role.STUDENT,
                UserStatus.ACTIVE, false, "计算机工程学院", "计科2301", NOW, 0));
    }

    @Test
    @DisplayName("只改班级时学院保持不变，且已落库")
    void updatesClassNameOnly() {
        MeView me = service.updateProfile(ming, new UpdateProfileCommand(null, "软工2302"));

        assertThat(me.className()).isEqualTo("软工2302");
        assertThat(me.college()).isEqualTo("计算机工程学院");
        assertThat(users.findById(MING_ID).orElseThrow().className()).isEqualTo("软工2302");
        assertThat(users.saveCount).isEqualTo(2); // setUp 一次 + 本次一次
    }

    @Test
    @DisplayName("空串表示清空该字段")
    void blankClearsField() {
        MeView me = service.updateProfile(ming, new UpdateProfileCommand("", null));

        assertThat(me.college()).isNull();
        assertThat(me.className()).isEqualTo("计科2301");
    }

    @Test
    @DisplayName("两项都传 null 是空操作，返回当前资料")
    void nullMeansUnchanged() {
        MeView me = service.updateProfile(ming, new UpdateProfileCommand(null, null));

        assertThat(me.college()).isEqualTo("计算机工程学院");
        assertThat(me.className()).isEqualTo("计科2301");
    }

    @Test
    @DisplayName("用户不存在时返回 404 语义")
    void rejectsUnknownUser() {
        Actor ghost = new Actor(404L, "ghost", Role.STUDENT);

        assertThatThrownBy(() -> service.updateProfile(ghost, new UpdateProfileCommand("x", "y")))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("用户不存在");
    }
}
