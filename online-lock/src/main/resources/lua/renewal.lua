-- 获取值
local str = redis.call('get', KEYS[1]);
if (str == nil) then
-- 不存在该值
    return 0;
else
    if (str == ARGV[1]) then
        redis.call('expire', KEYS[1], ARGV[2]);
        return 1;
    end
end
return 0;