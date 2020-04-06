-- 获取值
local str = redis.call('get', KEYS[1]);
if (str == nil) then
-- 不存在该值
    return 0;
else
    if (str == ARGV[1]) then
-- 不存在该值
        redis.call('del', KEYS[1]);
        return 1;
    end
end
return 0;