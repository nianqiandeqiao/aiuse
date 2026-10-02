package com.aiuse.medreminder.repository;

import com.aiuse.medreminder.dto.MedicationPlanCreateRequest;
import com.aiuse.medreminder.entity.MedicationPlan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class MedicationPlanRepository {

    private static final String COLUMNS = "id, elder_id, drug_name, dosage, frequency, take_relation, "
            + "remind_times, start_date, end_date, status, created_at, updated_at, deleted";

    private static final RowMapper<MedicationPlan> ROW_MAPPER = (rs, rowNum) -> new MedicationPlan(
            rs.getLong("id"),
            rs.getLong("elder_id"),
            rs.getString("drug_name"),
            rs.getString("dosage"),
            rs.getString("frequency"),
            rs.getString("take_relation"),
            rs.getString("remind_times"),
            toLocalDate(rs.getDate("start_date")),
            toLocalDate(rs.getDate("end_date")),
            (Integer) rs.getObject("status"),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at")),
            rs.getBoolean("deleted"));

    private final JdbcTemplate jdbcTemplate;

    public MedicationPlanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public MedicationPlan insert(MedicationPlanCreateRequest request) {
        String sql = "INSERT INTO medication_plan (elder_id, drug_name, dosage, frequency, take_relation, "
                + "remind_times, start_date, end_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, request.elderId());
            ps.setString(2, request.drugName());
            ps.setString(3, request.dosage());
            ps.setString(4, request.frequency());
            ps.setString(5, request.takeRelation());
            ps.setString(6, request.remindTimes());
            ps.setDate(7, request.startDate() == null ? null : Date.valueOf(request.startDate()));
            ps.setDate(8, request.endDate() == null ? null : Date.valueOf(request.endDate()));
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("创建用药计划失败：未获取到主键");
        }
        return findById(key.longValue()).orElseThrow();
    }

    public Optional<MedicationPlan> findById(long id) {
        List<MedicationPlan> list = jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM medication_plan WHERE id = ? AND deleted = 0",
                ROW_MAPPER, id);
        return list.stream().findFirst();
    }

    public List<MedicationPlan> findByElderId(long elderId) {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM medication_plan WHERE elder_id = ? AND deleted = 0 "
                        + "ORDER BY id DESC",
                ROW_MAPPER, elderId);
    }

    private static java.time.LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    private static java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}

