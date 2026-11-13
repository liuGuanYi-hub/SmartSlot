package com.smartslot.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVo {

    private String token;
    private Long userId;
    private String username;
    private String nickname;
    private String role;
    private String avatar;
    private BigDecimal balance;
}
