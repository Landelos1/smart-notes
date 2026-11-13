package com.ldd.initialization.service;

import com.ldd.initialization.domain.Task;
import com.ldd.initialization.dto.TaskCreateDTO;
import com.ldd.initialization.dto.TaskUpdateDTO;
import com.ldd.initialization.vo.TaskVO;

import java.util.List;

public interface TaskService {
    List<TaskVO> list(Long userId);
    TaskVO create(TaskCreateDTO dto, Long userId);
    TaskVO update(Long id, TaskUpdateDTO dto, Long userId);
    void delete(Long id, Long userId);
    TaskVO toVO(Task task);
    TaskVO saveGenerated(Task task);
    List<TaskVO> listGeneratedByNote(Long noteId, Long userId);
    void deleteGeneratedByNote(Long noteId, Long userId);
}
