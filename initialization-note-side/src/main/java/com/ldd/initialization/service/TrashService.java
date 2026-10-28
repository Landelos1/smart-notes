package com.ldd.initialization.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ldd.initialization.domain.FileUpInfo;
import com.ldd.initialization.domain.Note;
import com.ldd.initialization.domain.Trash;
import com.ldd.initialization.dto.TrashListDTO;
import com.ldd.initialization.vo.TrashItemVO;

/**
 * 回收站Service接口
 */
public interface TrashService extends IService<Trash> {

    /**
     * 分页查询回收站列表
     * @param listDTO 查询参数
     * @param userId 用户ID
     * @return 分页结果
     */
    IPage<TrashItemVO> getTrashList(TrashListDTO listDTO, Long userId);

    /**
     * 将笔记移入回收站
     * @param note 笔记对象
     * @param userId 用户ID
     */
    void moveNoteToTrash(Note note, Long userId);

    /**
     * 将文件移入回收站
     * @param fileInfo 文件对象
     * @param userId 用户ID
     */
    void moveFileToTrash(FileUpInfo fileInfo, Long userId);

    /**
     * 从回收站恢复笔记
     * @param trashId 回收站记录ID
     * @param userId 用户ID
     * @return 恢复后的笔记ID
     */
    Long restoreNote(Long trashId, Long userId);

    /**
     * 从回收站恢复文件
     * @param trashId 回收站记录ID
     * @param userId 用户ID
     * @return 恢复后的文件ID
     */
    Long restoreFile(Long trashId, Long userId);

    /**
     * 从回收站恢复项目（自动判断类型）
     * @param trashId 回收站记录ID
     * @param userId 用户ID
     * @return 恢复结果
     */
    Long restoreItem(Long trashId, Long userId);

    /**
     * 彻底删除回收站项目
     * @param trashId 回收站记录ID
     * @param userId 用户ID
     */
    void permanentDelete(Long trashId, Long userId);

    /**
     * 批量彻底删除
     * @param trashIds 回收站记录ID列表
     * @param userId 用户ID
     */
    void batchPermanentDelete(Long[] trashIds, Long userId);

    /**
     * 清空回收站
     * @param userId 用户ID
     */
    void emptyTrash(Long userId);

    /**
     * 清理过期的回收站数据
     * @return 清理的记录数
     */
    int cleanExpiredTrash();

    /**
     * 统计回收站数量
     * @param userId 用户ID
     * @param trashType 类型（可选）
     * @return 记录数
     */
    Long countTrash(Long userId, Integer trashType);
}
