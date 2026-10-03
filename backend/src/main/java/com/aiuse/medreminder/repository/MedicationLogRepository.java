package com.aiuse.medreminder.repository;

import com.aiuse.medreminder.dto.CheckinRequest;
import com.aiuse.medreminder.entity.MedicationLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class MedicationLogRepository {

    private static final String COLUMNS =
            "id, elder_id, plan_id, take_date, remind_time, take_time, status, note, created_at";

    private static final RowMapper<MedicationLog> ROW_MAPPER = (rs, rowNum) -> new MedicationLog(
            rs.getLong("id"),
            rs.getLong("elder_id"),
            rs.getLong("plan_id"),
            toLocalDate(rs.getDate("take_date")),
            rs.getString("remind_time"),
            toLocalDateTime(rs.getTimestamp("take_time")),
            rs.getString("status"),
            rs.getString("note"),
            toLocalDateTime(rs.getTimestamp("created_at")));

    private final JdbcTemplate jdbcTemplate;

    public MedicationLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public MedicationLog insert(long elderId, long planId, LocalDate takeDate, String remindTime,
                                String status, String note) {
        String sql = "INSERT INTO medication_log (elder_id, plan_id, take_date, remind_time, take_time, "
                + "status, note) VALUES (?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, elderId);
            ps.setLong(2, planId);
            ps.setDate(3, Date.valueOf(takeDate));
            ps.setString(4, remindTime);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(6, status);
            ps.setString(7, note);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("服药打卡失败：未获取到主键");
        }
        return findById(key.longValue()).orElseThrow();
    }

    public Optional<MedicationLog> findById(long id) {
        List<MedicationLog> list = jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM medication_log WHERE id = ?", ROW_MAPPER, id);
        return list.stream().findFirst();
    }

    public List<MedicationLog> findByElderIdAndDate(long elderId, LocalDate takeDate) {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM medication_log WHERE elder_id = ? AND take_date = ? "
                        + "ORDER BY remind_time ASC, id ASC",
                ROW_MAPPER, elderId, Date.valueOf(takeDate));
    }

    public List<MedicationLog> findByElderId(long elderId) {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM medication_log WHERE elder_id = ? "
                        + "ORDER BY take_date DESC, remind_time ASC, id ASC",
                ROW_MAPPER, elderId);
    }

    public Optional<MedicationLog> findByPlanIdAndDateAndRemindTime(long planId, LocalDate takeDate,
                                                                    String remindTime) {
        List<MedicationLog> list = jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM medication_log WHERE plan_id = ? AND take_date = ? "
                        + "AND remind_time = ?",
                ROW_MAPPER, planId, Date.valueOf(takeDate), remindTime);
        return list.stream().findFirst();
    }

    public List<MedicationLog> findByElderIdAndDateRange(long elderId, LocalDate from, LocalDate to) {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM medication_log WHERE elder_id = ? AND take_date BETWEEN ? AND ? "
                        + "ORDER BY take_date DESC, remind_time ASC, id ASC",
                ROW_MAPPER, elderId, Date.valueOf(from), Date.valueOf(to));
    }

    public int countByStatusInRange(long elderId, LocalDate from, LocalDate to, String status) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM medication_log WHERE elder_id = ? AND take_date BETWEEN ? AND ? "
                        + "AND status = ?",
                Integer.class, elderId, Date.valueOf(from), Date.valueOf(to), status);
        return count == null ? 0 : count;
    }

    public int deleteById(long id) {
        return jdbcTemplate.update("DELETE FROM medication_log WHERE id = ?", id);
    }

    private static java.time.LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    private static java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
