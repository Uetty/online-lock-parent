-- 如果不存在则设置该键和值
local nums = redis.call('setnx', KEYS[1], ARGV[1]);
if (nums == 0) then
-- 判断未设置成功
    return 0;
end
redis.call('expire', KEYS[1], ARGV[2]);
return 1;