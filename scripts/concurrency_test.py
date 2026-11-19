"""
并发预约测试：N 个线程同时抢同一场地同一时间段。
预期：仅 1 个请求成功，其余返回 409。
用法: python scripts/concurrency_test.py [threads]
"""
import datetime
import json
import sys
import threading
import urllib.error
import urllib.parse
import urllib.request

BASE = "http://127.0.0.1:8080/api"
THREADS = int(sys.argv[1]) if len(sys.argv) > 1 else 6


def call(method, path, token=None, body=None, params=None):
    url = BASE + path
    if params:
        url += "?" + urllib.parse.urlencode(params)
    data = json.dumps(body).encode("utf-8") if body is not None else None
    request = urllib.request.Request(url, data=data, method=method)
    request.add_header("Content-Type", "application/json")
    if token:
        request.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return response.status, json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        return exc.code, json.loads(exc.read().decode("utf-8"))


status, res = call("POST", "/auth/login", body={"username": "member02", "password": "123456"})
token = res["data"]["token"]

status, res = call("GET", "/system/clock")
today = datetime.date.fromisoformat(res["data"]["businessDate"])

slot = None
for offset in range(2, 22):
    day = today + datetime.timedelta(days=offset)
    for hour in ["08:00", "10:00", "12:00", "14:00", "16:00", "18:00", "20:00"]:
        start = f"{day.isoformat()} {hour}:00"
        end_dt = datetime.datetime.strptime(start, "%Y-%m-%d %H:%M:%S") + datetime.timedelta(hours=1)
        end = end_dt.strftime("%Y-%m-%d %H:%M:%S")
        st, rs = call("GET", "/courts/availability", params={"startAt": start, "endAt": end})
        if st == 200 and rs.get("data"):
            slot = (rs["data"][0]["courtId"], start, end)
            break
    if slot:
        break

if not slot:
    print("未找到可测试的空闲时段")
    sys.exit(1)

court_id, start, end = slot
print(f"测试目标: court={court_id} {start} ~ {end}, 并发线程={THREADS}")

barrier = threading.Barrier(THREADS)
results = []
lock = threading.Lock()


def worker(index):
    body = {
        "courtId": court_id,
        "startAt": start,
        "endAt": end,
        "requestId": f"concurrent-{int(datetime.datetime.now().timestamp() * 1000)}-{index}",
        "payNow": True,
    }
    barrier.wait()
    status, res = call("POST", "/bookings", token, body=body)
    with lock:
        results.append((status, res.get("message"), (res.get("data") or {}).get("bookingId")))


threads = [threading.Thread(target=worker, args=(i,)) for i in range(THREADS)]
for t in threads:
    t.start()
for t in threads:
    t.join()

success = [r for r in results if r[0] == 200]
conflict = [r for r in results if r[0] == 409]
print(f"成功: {len(success)}  冲突: {len(conflict)}  其他: {len(results) - len(success) - len(conflict)}")
for status, message, booking_id in results:
    print(f"  HTTP {status} bookingId={booking_id} {message}")

# 清理：取消成功订单，释放时间段
for _, _, booking_id in success:
    if booking_id:
        call("PATCH", f"/bookings/{booking_id}/cancel", token, body={"reason": "并发测试清理"})

passed = len(success) == 1 and len(conflict) == THREADS - 1
print("RESULT:", "PASS" if passed else "FAIL")
sys.exit(0 if passed else 1)
