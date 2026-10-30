package com.ldd.initialization.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ldd.initialization.config.exception.BizException;
import com.ldd.initialization.domain.FileUpInfo;
import com.ldd.initialization.domain.Note;
import com.ldd.initialization.domain.NoteTag;
import com.ldd.initialization.dto.NoteCreateDTO;
import com.ldd.initialization.dto.NoteUpdateDTO;
import com.ldd.initialization.enums.FileSourceTypeEnum;
import com.ldd.initialization.mapper.NoteMapper;
import com.ldd.initialization.mapper.NoteTagMapper;
import com.ldd.initialization.result.BizResponseCode;
import com.ldd.initialization.result.Result;
import com.ldd.initialization.service.FileInfoService;
import com.ldd.initialization.service.NoteService;
import com.ldd.initialization.service.NotebookService;
import com.ldd.initialization.service.TrashService;
import com.ldd.initialization.service.ai.DocumentService;
import com.ldd.initialization.utils.PdfUtils;
import com.ldd.initialization.utils.UserUtils;
import com.ldd.initialization.vo.NoteBriefVO;
import com.ldd.initialization.vo.NoteDailyCountVO;
import com.ldd.initialization.vo.NoteDetailVO;
import com.ldd.initialization.vo.NoteStatsVO;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 笔记Service实现类
 */
@Service
@Slf4j
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements NoteService {

    @Autowired
    private NotebookService notebookService;

    @Autowired
    private NoteTagMapper noteTagMapper;

    @Autowired
    private DocumentService documentService;
    @Autowired
    private FileInfoService fileInfoService;
    
    @Autowired
    private TrashService trashService;
    // Flexmark 配置
    private final MutableDataSet options = new MutableDataSet();
    private final Parser parser = Parser.builder(options).build();
    private final HtmlRenderer renderer = HtmlRenderer.builder(options).build();

    @Override
    @Transactional
    public NoteDetailVO createNote(NoteCreateDTO createDTO, Long userId) {
        // 验证笔记本是否存在且属于用户
        if (!notebookService.isNotebookOwner(createDTO.getNotebookId(), userId)) {
            throw new BizException(BizResponseCode.ERROR_1, "笔记本不存在或无权限");
        }

        // 创建笔记
        Note note = new Note();
        BeanUtils.copyProperties(createDTO, note);
        note.setUserId(userId);
        note.setViewCount(0);
        note.setWordCount(calculateWordCount(createDTO.getContentMd()));

        // 设置默认值
        if (note.getStatus() == null) {
            note.setStatus(1); // 默认已发布
        }
        if (note.getIsPinned() == null) {
            note.setIsPinned(false);
        }

        this.save(note);

        // 保存标签
        saveTags(note.getId(), createDTO.getTags());

        // 返回详情
        return getNoteDetail(note.getId(), userId);
    }

    @Override
    public IPage<NoteBriefVO> getNotesByPage(int page, int size, Long notebookId, Long userId, String keyword) {
        Page<NoteBriefVO> pageObj = new Page<>(page, size);
        IPage<NoteBriefVO> result;
        if (UserUtils.isAdmin()) {
            result = this.baseMapper.selectNotesBriefByPageForAdmin(pageObj, notebookId, keyword);
        } else {
            result = this.baseMapper.selectNotesBriefByPage(pageObj, notebookId, userId, keyword);
        }

        // 为每个笔记添加标签信息
        result.getRecords().forEach(note -> {
            List<String> tags = noteTagMapper.selectTagsByNoteId(note.getId());
            note.setTags(tags);
        });

        return result;
    }

    @Override
    public NoteDetailVO getNoteDetail(Long noteId, Long userId) {
        // 查询笔记详情
        NoteDetailVO noteDetail;
        if (UserUtils.isAdmin()) {
            noteDetail = this.baseMapper.selectNoteDetailByIdForAdmin(noteId);
        } else {
            noteDetail = this.baseMapper.selectNoteDetailById(noteId, userId);
        }
        if (noteDetail == null) {
            throw new BizException(BizResponseCode.ERROR_1, "笔记不存在或无权限");
        }

        // 转换Markdown为HTML
//        if (StringUtils.hasText(noteDetail.getContentMd())) {
//            Node document = parser.parse(noteDetail.getContentMd());
//            String html = renderer.render(document);
//            noteDetail.setContentHtml(html);
//        }

        // 查询标签
        List<String> tags = noteTagMapper.selectTagsByNoteId(noteId);
        noteDetail.setTags(tags);

        // 增加查看次数
        this.baseMapper.incrementViewCount(noteId);

        return noteDetail;
    }

    @Override
    @Transactional
    public NoteDetailVO updateNote(Long noteId, NoteUpdateDTO updateDTO, Long userId) {
        // 验证笔记是否存在且属于用户
        Note note = this.getById(noteId);
        if (note == null || (!UserUtils.isAdmin() && !note.getUserId().equals(userId))) {
            throw new BizException(BizResponseCode.ERROR_1, "笔记不存在或无权限");
        }

        if (updateDTO.getNotebookId() != null
                && !notebookService.isNotebookOwner(updateDTO.getNotebookId(), note.getUserId())) {
            throw new BizException(BizResponseCode.ERROR_1, "目标笔记本不存在或无权限");
        }

        // 更新笔记信息
        BeanUtils.copyProperties(updateDTO, note);
        note.setWordCount(calculateWordCount(updateDTO.getContentMd()));
        note.setUpdateTime(LocalDateTime.now());
        this.saveOrUpdate(note);

        // 更新标签
        noteTagMapper.deleteTagsByNoteId(noteId);
        saveTags(noteId, updateDTO.getTags());

        // 返回详情
        return getNoteDetail(noteId, userId);
    }

    @Override
    @Transactional
    public void deleteNote(Long noteId, Long userId) {
        // 管理员可删除任意笔记；普通用户仅可删除自己的
        Note note = this.getById(noteId);
        if (note == null || (!UserUtils.isAdmin() && !note.getUserId().equals(userId))) {
            throw new BizException(BizResponseCode.ERROR_1, "笔记不存在或无权限");
        }

        // 移入回收站
        trashService.moveNoteToTrash(note, note.getUserId());
        
        // 删除笔记与标签
        this.removeById(noteId);
        noteTagMapper.deleteTagsByNoteId(noteId);
    }

    @Override
    @Transactional
    public void togglePinNote(Long noteId, Long userId) {
        // 验证笔记是否存在且属于用户
        Note note = this.getById(noteId);
        if (note == null || (!UserUtils.isAdmin() && !note.getUserId().equals(userId))) {
            throw new BizException(BizResponseCode.ERROR_1, "笔记不存在或无权限");
        }

        // 切换置顶状态
        note.setIsPinned(!note.getIsPinned());
        this.updateById(note);
    }

    @Override
    public List<String> getUserTags(Long userId) {
        return noteTagMapper.selectDistinctTagsByUserId(userId);
    }

    @Override
    public boolean isNoteOwner(Long noteId, Long userId) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getId, noteId)
                .eq(Note::getUserId, userId);
        return this.count(wrapper) > 0;
    }

    @Override
    public NoteStatsVO getUserNoteStats(Long userId) {
        NoteStatsVO stats = new NoteStatsVO();
        Long total = this.baseMapper.countNotesByUserId(userId);
        stats.setTotal(total != null ? total : 0L);

        NoteDailyCountVO dailyCount = this.baseMapper.selectMostProductiveDay(userId);
        if (dailyCount != null) {
            stats.setMostProductiveDate(dailyCount.getDay());
            stats.setMostProductiveCount(dailyCount.getCount() != null ? dailyCount.getCount() : 0L);
        } else {
            stats.setMostProductiveDate(null);
            stats.setMostProductiveCount(0L);
        }

        return stats;
    }

    @Override
    public Map<String, Object> exportNote(Long noteId, Long userId, String format) {
        Note note = this.getById(noteId);
        if (note == null || (!UserUtils.isAdmin() && !note.getUserId().equals(userId))) {
            throw new BizException(BizResponseCode.ERROR_1, "笔记不存在或无权限");
        }

        String contentMd = StringUtils.hasText(note.getContentMd()) ? note.getContentMd() : "";
        String title = StringUtils.hasText(note.getTitle()) ? note.getTitle() : "note";
        String safeTitle = sanitizeFilename(title);
        String author = resolveAuthorName();
        String formatLower = StringUtils.hasText(format) ? format.trim().toLowerCase() : "pdf";

        Map<String, Object> result = new HashMap<>();
        switch (formatLower) {
            case "pdf" -> {
                byte[] bytes = PdfUtils.convertMarkdownToPdf(title, contentMd, author);
                result.put("bytes", bytes);
                result.put("contentType", "application/pdf");
                result.put("filename", safeTitle + ".pdf");
            }
            case "docx" -> {
                byte[] bytes = convertMarkdownToDocxBytes(title, contentMd, author);
                result.put("bytes", bytes);
                result.put("contentType", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                result.put("filename", safeTitle + ".docx");
            }
            case "xlsx" -> {
                byte[] bytes = convertNoteToExcelBytes(note, contentMd, author);
                result.put("bytes", bytes);
                result.put("contentType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                result.put("filename", safeTitle + ".xlsx");
            }
            case "md" -> {
                byte[] bytes = contentMd.getBytes(StandardCharsets.UTF_8);
                result.put("bytes", bytes);
                result.put("contentType", "text/markdown");
                result.put("filename", safeTitle + ".md");
            }
            default -> throw new BizException(BizResponseCode.ERROR_1, "不支持的导出格式: " + formatLower);
        }

        return result;
    }

    /**
     * 保存标签
     */
    private void saveTags(Long noteId, List<String> tags) {
        if (!CollectionUtils.isEmpty(tags)) {
            for (String tagName : tags) {
                if (StringUtils.hasText(tagName)) {
                    NoteTag noteTag = new NoteTag();
                    noteTag.setNoteId(noteId);
                    noteTag.setTagName(tagName.trim());
                    noteTagMapper.insert(noteTag);
                }
            }
        }
    }

    /**
     * 计算字数
     */
    private Integer calculateWordCount(String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }
        // 简单的字数统计，去除空格和换行
        return content.replaceAll("\\s+", "").length();
    }

    private String sanitizeFilename(String filename) {
        String safe = filename.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return safe.isEmpty() ? "note" : safe;
    }

    private String resolveAuthorName() {
        try {
            String realName = UserUtils.getCurrentUser().getRealName();
            if (StringUtils.hasText(realName)) {
                return realName;
            }
            String username = UserUtils.getCurrentUser().getUsername();
            return StringUtils.hasText(username) ? username : "未知";
        } catch (Exception e) {
            return "未知";
        }
    }

    private byte[] convertMarkdownToDocxBytes(String title, String markdownContent, String author) {
        String htmlContent = PdfUtils.convertMarkdownToHtml(markdownContent);
        String plainText = htmlToPlainText(htmlContent);

        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFParagraph titleParagraph = document.createParagraph();
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setText(StringUtils.hasText(title) ? title : "note");

            XWPFParagraph metaParagraph = document.createParagraph();
            XWPFRun metaRun = metaParagraph.createRun();
            metaRun.setFontSize(11);
            metaRun.setText("作者：" + (StringUtils.hasText(author) ? author : "未知"));
            metaRun.addBreak();
            metaRun.setText("生成时间：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            XWPFParagraph contentParagraph = document.createParagraph();
            XWPFRun contentRun = contentParagraph.createRun();
            contentRun.setFontSize(12);
            String[] lines = plainText.split("\\r?\\n");
            for (int i = 0; i < lines.length; i++) {
                contentRun.setText(lines[i]);
                if (i < lines.length - 1) {
                    contentRun.addBreak();
                }
            }

            document.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Markdown转Word失败: {}", e.getMessage(), e);
            throw new RuntimeException("Word生成失败: " + e.getMessage(), e);
        }
    }

    private String htmlToPlainText(String html) {
        if (!StringUtils.hasText(html)) {
            return "";
        }
        String text = html.replaceAll("(?i)<br\\s*/?>", "\n");
        text = text.replaceAll("(?i)</p>", "\n");
        text = text.replaceAll("<[^>]+>", "");
        text = text.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
        return text.trim();
    }

    private byte[] convertNoteToExcelBytes(Note note, String contentMd, String author) {
        List<String> tags = noteTagMapper.selectTagsByNoteId(note.getId());
        String tagText = tags == null || tags.isEmpty() ? "" : String.join(", ", tags);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Note");
            sheet.setColumnWidth(0, 20 * 256);
            sheet.setColumnWidth(1, 80 * 256);

            CellStyle wrapStyle = workbook.createCellStyle();
            wrapStyle.setWrapText(true);

            int rowIndex = 0;
            rowIndex = writeExcelRow(sheet, rowIndex, "标题", note.getTitle(), wrapStyle);
            rowIndex = writeExcelRow(sheet, rowIndex, "作者", author, wrapStyle);
            rowIndex = writeExcelRow(sheet, rowIndex, "创建时间", note.getCreateTime() != null ? note.getCreateTime().toString() : "", wrapStyle);
            rowIndex = writeExcelRow(sheet, rowIndex, "更新时间", note.getUpdateTime() != null ? note.getUpdateTime().toString() : "", wrapStyle);
            rowIndex = writeExcelRow(sheet, rowIndex, "标签", tagText, wrapStyle);
            writeExcelRow(sheet, rowIndex, "内容", contentMd, wrapStyle);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Excel导出失败: {}", e.getMessage(), e);
            throw new RuntimeException("Excel生成失败: " + e.getMessage(), e);
        }
    }

    private int writeExcelRow(Sheet sheet, int rowIndex, String label, String value, CellStyle style) {
        Row row = sheet.createRow(rowIndex++);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value == null ? "" : value);
        valueCell.setCellStyle(style);
        return rowIndex;
    }

    @Override
    @Transactional
    public Result<java.util.Map<String, Object>> addNoteToPersonalKnowledgeBase(Long noteId, Long userId) {
        java.util.Map<String, Object> result = new HashMap<>();

        try {
            // 1. 验证笔记是否存在且属于用户
            Note note = this.getById(noteId);
            if (note == null || !note.getUserId().equals(userId)) {
                log.warn("笔记 {} 不存在或不属于用户 {}", noteId, userId);
                return Result.error("笔记不存在或无权限"); // 直接返回业务错误
            }

            // 2. 计算当前笔记内容的MD5
            String currentNoteContentMd5 = DigestUtils.md5Hex(note.getContentMd().getBytes(StandardCharsets.UTF_8));

            // 3. 检查是否已经转换过PDF，并获取旧的FileUpInfo
            FileUpInfo existingPdfFileInfo = getExistingNotePdfFileInfo(noteId, userId);

            if (existingPdfFileInfo != null) {
                // 存在旧记录，比较MD5
                if (currentNoteContentMd5.equals(existingPdfFileInfo.getFileMd5())) {
                    log.info("笔记 {} 内容未更改，跳过重新处理。", noteId);
                    return Result.error("笔记内容未更改，无需重新添加到知识库"); // 直接返回业务错误
                } else {
                    // 内容已更改，需要删除旧向量和文件记录
                    log.info("笔记 {} 内容已更改，将删除旧向量和文件记录。", noteId);

                    // 删除旧的向量数据
                    java.util.Map<String, Object> deleteVectorResult = documentService.deleteDocumentVectors(
                            existingPdfFileInfo.getId(),
                            userId, // 个人知识库ID就是用户ID
                            1       // 知识库类型：1-个人知识库
                    );
                    if (!(Boolean) deleteVectorResult.getOrDefault("success", false)) {
                        log.error("删除笔记 {} 旧向量失败: {}", noteId, deleteVectorResult.get("message"));
                        // 抛出异常以回滚事务
                        throw new RuntimeException("删除旧向量失败: " + deleteVectorResult.get("message"));
                    }
                    log.info("笔记 {} 旧向量删除成功。", noteId);

                    // 删除旧的FileUpInfo记录（这也会删除实际文件，如果fileInfoService有此逻辑）
                    boolean removed = fileInfoService.removeById(existingPdfFileInfo.getId());
                    if (!removed) {
                        log.warn("删除笔记 {} 对应的旧文件记录 {} 失败，可能已被手动删除。", noteId, existingPdfFileInfo.getId());
                        // 即使删除失败，也尝试继续，因为向量已删除，但最好记录警告
                    } else {
                        log.info("笔记 {} 对应的旧文件记录 {} 删除成功。", noteId, existingPdfFileInfo.getId());
                    }
                }
            }

            // 4. 获取用户信息作为作者
            String author = UserUtils.getCurrentUser().getRealName();
            // 5. 将笔记内容转换为PDF
            byte[] pdfBytes = PdfUtils.convertMarkdownToPdf(
                    note.getTitle(),
                    note.getContentMd(),
                    author
            );

            // 6. 创建MultipartFile对象
            String filename = note.getTitle() + ".pdf";
            PdfUtils.CustomMultipartFile pdfFile = new PdfUtils.CustomMultipartFile(
                    pdfBytes,
                    filename,
                    "application/pdf"
            );

            // 7. 保存PDF文件到文件系统，并设置来源信息
            FileUpInfo newFileInfo = fileInfoService.uploadFile(pdfFile, userId);

            // 8. 设置笔记来源信息和MD5
            newFileInfo.setSourceType(FileSourceTypeEnum.NOTE_TO_PDF.getValue());
            newFileInfo.setSourceNoteId(noteId);
            newFileInfo.setSourceNoteTitle(note.getTitle());
            newFileInfo.setFileMd5(currentNoteContentMd5); // 存储原始Markdown内容的MD5
            newFileInfo.setUpdateTime(LocalDateTime.now());
            fileInfoService.updateById(newFileInfo); // 更新FileUpInfo记录

            // 9. 将PDF文件添加到个人知识库（向量化）
            java.util.Map<String, Object> processResult = documentService.uploadAndProcessDocument(
                    pdfFile,
                    userId, // 个人知识库ID就是用户ID
                    1, // 1表示个人知识库
                    newFileInfo.getId() // 关联新的文件记录ID
            );

            // 10. 构建返回结果
            result.put("success", true);
            result.put("message", "笔记已成功添加到个人知识库");
            result.put("noteId", noteId);
            result.put("noteTitle", note.getTitle());
            result.put("fileId", newFileInfo.getId());
            result.put("fileName", filename);
            result.put("fileSize", newFileInfo.getFileSize());
            result.put("processResult", processResult);

            log.info("笔记 {} 已成功添加到个人知识库，文件ID: {}", noteId, newFileInfo.getId());

            // 10. 构建成功返回结果的数据部分
            java.util.Map<String, Object> successData = new HashMap<>();
            successData.put("noteId", noteId);
            successData.put("noteTitle", note.getTitle());
            successData.put("fileId", newFileInfo.getId());
            successData.put("fileName", filename);
            successData.put("fileSize", newFileInfo.getFileSize());
            successData.put("processResult", processResult); // 包含向量处理的详细结果
            return Result.success(successData); // 返回成功结果

        } catch (Exception e) {
            log.error("将笔记 {} 添加到个人知识库失败: {}", noteId, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "添加失败: " + e.getMessage());
            // 异常会由 @Transactional 自动回滚
            return Result.error("添加失败: " + e.getMessage());
        }
    }

/**
 * 检查笔记是否已经转换为PDF并添加到知识库，并返回对应的FileUpInfo。
 * 如果存在，则返回FileUpInfo对象；否则返回null。
 */
public FileUpInfo getExistingNotePdfFileInfo(Long noteId, Long userId) {
    LambdaQueryWrapper<FileUpInfo> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(FileUpInfo::getUserId, userId)
            .eq(FileUpInfo::getSourceType, FileSourceTypeEnum.NOTE_TO_PDF.getValue())
            .eq(FileUpInfo::getSourceNoteId, noteId);
    return fileInfoService.getOne(wrapper); // 使用getOne获取单个记录
}

public boolean isNoteConvertedToPdf(Long noteId, Long userId) {
    LambdaQueryWrapper<FileUpInfo> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(FileUpInfo::getUserId, userId)
            .eq(FileUpInfo::getSourceType, FileSourceTypeEnum.NOTE_TO_PDF.getValue())
            .eq(FileUpInfo::getSourceNoteId, noteId);
    return fileInfoService.count(wrapper) > 0;
}



} 
