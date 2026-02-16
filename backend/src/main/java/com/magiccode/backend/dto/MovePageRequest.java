package com.magiccode.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class MovePageRequest {
    private Long parentId;
    private Integer orderIndex;
}
