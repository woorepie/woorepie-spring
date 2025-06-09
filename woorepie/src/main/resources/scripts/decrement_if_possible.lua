local current = redis.call("get", KEYS[1])
if not current then return -1 end

local newVal = tonumber(current) - tonumber(ARGV[1])
if newVal < 0 then
    redis.call("set", KEYS[1], 0)
    return 0
else
    redis.call("set", KEYS[1], newVal)
    return newVal
end
