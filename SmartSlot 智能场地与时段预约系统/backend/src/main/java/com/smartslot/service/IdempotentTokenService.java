package com.smartslot.service;

/**
 * 幂等 Token 管理服务
 */
public interface IdempotentTokenService {

    /**
     * 生成并在分布式缓存/本地注册一个具有防重效力的一次性幂等 Token
     *
     * @return 64位防伪幂等 Token
     */
    String generateToken();

    /**
     * 原子校验并消费该 Token (只能成功消费一次)
     *
     * @param token 客户端传入的防重 Token
     * @return true-校验成功且首次消费, false-无效或已被重复使用
     */
    boolean verifyAndConsume(String token);
}
