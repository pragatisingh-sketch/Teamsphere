package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    private Long id;
    private String content;
    private String authorName;
    private String authorEmail;
    private String authorRole;
    private LocalDateTime createdAt;
    private Long parentId;
    private List<CommentDTO> replies;
    private List<String> mentions; // List of LDAPs or emails mentioned
}
