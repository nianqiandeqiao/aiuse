package com.aiuse.medreminder;

import com.aiuse.medreminder.service.DrugDictionary;
import com.aiuse.medreminder.service.DrugDictionary.DrugHit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 药品别名词典测试：验证 OCR 文本 -> 标准药名的归一能力。
 */
@SpringBootTest
@DisplayName("药品别名词典测试")
class DrugDictionaryTest {

    @Autowired
    private DrugDictionary drugDictionary;

    @Test
    @DisplayName("能从药盒文字中识别出标准药名")
    void matchDrugsFromText() {
        List<DrugHit> hits = drugDictionary.match("阿司匹林肠溶片\n华法林钠片");

        assertThat(hits).extracting(DrugHit::standardName)
                .containsExactlyInAnyOrder("阿司匹林", "华法林");
        assertThat(hits).extracting(DrugHit::category)
                .containsExactlyInAnyOrder("抗血小板", "抗凝血");
    }

    @Test
    @DisplayName("商品名/别名能归一成标准名")
    void standardizeBrandName() {
        assertThat(drugDictionary.standardize("芬必得")).isEqualTo("布洛芬");
        assertThat(drugDictionary.standardize("拜阿司匹灵")).isEqualTo("阿司匹林");
        assertThat(drugDictionary.standardize(" 倍他乐克 ")).isEqualTo("美托洛尔");
    }

    @Test
    @DisplayName("词典里不存在的药名原样返回")
    void unknownDrugKeepsOriginalName() {
        assertThat(drugDictionary.standardize("测试用未知药品"))
                .isEqualTo("测试用未知药品");
    }

    @Test
    @DisplayName("词典规模不少于 30 种常用药")
    void dictionarySize() {
        assertThat(drugDictionary.size()).isGreaterThanOrEqualTo(30);
    }
}

