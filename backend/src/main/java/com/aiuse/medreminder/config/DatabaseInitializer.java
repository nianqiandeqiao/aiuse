package com.aiuse.medreminder.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 启动时执行 schema.sql 建表（全部 CREATE TABLE IF NOT EXISTS，可重复执行）。
 */
@Component
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final DataSource dataSource;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        ResourceDatabasePopulator populator =
                new ResourceDatabasePopulator(new ClassPathResource("schema.sql"));
        populator.setSqlScriptEncoding("UTF-8");
        populator.setContinueOnError(false);
        populator.execute(dataSource);
        log.info("schema.sql 执行完成，三张核心表已就绪");

        migrateOwnerColumn(dataSource);
        migratePlanStockColumns(dataSource);
    }

    /**
     * 轻量迁移：给用药计划补上有效期与剩余量字段（老人囤药、忘过期是真实痛点）。
     */
    private void migratePlanStockColumns(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        addColumnIfMissing(jdbcTemplate, "medication_plan", "expire_date",
                "ALTER TABLE medication_plan ADD COLUMN expire_date DATE NULL COMMENT '有效期' AFTER end_date");
        addColumnIfMissing(jdbcTemplate, "medication_plan", "stock_quantity",
                "ALTER TABLE medication_plan ADD COLUMN stock_quantity INT NULL COMMENT '剩余量' AFTER expire_date");
        addColumnIfMissing(jdbcTemplate, "medication_plan", "stock_unit",
                "ALTER TABLE medication_plan ADD COLUMN stock_unit VARCHAR(10) NULL COMMENT '剩余量单位' AFTER stock_quantity");
    }

    private void addColumnIfMissing(JdbcTemplate jdbcTemplate, String table, String column, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, table, column);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
            log.info("已为 {} 表新增 {} 列", table, column);
        }
    }

    /**
     * 轻量迁移：老版本数据库没有 owner_id 列，这里检测后补上，
     * 保证已有环境升级后不用手工改表。
     */
    private void migrateOwnerColumn(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Integer columnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'elder' AND COLUMN_NAME = 'owner_id'",
                Integer.class);
        if (columnCount == null || columnCount == 0) {
            jdbcTemplate.execute(
                    "ALTER TABLE elder ADD COLUMN owner_id BIGINT NULL COMMENT '所属用户ID' AFTER id");
            log.info("已为 elder 表新增 owner_id 列（数据归属用户）");
        }
        Integer indexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'elder' "
                        + "AND INDEX_NAME = 'idx_elder_owner'",
                Integer.class);
        if (indexCount == null || indexCount == 0) {
            jdbcTemplate.execute("CREATE INDEX idx_elder_owner ON elder (owner_id)");
            log.info("已为 elder.owner_id 建索引");
        }
    }
}
