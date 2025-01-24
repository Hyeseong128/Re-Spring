package org.ssafy.respring.domain.comment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.ssafy.respring.domain.comment.dto.request.CommentRequestDto;
import org.ssafy.respring.domain.comment.dto.response.CommentResponseDto;
import org.ssafy.respring.domain.comment.service.CommentService;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@Tag(name = "댓글 관리", description = "댓글 생성, 수정, 삭제 및 조회 기능을 제공합니다.")
public class CommentController {
    private final CommentService commentService;

    @Operation(summary = "나의 게시글 댓글 조회", description = "사용자가 작성한 모든 게시글 댓글을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "댓글 목록 조회 성공")
    @GetMapping("/post")
    public ResponseEntity<List<CommentResponseDto>> getMyPostComments(@RequestParam UUID userId) {
        return ResponseEntity.ok(commentService.getMyPostComments(userId));
    }

    @Operation(summary = "게시글 댓글 생성", description = "게시글에 댓글을 작성합니다.")
    @ApiResponse(responseCode = "200", description = "댓글 생성 성공")
    @PostMapping("/post")
    public ResponseEntity<CommentResponseDto> createPostComment(@RequestBody CommentRequestDto dto) {
        return ResponseEntity.ok(commentService.createComment(dto));
    }

    @Operation(summary = "책 댓글 생성", description = "책에 댓글을 작성합니다.")
    @ApiResponse(responseCode = "200", description = "댓글 생성 성공")
    @PostMapping("/book")
    public ResponseEntity<CommentResponseDto> createBookComment(@RequestBody CommentRequestDto dto) {
        return ResponseEntity.ok(commentService.createComment(dto));
    }

    @Operation(summary = "게시글 댓글 수정", description = "특정 게시글 댓글의 내용을 수정합니다.")
    @ApiResponse(responseCode = "200", description = "댓글 수정 성공")
    @PatchMapping("/post/{commentId}")
    public ResponseEntity<CommentResponseDto> updatePostComment(
            @PathVariable Long commentId, @RequestBody String content) {
        return ResponseEntity.ok(commentService.updateComment(commentId, content));
    }

    @Operation(summary = "책 댓글 수정", description = "특정 책 댓글의 내용을 수정합니다.")
    @ApiResponse(responseCode = "200", description = "댓글 수정 성공")
    @PatchMapping("/book/{commentId}")
    public ResponseEntity<CommentResponseDto> updateBookComment(
            @PathVariable Long commentId, @RequestBody String content) {
        return ResponseEntity.ok(commentService.updateComment(commentId, content));
    }

    @Operation(summary = "게시글 댓글 삭제", description = "특정 게시글 댓글을 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "댓글 삭제 성공")
    @DeleteMapping("/post/{commentId}")
    public ResponseEntity<Void> deletePostComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "책 댓글 삭제", description = "특정 책 댓글을 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "댓글 삭제 성공")
    @DeleteMapping("/book/{commentId}")
    public ResponseEntity<Void> deleteBookComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "게시글 댓글 조회", description = "특정 게시글에 작성된 댓글 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "댓글 목록 조회 성공")
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<CommentResponseDto>> getPostComments(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getCommentsByPostId(postId));
    }

    @Operation(summary = "책 댓글 조회", description = "특정 책에 작성된 댓글 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "댓글 목록 조회 성공")
    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<CommentResponseDto>> getBookComments(@PathVariable Long bookId) {
        return ResponseEntity.ok(commentService.getCommentsByBookId(bookId));
    }

    @Operation(summary = "자식 댓글 조회", description = "특정 댓글의 자식 댓글들을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "자식 댓글 조회 성공")
    @GetMapping("/{parentId}/children")
    public ResponseEntity<List<CommentResponseDto>> getChildrenByParentId(@PathVariable Long parentId) {
        List<CommentResponseDto> children = commentService.getChildrenByParentId(parentId);
        return ResponseEntity.ok(children);
    }
}

