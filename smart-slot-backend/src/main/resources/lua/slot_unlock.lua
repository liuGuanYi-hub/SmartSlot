-- =========================================================================
-- SmartSlot: 安全原子释放时段锁 Lua 脚本
-- 确保仅持有该时段锁的同一用户/订单 或 管理员特权 才能安全释放，防止误删他人锁
-- 返回值: 1-释放成功; 0-验证失败未释放
-- =========================================================================

local lockKey = KEYS[1]
local expectedValue = ARGV[1]

local current = redis.call('get', lockKey)

if current == false or current == nil then
    -- 锁本身已自然过期不存在
    return 1
end

if current == expectedValue or expectedValue == 'FORCE_UNLOCK' then
    redis.call('del', lockKey)
    return 1
else
    return 0
end
