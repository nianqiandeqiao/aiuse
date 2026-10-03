package com.aiuse.medreminder;

import com.aiuse.medreminder.entity.Elder;
import com.aiuse.medreminder.mapper.ElderMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 持久层（Mapper）测试：验证 SQL 与字段映射是否正确。
 * 类上加 @Transactional，每个测试方法结束自动回滚，不会污染数据库。
 */
@SpringBootTest
@Transactional
@DisplayName("ElderMapper 持久层测试")
class ElderMapperTest {

    @Autowired
    private ElderMapper elderMapper;

    private Elder newElder(String name, int age) {
        Elder elder = new Elder();
        elder.setName(name);
        elder.setAge(age);
        elder.setGender("M");
        elder.setPhone("13800000000");
        return elder;
    }

    @Test
    @DisplayName("insert 回填自增主键，findById 能查到刚插入的数据")
    void insertAndFindById() {
        Elder elder = newElder("测试老人A", 70);

        int rows = elderMapper.insert(elder);

        assertThat(rows).isEqualTo(1);
        assertThat(elder.getId()).isNotNull();
        Elder found = elderMapper.findById(elder.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("测试老人A");
        assertThat(found.getAge()).isEqualTo(70);
        assertThat(found.getDeleted()).isFalse();
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("update 能修改字段，search 能按姓名模糊命中")
    void updateAndSearch() {
        Elder elder = newElder("测试老人B", 66);
        elderMapper.insert(elder);

        elder.setName("测试老人C");
        elder.setAge(67);
        assertThat(elderMapper.update(elder)).isEqualTo(1);

        Elder updated = elderMapper.findById(elder.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("测试老人C");
        assertThat(updated.getAge()).isEqualTo(67);

        List<Elder> hits = elderMapper.search("测试老人C");
        assertThat(hits).extracting(Elder::getId).contains(elder.getId());
    }

    @Test
    @DisplayName("softDelete 逻辑删除后查不到，重复删除影响行数为 0")
    void softDelete() {
        Elder elder = newElder("测试老人D", 80);
        elderMapper.insert(elder);

        assertThat(elderMapper.softDelete(elder.getId())).isEqualTo(1);
        assertThat(elderMapper.findById(elder.getId())).isEmpty();
        assertThat(elderMapper.softDelete(elder.getId())).isZero();
    }
}

