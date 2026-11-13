package com.smartslot.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 拼场大厅检索 DTO
 */
@Data
public class MatchQueryDto {

    private String categoryName;

    private Integer status; // 0-招募中, 1-拼场成功, 2-已核销, 3-已取消

    private LocalDate bookDate;

    private String keyword;

    private Integer pageNum = 1;

    private Integer pageSize = 12;
}
