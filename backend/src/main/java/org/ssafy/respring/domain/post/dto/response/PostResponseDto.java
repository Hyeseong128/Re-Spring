package org.ssafy.respring.domain.post.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.ssafy.respring.domain.comment.dto.response.CommentResponseDto;
import org.ssafy.respring.domain.image.dto.response.ImageResponseDto;
import org.ssafy.respring.domain.post.vo.Category;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class PostResponseDto {
    private Long id;
    private String title;
    private String content;
    private Category category;
    private UUID userId; // User의 UUID
    private String userName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long likes;
    private List<ImageResponseDto> images;
    private int commentCount;
    private List<CommentResponseDto> comments;
}