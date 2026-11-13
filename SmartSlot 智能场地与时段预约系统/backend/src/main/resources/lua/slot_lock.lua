-- =========================================================================
-- SmartSlot: 高并发原子时段锁定与抢占 Lua 脚本
-- 保证“查询状态 + 占用锁定 + 绑定用户凭证 + 设置过期 TTL”在单个原子操作内完成
-- 返回值: 1-加锁成功; 0-已被他人锁定占用失败
-- =========================================================================

local lockKey = KEYS[1]
local lockValue = ARGV[1]
local ttlSeconds = tonumber(ARGV[2])

-- 检查当前时段是否已被锁定
local current = redis.call('get', lockKey)

if current == false or current == nil then
    -- 空闲，执行原子写入并设置 TTL (秒)
    redis.call('set', lockKey, lockValue, 'EX', ttlSeconds)
    return 1
elseif current == lockValue then
    -- 同一请求/订单重入锁，刷新 TTL
    redis.call('expire', lockKey, ttlSeconds)
    return 1
else
    -- 已被他人抢占锁定
    return 0
end
