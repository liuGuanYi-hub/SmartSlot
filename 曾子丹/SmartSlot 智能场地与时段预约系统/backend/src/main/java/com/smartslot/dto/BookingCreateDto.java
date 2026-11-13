package com.smartslot.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建预约表单 DTO
 * 满足基本要求：表单交互与 JSR-303 参数校验
 */
@Data
public class BookingCreateDto {

    @NotNull(message = "请选择场地")
    private Long venueId;

    @NotNull(message = "请选择预约日期")
    @FutureOrPresent(message = "预约日期不能早于今天")
    private LocalDate bookDate;

    @NotBlank(message = "请选择时段")
    @Pattern(regexp = "^\\d{2}:00-\\d{2}:00$", message = "时段格式必须为 HH:00-HH:00")
    private String timeSlot;

    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系人电话格式不合法")
    private String contactPhone;

    private Long userCouponId; // 选用的优惠券ID (可选)
}
