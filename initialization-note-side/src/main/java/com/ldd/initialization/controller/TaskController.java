package com.ldd.initialization.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.ldd.initialization.dto.TaskCreateDTO;
import com.ldd.initialization.dto.TaskUpdateDTO;
import com.ldd.initialization.result.Result;
import com.ldd.initialization.service.TaskService;
import com.ldd.initialization.utils.UserUtils;
import com.ldd.initialization.vo.TaskVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@SaCheckLogin
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) { this.taskService = taskService; }

    @GetMapping
    public Result<List<TaskVO>> list() { return Result.success(taskService.list(UserUtils.getCurrentUserId())); }

    @PostMapping
    public Result<TaskVO> create(@Valid @RequestBody TaskCreateDTO dto) { return Result.success(taskService.create(dto, UserUtils.getCurrentUserId())); }

    @PutMapping("/{id}")
    public Result<TaskVO> update(@PathVariable Long id, @Valid @RequestBody TaskUpdateDTO dto) { return Result.success(taskService.update(id, dto, UserUtils.getCurrentUserId())); }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { taskService.delete(id, UserUtils.getCurrentUserId()); return Result.success(); }
}
