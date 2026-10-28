package com.ldd.initialization.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ldd.initialization.dto.TrashListDTO;
import com.ldd.initialization.result.PageResult;
import com.ldd.initialization.result.Result;
import com.ldd.initialization.service.TrashService;
import com.ldd.initialization.utils.UserUtils;
import com.ldd.initialization.vo.TrashItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 回收站Controller
 */
@RestController
@RequestMapping("/api/trash")
@SaCheckLogin
public class TrashController {

    @Autowired
    private TrashService trashService;

    /**
     * 获取回收站列表
     */
    @GetMapping
    public Result<PageResult<TrashItemVO>> getTrashList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer trashType,
            @RequestParam(required = false) String keyword) {
        
        Long userId = UserUtils.getCurrentUserId();
        
        TrashListDTO listDTO = new TrashListDTO();
        listDTO.setPage(page);
        listDTO.setSize(size);
        listDTO.setTrashType(trashType);
        listDTO.setKeyword(keyword);
        
        IPage<TrashItemVO> trashPage = trashService.getTrashList(listDTO, userId);
        
        PageResult<TrashItemVO> pageResult = new PageResult<>();
        pageResult.setData(trashPage.getRecords());
        pageResult.setTotal(trashPage.getTotal());
        
        return Result.success(pageResult);
    }

    /**
     * 获取回收站统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getTrashStats() {
        Long userId = UserUtils.getCurrentUserId();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", trashService.countTrash(userId, null));
        stats.put("noteCount", trashService.countTrash(userId, 1));
        stats.put("fileCount", trashService.countTrash(userId, 2));
        
        return Result.success(stats);
    }

    /**
     * 恢复回收站项目
     */
    @PostMapping("/{trashId}/restore")
    public Result<Map<String, Object>> restoreItem(@PathVariable Long trashId) {
        Long userId = UserUtils.getCurrentUserId();
        Long restoredId = trashService.restoreItem(trashId, userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("restoredId", restoredId);
        result.put("message", "恢复成功");
        
        return Result.success(result);
    }

    /**
     * 彻底删除回收站项目
     */
    @DeleteMapping("/{trashId}")
    public Result<Void> permanentDelete(@PathVariable Long trashId) {
        Long userId = UserUtils.getCurrentUserId();
        trashService.permanentDelete(trashId, userId);
        return Result.success(null);
    }

    /**
     * 批量彻底删除
     */
    @DeleteMapping("/batch")
    public Result<Void> batchPermanentDelete(@RequestBody Long[] trashIds) {
        Long userId = UserUtils.getCurrentUserId();
        trashService.batchPermanentDelete(trashIds, userId);
        return Result.success(null);
    }

    /**
     * 清空回收站
     */
    @DeleteMapping("/empty")
    public Result<Void> emptyTrash() {
        Long userId = UserUtils.getCurrentUserId();
        trashService.emptyTrash(userId);
        return Result.success(null);
    }
}
