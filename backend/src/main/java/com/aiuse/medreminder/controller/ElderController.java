package com.aiuse.medreminder.controller;

import com.aiuse.medreminder.common.ApiResponse;
import com.aiuse.medreminder.dto.ElderCreateRequest;
import com.aiuse.medreminder.entity.Elder;
import com.aiuse.medreminder.service.ElderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 老人档案：增、查、删。
 */
@RestController
@RequestMapping("/api/elders")
public class ElderController {

    private final ElderService elderService;

    public ElderController(ElderService elderService) {
        this.elderService = elderService;
    }

    @PostMapping
    public ApiResponse<Elder> create(@Valid @RequestBody ElderCreateRequest request) {
        return ApiResponse.ok(elderService.create(request));
    }

    @GetMapping
    public ApiResponse<List<Elder>> list() {
        return ApiResponse.ok(elderService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<Elder> get(@PathVariable long id) {
        return ApiResponse.ok(elderService.get(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        elderService.delete(id);
        return ApiResponse.ok();
    }
}

