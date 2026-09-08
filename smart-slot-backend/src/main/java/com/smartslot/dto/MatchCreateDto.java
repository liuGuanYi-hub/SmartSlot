package com.smartslot.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * 发起拼场招募 DTO
 */
@Data
public class MatchCreateDto {

    @NotNull(message = "请选择场地")
    private Long venueId;

    @NotNull(message = "请选择活动日期")
    @FutureOrPresent(message = "活动日期不能早于今天")
    private LocalDate bookDate;

    @NotBlank(message = "请选择时段")
    @Pattern(regexp = "^\\d{2}:00-\\d{2}:00$", message = "时段格式必须为 HH:00-HH:00")
    private String timeSlot;

    @NotBlank(message = "请输入拼场主题")
    @Size(max = 100, message = "主题不能超过 100 字")
    private String title;

    @NotBlank(message = "请选择运动标签")
    private String sportTag;

    @NotNull(message = "请设置目标招募人数")
    @Min(value = 2, message = "招募总人数至少为 2 人")
    @Max(value = 20, message = "招募人数不能超过 20 人")
    private Integer targetMembers;

    private String description;
}
