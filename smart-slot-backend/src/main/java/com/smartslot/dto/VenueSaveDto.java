package com.smartslot.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 场地编辑与新增表单 DTO
 * 满足基本要求：复杂表单交互
 */
@Data
public class VenueSaveDto {

    private Long id;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotBlank(message = "场地名称不能为空")
    private String name;

    @NotNull(message = "容纳人数不能为空")
    @Min(value = 1, message = "容纳人数至少为 1")
    private Integer capacity;

    @NotNull(message = "每小时单价不能为空")
    @DecimalMin(value = "0.01", message = "单价必须大于 0")
    private BigDecimal pricePerHour;

    private String coverImage;

    private String facilities;

    private String description;

    @NotBlank(message = "每日开放时间不能为空")
    private String openTime;

    @NotBlank(message = "每日闭馆时间不能为空")
    private String closeTime;

    private Integer status; // 1-开放, 0-维护
}
