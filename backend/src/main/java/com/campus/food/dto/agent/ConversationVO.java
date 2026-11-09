package com.campus.food.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationVO {

    private Long id;

    private String title;

    private LocalDateTime updatedAt;
}