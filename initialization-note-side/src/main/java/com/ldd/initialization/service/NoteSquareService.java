package com.ldd.initialization.service;

import com.ldd.initialization.dto.NoteCommentCreateDTO;
import com.ldd.initialization.dto.NoteSquarePublishDTO;
import com.ldd.initialization.dto.NoteSquareSearchDTO;
import com.ldd.initialization.result.PageResult;
import com.ldd.initialization.vo.NoteCommentVO;
import com.ldd.initialization.vo.NoteSquareDetailVO;
import com.ldd.initialization.vo.NoteSquareVO;

import java.util.List;

/**
 * Note square service.
 */
public interface NoteSquareService {

    NoteSquareDetailVO publishNote(NoteSquarePublishDTO publishDTO, Long userId);

    void unpublishNote(Long noteId, Long userId);

    PageResult<NoteSquareVO> getSquareNotes(NoteSquareSearchDTO searchDTO, Long userId);

    PageResult<NoteSquareVO> getFavoriteNotes(NoteSquareSearchDTO searchDTO, Long userId);

    PageResult<NoteSquareVO> getLikedNotes(NoteSquareSearchDTO searchDTO, Long userId);

    PageResult<NoteSquareVO> getMyNotes(NoteSquareSearchDTO searchDTO, Long userId);

    NoteSquareDetailVO getNoteDetail(Long noteId, Long userId);

    void likeNote(Long noteId, Long userId);

    void unlikeNote(Long noteId, Long userId);

    void favoriteNote(Long noteId, Long userId);

    void unfavoriteNote(Long noteId, Long userId);

    List<NoteCommentVO> getNoteComments(Long noteId, Long userId);

    NoteCommentVO createNoteComment(Long noteId, NoteCommentCreateDTO commentDTO, Long userId);

    void deleteNoteComment(Long commentId, Long userId);
}
