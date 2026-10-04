package com.aiuse.medreminder;

import com.aiuse.medreminder.dto.ElderCreateRequest;
import com.aiuse.medreminder.dto.MedicationPlanCreateRequest;
import com.aiuse.medreminder.entity.Elder;
import com.aiuse.medreminder.entity.MedicationPlan;
import com.aiuse.medreminder.service.ElderService;
import com.aiuse.medreminder.service.InteractionService;
import com.aiuse.medreminder.service.MedicationPlanService;
import com.aiuse.medreminder.security.CurrentUser;
import com.aiuse.medreminder.security.CurrentUserHolder;
import com.aiuse.medreminder.vo.InteractionCheckResult;
import com.aiuse.medreminder.vo.InteractionConflict;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用药安全核心测试：相互作用检查与重复用药检查。
 */
@SpringBootTest
@Transactional
@DisplayName("药物相互作用检查测试")
class InteractionServiceTest {

    private static final long TEST_USER_ID = 900003L;

    @BeforeEach
    void login() {
        CurrentUserHolder.set(new CurrentUser(TEST_USER_ID, "tester", "测试用户"));
    }

    @AfterEach
    void logout() {
        CurrentUserHolder.clear();
    }

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private ElderService elderService;

    @Autowired
    private MedicationPlanService planService;

    @Test
    @DisplayName("华法林 + 阿司匹林 判定为严重冲突，并给出说明书依据")
    void severeConflict() {
        InteractionCheckResult result =
                interactionService.checkDrugNames(List.of("华法林钠片", "阿司匹林肠溶片"));

        assertThat(result.drugCount()).isEqualTo(2);
        assertThat(result.conflictCount()).isGreaterThanOrEqualTo(1);
        assertThat(result.maxSeverity()).isEqualTo(1);
        assertThat(result.maxSeverityLabel()).isEqualTo("严重");
        assertThat(result.conflicts()).extracting(InteractionConflict::title)
                .contains("出血风险显著增加");
        assertThat(result.conflicts().get(0).source()).contains("说明书");
        assertThat(result.conflicts().get(0).advice()).isNotBlank();
    }

    @Test
    @DisplayName("同一药品重复出现时提示重复用药")
    void duplicateDrugDetected() {
        InteractionCheckResult result =
                interactionService.checkDrugNames(List.of("阿司匹林", "阿司匹林肠溶片"));

        assertThat(result.conflicts()).extracting(InteractionConflict::type)
                .contains(InteractionConflict.TYPE_DUPLICATE);
    }

    @Test
    @DisplayName("无冲突的组合返回空列表")
    void noConflict() {
        InteractionCheckResult result =
                interactionService.checkDrugNames(List.of("阿莫西林", "对乙酰氨基酚"));

        assertThat(result.conflictCount()).isZero();
        assertThat(result.maxSeverity()).isNull();
    }

    @Test
    @DisplayName("按老人检查：自动读取进行中计划并识别冲突（含商品名归一）")
    void checkByElder() {
        Elder elder = elderService.create(new ElderCreateRequest(
                "冲突测试老人", 81, "M", "13600000000", "高血压", null));
        planService.create(new MedicationPlanCreateRequest(
                elder.getId(), "阿司匹林肠溶片", "1片", "每日1次", "饭后", "08:00", LocalDate.now(), null));
        planService.create(new MedicationPlanCreateRequest(
                elder.getId(), "华法林钠片", "1片", "每日1次", "饭后", "20:00", LocalDate.now(), null));

        InteractionCheckResult result = interactionService.checkByElder(elder.getId());

        assertThat(result.elderId()).isEqualTo(elder.getId());
        assertThat(result.drugs()).containsExactlyInAnyOrder("阿司匹林", "华法林");
        assertThat(result.maxSeverity()).isEqualTo(1);
    }

    @Test
    @DisplayName("已停用的计划不参与冲突检查")
    void stoppedPlanNotChecked() {
        Elder elder = elderService.create(new ElderCreateRequest(
                "停用计划测试老人", 79, "F", null, null, null));
        MedicationPlan aspirin = planService.create(new MedicationPlanCreateRequest(
                elder.getId(), "阿司匹林", "1片", "每日1次", "饭后", "08:00", LocalDate.now(), null));
        planService.create(new MedicationPlanCreateRequest(
                elder.getId(), "华法林", "1片", "每日1次", "饭后", "20:00", LocalDate.now(), null));
        planService.updateStatus(aspirin.getId(), 0);

        InteractionCheckResult result = interactionService.checkByElder(elder.getId());

        assertThat(result.drugCount()).isEqualTo(1);
        assertThat(result.conflictCount()).isZero();
    }

    @Test
    @DisplayName("规则库不少于 10 条且都带有依据来源")
    void ruleLibrary() {
        var rules = interactionService.listRules();

        assertThat(rules.size()).isGreaterThanOrEqualTo(10);
        assertThat(rules).allSatisfy(rule -> {
            assertThat(rule.getDrugA()).isNotBlank();
            assertThat(rule.getDrugB()).isNotBlank();
            assertThat(rule.getSeverity()).isBetween(1, 3);
            assertThat(rule.getSeverityLabel()).isIn("严重", "警告", "注意");
            assertThat(rule.getAdvice()).isNotBlank();
            assertThat(rule.getSource()).isNotBlank();
        });
    }
}
