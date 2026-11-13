package com.smartslot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewCreateDto {

    private Long orderId; // 订单ID (可选)

    private Long venueId; // 场地ID

    @NotNull(message = "请给出综合评分")
    @Min(value = 1, message = "评分最低为 1 星")
    @Max(value = 5, message = "评分最高为 5 星")
    private Integer rating;

    private Integer envRating; // 环境评分 1-5

    private Integer facilityRating; // 设施评分 1-5

    private Integer serviceRating; // 服务评分 1-5

    private String tags; // 评价标签(逗号分隔)

    private String images; // 实拍晒图URL(逗号分隔)

    @NotBlank(message = "评价内容不能为空")
    private String content;
}
