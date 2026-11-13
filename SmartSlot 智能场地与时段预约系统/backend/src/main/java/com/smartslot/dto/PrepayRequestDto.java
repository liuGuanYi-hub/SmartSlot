package com.smartslot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PrepayRequestDto {

    @NotBlank(message = "订单编号不能为空")
    private String orderNo;

    /**
     * 支付渠道: ALIPAY, WECHAT, BALANCE
     */
    @NotBlank(message = "支付渠道不能为空")
    private String channel;
}
