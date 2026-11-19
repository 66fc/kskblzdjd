/**
 * 打印访问地址（本机 / 局域网），并检测各服务健康状态。
 * 用法：node scripts/print-urls.mjs [端口]
 */
import { networkInterfaces } from 'node:os'

const PORT = Number(process.argv[2] || process.env.PORT || 8088)
const BASE = `http://127.0.0.1:${PORT}`

async function probe(path) {
  const started = Date.now()
  try {
    const res = await fetch(BASE + path, { signal: AbortSignal.timeout(4000) })
    return { ok: res.ok, status: res.status, ms: Date.now() - started }
  } catch {
    return { ok: false, status: 0, ms: Date.now() - started }
  }
}

const v4 = Object.values(networkInterfaces())
  .flat()
  .filter((i) => i && i.family === 'IPv4' && !i.internal)
  .map((i) => i.address)

const [page, api] = await Promise.all([probe('/club/home'), probe('/api/system/clock')])

console.log('  服务状态')
console.log(`    Web  ${page.ok ? '正常' : '异常'}  ${page.status}  (${page.ms}ms)`)
console.log(`    API  ${api.ok ? '正常' : '异常'}  ${api.status}  (${api.ms}ms)`)
console.log('')
console.log('  访问地址')
console.log(`    本机     ${BASE}/`)
if (v4.length === 0) {
  console.log('    局域网   未检测到局域网地址')
} else {
  v4.forEach((ip, i) => {
    console.log(`    ${i === 0 ? '局域网  ' : '        '}  http://${ip}:${PORT}/`)
  })
}
console.log('')
console.log('  页面入口')
console.log(`    客户端   ${BASE}/club/home      （会员预约）`)
console.log(`    后台     ${BASE}/login           （admin / admin123）`)
console.log(`    会员登录 ${BASE}/club/login      （member02 / 123456）`)
console.log(`    Swagger  http://127.0.0.1:8080/swagger-ui.html`)
console.log('')
