package com.aiuse.medreminder.service;

import com.aiuse.medreminder.mapper.HealthMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查业务层：确认服务存活 + 数据库连通。
 */
@Service
public class HealthService {

    private final HealthMapper healthMapper;

    public HealthService(HealthMapper healthMapper) {
        this.healthMapper = healthMapper;
    }

    public Map<String, Object> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("time", LocalDateTime.now().toString());
        try {
            Integer one = healthMapper.ping();
            data.put("db", one != null && one == 1 ? "UP" : "DOWN");
            data.put("mysqlVersion", healthMapper.mysqlVersion());
            data.put("schema", healthMapper.schema());
        } catch (Exception e) {
            data.put("db", "DOWN");
            data.put("dbError", e.getMessage());
        }
        return data;
    }
}

