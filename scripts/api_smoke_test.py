"""
羽毛球场地管理系统 - REST API 冒烟测试
仅使用 Python 标准库，覆盖认证、权限、预约并发拦截、取消释放、
签到/结束使用、会员调级、商品销售与统计接口。

脚本会自动寻找未来空闲时段，可在同一数据库上重复执行。
用法: python scripts/api_smoke_test.py [base_url]
"""
import datetime
import json
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

BASE = (sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8080") + "/api"
PASSED = 0
FAILED = 0


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


def check(name, condition, detail=""):
    global PASSED, FAILED
    if condition:
        PASSED += 1
        print(f"[PASS] {name} {detail}")
    else:
        FAILED += 1
        print(f"[FAIL] {name} {detail}")


def login(username, password):
    status, res = call("POST", "/auth/login", body={"username": username, "password": password})
    check(f"登录 {username}", status == 200 and res.get("code") == 200,
          f"status={status} msg={res.get('message')}")
    return res.get("data", {}).get("token")


admin = login("admin", "admin123")
member = login("member02", "123456")

status, clock_res = call("GET", "/system/clock")
original_now = (clock_res.get("data") or {}).get("effectiveNow") or "2026-11-19 10:00:00"
base_date = datetime.date.fromisoformat(original_now[:10])

# 自动寻找一个未来 2-21 天、1 小时区间内至少 5 块场地空闲的时段
chosen = None
for offset in range(2, 22):
    day = base_date + datetime.timedelta(days=offset)
    for hour in ["09:00", "11:00", "13:00", "15:00", "17:00", "19:00"]:
        start_text = f"{day.isoformat()} {hour}:00"
        end_dt = datetime.datetime.strptime(start_text, "%Y-%m-%d %H:%M:%S") + datetime.timedelta(hours=1)
        end_text = end_dt.strftime("%Y-%m-%d %H:%M:%S")
        status, res = call("GET", "/courts/availability",
                           params={"startAt": start_text, "endAt": end_text})
        if status == 200 and len(res.get("data") or []) >= 5:
            chosen = (day.isoformat(), hour, start_text, end_text, res["data"])
            break
    if chosen:
        break

if not chosen:
    print("[FAIL] 未找到可用测试时段，请先重新导入种子数据")
    sys.exit(1)

test_date, test_hour, start_text, end_text, available_courts = chosen
court_id = available_courts[0]["courtId"]
check("视图查询可预约场地（完整区间全部空闲）", len(available_courts) >= 5,
      f"date={test_date} {test_hour} courts={len(available_courts)}")
check("维护中/停用场地未出现在视图结果中",
      all(c["courtId"] not in (6, 7) for c in available_courts),
      f"ids={[c['courtId'] for c in available_courts]}")


def slot_map(day):
    status, res = call("GET", f"/admin/courts/{court_id}/slots", admin, params={"date": day})
    return {s["slotStart"][11:16]: s for s in res.get("data", [])}


def slot_time(text, add_minutes=0):
    return (datetime.datetime.strptime(text, "%Y-%m-%d %H:%M:%S")
            + datetime.timedelta(minutes=add_minutes)).strftime("%H:%M")


def plus_minutes(text, add_minutes):
    return (datetime.datetime.strptime(text, "%Y-%m-%d %H:%M:%S")
            + datetime.timedelta(minutes=add_minutes)).strftime("%Y-%m-%d %H:%M:%S")

first_slot = slot_time(start_text)
second_slot = slot_time(start_text, 30)

# 1. 权限：普通会员访问管理接口
status, res = call("GET", "/admin/members", member, params={"page": 1, "size": 1})
check("普通会员访问管理接口被拒绝", status == 403, f"status={status} msg={res.get('message')}")

status, res = call("PUT", "/admin/members/3/level", member, body={"levelId": 4, "reason": "越权测试"})
check("普通会员修改会员等级被拒绝", status == 403, f"status={status} msg={res.get('message')}")

# 2. 会员创建预约 + 触发器占用
request_id = f"smoke-{int(time.time())}"
status, res = call("POST", "/bookings", member, body={
    "courtId": court_id,
    "startAt": start_text,
    "endAt": end_text,
    "requestId": request_id,
    "payNow": True
})
booking = res.get("data") or {}
booking_id = booking.get("bookingId")
check("会员创建预约成功", status == 200 and res.get("code") == 200,
      f"status={status} msg={res.get('message')} bookingNo={booking.get('bookingNo')}")

slots = slot_map(test_date)
check("触发器占用对应 30 分钟时间段",
      slots.get(first_slot, {}).get("slotStatus") == "BOOKED"
      and slots.get(second_slot, {}).get("slotStatus") == "BOOKED",
      f"{first_slot}={slots.get(first_slot, {}).get('slotStatus')} "
      f"{second_slot}={slots.get(second_slot, {}).get('slotStatus')}")

# 3. 幂等与重叠拦截
status, res = call("POST", "/bookings", member, body={
    "courtId": court_id, "startAt": start_text, "endAt": end_text,
    "requestId": request_id, "payNow": True
})
check("相同 requestId 幂等返回同一订单",
      status == 200 and (res.get("data") or {}).get("bookingId") == booking_id,
      f"bookingId={(res.get('data') or {}).get('bookingId')}")

overlap_start = plus_minutes(start_text, 30)
overlap_end = plus_minutes(end_text, 30)
status, res = call("POST", "/bookings", member, body={
    "courtId": court_id, "startAt": overlap_start, "endAt": overlap_end,
    "requestId": request_id + "-dup", "payNow": True
})
check("同一场地重叠时间段被拦截", status == 409, f"status={status} msg={res.get('message')}")

# 4. 取消预约 + 触发器释放
status, res = call("PATCH", f"/bookings/{booking_id}/cancel", member, body={"reason": "冒烟测试取消"})
check("会员取消预约", status == 200, f"status={status} msg={res.get('message')}")

slots = slot_map(test_date)
check("取消后仅释放本订单时间段",
      slots.get(first_slot, {}).get("slotStatus") == "AVAILABLE"
      and slots.get(second_slot, {}).get("slotStatus") == "AVAILABLE",
      f"{first_slot}={slots.get(first_slot, {}).get('slotStatus')} "
      f"{second_slot}={slots.get(second_slot, {}).get('slotStatus')}")

# 5. 完整使用闭环：预约 -> 签到 -> 结束使用
status, baseline_res = call("GET", f"/admin/reports/daily/court/{court_id}", admin,
                            params={"date": test_date})
base_minutes = int((baseline_res.get("data") or {}).get("totalActualMinutes") or 0)
base_amount = float((baseline_res.get("data") or {}).get("totalSalesAmount") or 0)

status, res = call("POST", "/bookings", admin, body={
    "memberId": 3,
    "courtId": court_id,
    "startAt": start_text,
    "endAt": end_text,
    "requestId": f"smoke-flow-{int(time.time())}",
    "payNow": True
})
flow = res.get("data") or {}
check("管理员代客下单", status == 200,
      f"bookingId={flow.get('bookingId')} status={flow.get('status')}")

call("PUT", "/admin/system/clock", admin, body={"simulatedNow": start_text, "paused": False})
status, res = call("PATCH", f"/admin/bookings/{flow.get('bookingId')}/check-in", admin)
check("签到并记录实际开始时间", status == 200 and (res.get("data") or {}).get("status") == "IN_PROGRESS",
      f"status={(res.get('data') or {}).get('status')} actualStart={(res.get('data') or {}).get('actualStartAt')}")

checkout_time = (datetime.datetime.strptime(start_text, "%Y-%m-%d %H:%M:%S")
                 + datetime.timedelta(minutes=70)).strftime("%Y-%m-%d %H:%M:%S")
call("PUT", "/admin/system/clock", admin, body={"simulatedNow": checkout_time, "paused": False})
status, res = call("PATCH", f"/admin/bookings/{flow.get('bookingId')}/check-out", admin)
settlement = (res.get("data") or {}).get("settlement") or {}
check("结束使用生成实际使用记录",
      status == 200 and settlement.get("actualMinutes") == 70 and settlement.get("billedMinutes") == 90,
      f"actual={settlement.get('actualMinutes')}min billed={settlement.get('billedMinutes')}min "
      f"fee={settlement.get('actualFeeAmount')}")

rate = float(flow.get("hourlyRateSnapshot") or 0)
discount = float(flow.get("discountSnapshot") or 1)
expected_fee = round(90 / 60 * rate * discount, 2)
check("结算金额按单价与折扣快照计算",
      abs(float(settlement.get("actualFeeAmount", 0)) - expected_fee) < 0.01,
      f"expected={expected_fee} actual={settlement.get('actualFeeAmount')}")

status, res = call("GET", f"/admin/reports/daily/court/{court_id}", admin, params={"date": test_date})
minutes = int((res.get("data") or {}).get("totalActualMinutes") or 0)
amount = float((res.get("data") or {}).get("totalSalesAmount") or 0)
check("存储过程统计实际使用时长与收入（非计费时长）",
      status == 200 and minutes == base_minutes + 70
      and abs(amount - (base_amount + expected_fee)) < 0.01,
      f"delta_minutes={minutes - base_minutes} delta_amount={round(amount - base_amount, 2)}")

# 恢复演示时钟
call("PUT", "/admin/system/clock", admin, body={"simulatedNow": original_now, "paused": False})

# 6. 手动调级与日志
status, detail_res = call("GET", "/admin/members/7", admin)
current_level = ((detail_res.get("data") or {}).get("member") or {}).get("levelId")
target_level = 3 if current_level != 3 else 4
status, res = call("PUT", "/admin/members/7/level", admin,
                   body={"levelId": target_level, "reason": "API 冒烟测试：手动调整"})
check("管理员手动调整会员等级", status == 200 and (res.get("data") or {}).get("levelId") == target_level,
      f"level={(res.get('data') or {}).get('levelName')}")

status, res = call("GET", "/admin/members/7/level-history", admin)
logs = res.get("data") or []
check("等级变更写入 member_level_change_log",
      status == 200 and len(logs) > 0 and logs[0].get("changeType") == "MANUAL",
      f"latest={logs[0].get('reason') if logs else 'none'}")

# 7. 商品销售与库存
status, before = call("GET", "/admin/products", admin)
stock_before = next((p["stock"] for p in before.get("data", []) if p["productId"] == 3), None)
status, res = call("POST", "/admin/sales", admin, body={
    "memberId": 2, "paymentMethod": "WECHAT",
    "items": [{"productId": 3, "quantity": 2}]
})
check("创建商品销售单", status == 200 and float((res.get("data") or {}).get("totalAmount", 0)) == 16.0,
      f"amount={(res.get('data') or {}).get('totalAmount')}")
status, after = call("GET", "/admin/products", admin)
stock_after = next((p["stock"] for p in after.get("data", []) if p["productId"] == 3), None)
check("商品库存自动扣减", stock_before is not None and stock_after == stock_before - 2,
      f"{stock_before} -> {stock_after}")

# 8. 报表统计
status, res = call("GET", "/admin/reports/daily/court", admin, params={"date": test_date})
check("全部场地日统计（存储过程）", status == 200 and len((res.get("data") or {}).get("courts", [])) == 7,
      f"courts={len((res.get('data') or {}).get('courts', []))} "
      f"total={(res.get('data') or {}).get('total', {}).get('totalSalesAmount')}")

status, res = call("GET", "/admin/sales/statistics", admin,
                   params={"from": (base_date - datetime.timedelta(days=7)).isoformat(),
                           "to": test_date, "groupBy": "day"})
check("租赁收入与商品收入分列统计",
      status == 200 and (res.get("data") or {}).get("rentalTotal") is not None
      and (res.get("data") or {}).get("productTotal") is not None,
      f"rental={(res.get('data') or {}).get('rentalTotal')} "
      f"product={(res.get('data') or {}).get('productTotal')}")

print("-" * 60)
print(f"TOTAL: {PASSED + FAILED}  PASSED: {PASSED}  FAILED: {FAILED}")
sys.exit(1 if FAILED else 0)
