package com.aiuse.medreminder.repository;

import com.aiuse.medreminder.dto.ElderCreateRequest;
import com.aiuse.medreminder.entity.Elder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class ElderRepository {

    private static final String COLUMNS =
            "id, name, age, gender, phone, chronic_disease, note, created_at, updated_at, deleted";

    private static final RowMapper<Elder> ROW_MAPPER = (rs, rowNum) -> new Elder(
            rs.getLong("id"),
            rs.getString("name"),
            (Integer) rs.getObject("age"),
            rs.getString("gender"),
            rs.getString("phone"),
            rs.getString("chronic_disease"),
            rs.getString("note"),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at")),
            rs.getBoolean("deleted"));

    private final JdbcTemplate jdbcTemplate;

    public ElderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Elder insert(ElderCreateRequest request) {
        String sql = "INSERT INTO elder (name, age, gender, phone, chronic_disease, note) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, request.name());
            ps.setObject(2, request.age());
            ps.setString(3, request.gender());
            ps.setString(4, request.phone());
            ps.setString(5, request.chronicDisease());
            ps.setString(6, request.note());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("创建老人档案失败：未获取到主键");
        }
        return findById(key.longValue()).orElseThrow();
    }

    public List<Elder> findAll() {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM elder WHERE deleted = 0 ORDER BY id DESC",
                ROW_MAPPER);
    }

    public Optional<Elder> findById(long id) {
        List<Elder> list = jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM elder WHERE id = ? AND deleted = 0",
                ROW_MAPPER, id);
        return list.stream().findFirst();
    }

    public int softDelete(long id) {
        return jdbcTemplate.update("UPDATE elder SET deleted = 1 WHERE id = ? AND deleted = 0", id);
    }

    private static java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}

