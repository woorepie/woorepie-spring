local estateKey = KEYS[1]
local customerKey = KEYS[2]
local estateOrderJson = ARGV[1]
local customerOrderJson = ARGV[2]
local timestamp = tonumber(ARGV[3])

-- JSON 파싱
local estateOrder = cjson.decode(estateOrderJson)
local customerOrder = cjson.decode(customerOrderJson)

-- ZADD 실행 (스코어는 timestamp)
redis.call('ZADD', estateKey, timestamp, estateOrderJson)
redis.call('ZADD', customerKey, timestamp, customerOrderJson)

-- 12시간 TTL(43200초) 설정
redis.call('EXPIRE', estateKey, 43200)
redis.call('EXPIRE', customerKey, 43200)

return 1
