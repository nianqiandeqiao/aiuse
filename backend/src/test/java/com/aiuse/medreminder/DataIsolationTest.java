package com.aiuse.medreminder;

import com.aiuse.medreminder.common.BizException;
import com.aiuse.medreminder.dto.CheckinRequest;
import com.aiuse.medreminder.dto.ElderCreateRequest;
import com.aiuse.medreminder.dto.ElderUpdateRequest;
import com.aiuse.medreminder.dto.MedicationPlanCreateRequest;
import com.aiuse.medreminder.dto.MedicationPlanUpdateRequest;
import com.aiuse.medreminder.entity.AppUser;
import com.aiuse.medreminder.entity.Elder;
import com.aiuse.medreminder.entity.MedicationLog;
import com.aiuse.medreminder.entity.MedicationPlan;
import com.aiuse.medreminder.security.CurrentUser;
import com.aiuse.medreminder.security.CurrentUserHolder;
import com.aiuse.medreminder.service.CheckinService;
import com.aiuse.medreminder.service.ElderService;
import com.aiuse.medreminder.service.InteractionService;
import com.aiuse.medreminder.service.MedicationPlanService;
import com.aiuse.medreminder.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 数据隔离测试（本次需求核心）：
 * 一个账号只能查看和修改自己的老人档案、用药计划与打卡记录，
 * 访问别人的数据一律返回 404，且不会产生任何修改。
 */
@SpringBootTest
@Transactional
@DisplayName("多用户数据隔离测试")
class DataIsolationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ElderService elderService;

    @Autowired
    private MedicationPlanService planService;

    @Autowired
    private CheckinService checkinService;

    @Autowired
    private InteractionService interactionService;

    @AfterEach
    void clearUser() {
        CurrentUserHolder.clear();
    }

    private void loginAs(AppUser user) {
        CurrentUserHolder.set(new CurrentUser(user.getId(), user.getUsername(), user.getDisplayName()));
    }

    private AppUser register(String prefix) {
        return userService.register(prefix + System.nanoTime() % 1000000, "abc123456", prefix);
    }

    @Test
    @DisplayName("账号 A 看不到账号 B 的老人档案")
    void elderListIsolated() {
        AppUser userA = register("userA");
        AppUser userB = register("userB");

        loginAs(userA);
        Elder elderA = elderService.create(new ElderCreateRequest("A家的老人", 78, "M", null, null, null));

        loginAs(userB);
        Elder elderB = elderService.create(new ElderCreateRequest("B家的老人", 82, "F", null, null, null));

        assertThat(elderService.list(null)).extracting(Elder::getId)
                .contains(elderB.getId())
                .doesNotContain(elderA.getId());
        assertThatThrownBy(() -> elderService.get(elderA.getId()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("老人档案不存在");
    }

    @Test
    @DisplayName("账号 B 不能修改或删除账号 A 的老人档案")
    void elderWriteIsolated() {
        AppUser userA = register("userA");
        AppUser userB = register("userB");

        loginAs(userA);
        Elder elderA = elderService.create(new ElderCreateRequest("A家的老人", 78, "M", null, null, null));

        loginAs(userB);
        assertThatThrownBy(() -> elderService.delete(elderA.getId()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("老人档案不存在");
        assertThatThrownBy(() -> elderService.update(elderA.getId(),
                new ElderUpdateRequest("被篡改", 60, "M", null, null, null)))
                .isInstanceOf(BizException.class);

        loginAs(userA);
        Elder stillThere = elderService.get(elderA.getId());
        assertThat(stillThere.getName()).isEqualTo("A家的老人");
        assertThat(stillThere.getAge()).isEqualTo(78);
    }

    @Test
    @DisplayName("账号 B 不能给账号 A 的老人加药、查计划")
    void planIsolated() {
        AppUser userA = register("userA");
        AppUser userB = register("userB");

        loginAs(userA);
        Elder elderA = elderService.create(new ElderCreateRequest("A家的老人", 78, "M", null, null, null));
        MedicationPlan planA = planService.create(new MedicationPlanCreateRequest(elderA.getId(), "阿司匹林", "1片",
                "每日1次", "饭后", "08:00", LocalDate.now(), null));

        loginAs(userB);
        assertThatThrownBy(() -> planService.listByElder(elderA.getId()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("老人档案不存在");
        assertThatThrownBy(() -> planService.create(new MedicationPlanCreateRequest(elderA.getId(),
                "华法林", "1片", "每日1次", "饭后", "20:00", LocalDate.now(), null)))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> planService.update(planA.getId(), new MedicationPlanUpdateRequest(
                "被篡改的药", "99片", "每日3次", "饭后", "08:00", LocalDate.now(), null)))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> planService.updateStatus(planA.getId(), 0))
                .isInstanceOf(BizException.class);

        loginAs(userA);
        assertThat(planService.get(planA.getId()).getDrugName()).isEqualTo("阿司匹林");
    }

    @Test
    @DisplayName("本人可以正常修改自己的用药计划（避免误伤正常功能）")
    void ownerCanUpdateOwnPlan() {
        AppUser userA = register("userA");
        loginAs(userA);

        Elder elder = elderService.create(new ElderCreateRequest("A家的老人", 78, "M", null, null, null));
        MedicationPlan plan = planService.create(new MedicationPlanCreateRequest(elder.getId(), "阿司匹林",
                "1片", "每日1次", "饭后", "08:00", LocalDate.now(), null));

        MedicationPlan updated = planService.update(plan.getId(), new MedicationPlanUpdateRequest(
                "华法林", "2片", "每日2次", "饭后", "08:00,20:00", LocalDate.now(), null));

        assertThat(updated.getDrugName()).isEqualTo("华法林");
        assertThat(updated.getDosage()).isEqualTo("2片");
        assertThat(updated.getRemindTimes()).isEqualTo("08:00,20:00");
    }

    @Test
    @DisplayName("账号 B 不能打卡、不能撤销 A 的打卡记录")
    void checkinIsolated() {
        AppUser userA = register("userA");
        AppUser userB = register("userB");

        loginAs(userA);
        Elder elderA = elderService.create(new ElderCreateRequest("A家的老人", 78, "M", null, null, null));
        MedicationPlan planA = planService.create(new MedicationPlanCreateRequest(elderA.getId(), "阿司匹林",
                "1片", "每日1次", "饭后", "08:00", LocalDate.now(), null));
        MedicationLog logA = checkinService.checkin(new CheckinRequest(elderA.getId(), planA.getId(),
                LocalDate.now(), "08:00", "TAKEN", null));

        loginAs(userB);
        assertThatThrownBy(() -> checkinService.checkin(new CheckinRequest(elderA.getId(), planA.getId(),
                LocalDate.now(), "08:00", "TAKEN", null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("老人档案不存在");
        assertThatThrownBy(() -> checkinService.undo(logA.getId()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("打卡记录不存在");
        assertThatThrownBy(() -> checkinService.stats(elderA.getId(), LocalDate.now(), LocalDate.now()))
                .isInstanceOf(BizException.class);

        loginAs(userA);
        assertThat(planService.today(elderA.getId(), LocalDate.now()).taken()).isEqualTo(1);
    }

    @Test
    @DisplayName("冲突检查只能检查自己的老人")
    void interactionCheckIsolated() {
        AppUser userA = register("userA");
        AppUser userB = register("userB");

        loginAs(userA);
        Elder elderA = elderService.create(new ElderCreateRequest("A家的老人", 78, "M", null, null, null));

        loginAs(userB);
        assertThatThrownBy(() -> interactionService.checkByElder(elderA.getId()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("老人档案不存在");
    }

    @Test
    @DisplayName("两个账号各自只看到自己的档案")
    void bothUsersSeeOnlyOwnData() {
        AppUser userA = register("userA");
        AppUser userB = register("userB");

        loginAs(userA);
        Elder elderA1 = elderService.create(new ElderCreateRequest("A家老人1", 78, "M", null, null, null));
        Elder elderA2 = elderService.create(new ElderCreateRequest("A家老人2", 79, "F", null, null, null));

        loginAs(userB);
        Elder elderB = elderService.create(new ElderCreateRequest("B家老人", 82, "F", null, null, null));
        assertThat(elderService.list(null)).extracting(Elder::getId)
                .contains(elderB.getId())
                .doesNotContain(elderA1.getId(), elderA2.getId());

        loginAs(userA);
        assertThat(elderService.list(null)).extracting(Elder::getId)
                .contains(elderA1.getId(), elderA2.getId())
                .doesNotContain(elderB.getId());
    }
}
