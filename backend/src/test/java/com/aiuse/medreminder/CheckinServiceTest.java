package com.aiuse.medreminder;

import com.aiuse.medreminder.common.BizException;
import com.aiuse.medreminder.dto.CheckinRequest;
import com.aiuse.medreminder.dto.ElderCreateRequest;
import com.aiuse.medreminder.dto.MedicationPlanCreateRequest;
import com.aiuse.medreminder.entity.Elder;
import com.aiuse.medreminder.entity.MedicationLog;
import com.aiuse.medreminder.entity.MedicationPlan;
import com.aiuse.medreminder.service.CheckinService;
import com.aiuse.medreminder.service.ElderService;
import com.aiuse.medreminder.service.MedicationPlanService;
import com.aiuse.medreminder.security.CurrentUser;
import com.aiuse.medreminder.security.CurrentUserHolder;
import com.aiuse.medreminder.vo.CheckinStats;
import com.aiuse.medreminder.vo.TodayDoseSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 业务层（Service）测试：打卡、重复打卡拦截、撤销、今日清单与按时率统计。
 */
@SpringBootTest
@Transactional
@DisplayName("打卡业务测试")
class CheckinServiceTest {

    /** 测试用登录用户（多用户隔离后，业务层要求有登录上下文）。 */
    private static final long TEST_USER_ID = 900002L;

    @BeforeEach
    void login() {
        CurrentUserHolder.set(new CurrentUser(TEST_USER_ID, "tester", "测试用户"));
    }

    @AfterEach
    void logout() {
        CurrentUserHolder.clear();
    }

    @Autowired
    private ElderService elderService;

    @Autowired
    private MedicationPlanService planService;

    @Autowired
    private CheckinService checkinService;

    private Elder createElder() {
        return elderService.create(new ElderCreateRequest(
                "打卡测试老人", 75, "F", "13700000000", "高血压", null));
    }

    private MedicationPlan createPlan(Long elderId) {
        return planService.create(new MedicationPlanCreateRequest(
                elderId, "阿司匹林", "1片", "每日2次", "饭后", "08:00,20:00",
                LocalDate.now(), null));
    }

    @Test
    @DisplayName("打卡成功；同一计划同一天同一时间点重复打卡被拒绝")
    void checkinAndDuplicate() {
        Elder elder = createElder();
        MedicationPlan plan = createPlan(elder.getId());
        LocalDate today = LocalDate.now();

        MedicationLog log = checkinService.checkin(new CheckinRequest(
                elder.getId(), plan.getId(), today, "08:00", "TAKEN", null));

        assertThat(log.getId()).isNotNull();
        assertThat(log.getStatus()).isEqualTo("TAKEN");
        assertThat(log.getTakeTime()).isNotNull();

        assertThatThrownBy(() -> checkinService.checkin(new CheckinRequest(
                elder.getId(), plan.getId(), today, "08:00", "TAKEN", null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已打卡");
    }

    @Test
    @DisplayName("今日服药清单：08:00,20:00 展开成 2 条，打卡后 taken=1")
    void todayList() {
        Elder elder = createElder();
        MedicationPlan plan = createPlan(elder.getId());
        LocalDate today = LocalDate.now();

        TodayDoseSummary before = planService.today(elder.getId(), today);
        assertThat(before.total()).isEqualTo(2);
        assertThat(before.taken()).isZero();
        assertThat(before.items()).extracting(item -> item.remindTime())
                .containsExactly("08:00", "20:00");

        checkinService.checkin(new CheckinRequest(
                elder.getId(), plan.getId(), today, "08:00", "TAKEN", null));

        TodayDoseSummary after = planService.today(elder.getId(), today);
        assertThat(after.taken()).isEqualTo(1);
        assertThat(after.pending()).isEqualTo(1);
    }

    @Test
    @DisplayName("按时率统计：应服2次、已服1次、漏服1次、按时率0.5")
    void stats() {
        Elder elder = createElder();
        MedicationPlan plan = createPlan(elder.getId());
        LocalDate today = LocalDate.now();

        checkinService.checkin(new CheckinRequest(
                elder.getId(), plan.getId(), today, "08:00", "TAKEN", null));

        CheckinStats stats = checkinService.stats(elder.getId(), today, today);
        assertThat(stats.expected()).isEqualTo(2);
        assertThat(stats.taken()).isEqualTo(1);
        assertThat(stats.missed()).isEqualTo(1);
        assertThat(stats.rate()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("撤销打卡后记录消失，重复撤销抛 404 业务异常")
    void undo() {
        Elder elder = createElder();
        MedicationPlan plan = createPlan(elder.getId());
        LocalDate today = LocalDate.now();

        MedicationLog log = checkinService.checkin(new CheckinRequest(
                elder.getId(), plan.getId(), today, "08:00", "TAKEN", null));

        checkinService.undo(log.getId());
        assertThat(planService.today(elder.getId(), today).taken()).isZero();

        assertThatThrownBy(() -> checkinService.undo(log.getId()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("打卡记录不存在");
    }

    @Test
    @DisplayName("A 老人的计划不能给 B 老人打卡")
    void checkinWithWrongElder() {
        Elder elderA = createElder();
        Elder elderB = createElder();
        MedicationPlan planA = createPlan(elderA.getId());

        assertThatThrownBy(() -> checkinService.checkin(new CheckinRequest(
                elderB.getId(), planA.getId(), LocalDate.now(), "08:00", "TAKEN", null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不属于老人");
    }
}
