/**
 * 羽球幻想馆 · 生产静态服务
 *
 * 作用：
 *   1. 托管 frontend/dist 构建产物（单页应用，history 路由回退到 index.html）
 *   2. 把 /api 反向代理到本机 Spring Boot（默认 127.0.0.1:8080）
 *   3. 监听 0.0.0.0，让同一局域网的其他电脑可以访问
 *
 * 用法：
 *   node scripts/serve-prod.mjs                 # 默认 8088 端口
 *   PORT=9000 node scripts/serve-prod.mjs       # 自定义端口
 *
 * 无第三方依赖，只需 Node 18+。
 */
import { createServer, request as httpRequest } from 'node:http'
import { createReadStream, existsSync, statSync } from 'node:fs'
import { extname, join, normalize, resolve, sep } from 'node:path'
import { fileURLToPath } from 'node:url'
import { networkInterfaces } from 'node:os'

const ROOT = resolve(fileURLToPath(new URL('..', import.meta.url)))
const DIST = join(ROOT, 'frontend', 'dist')
const PORT = Number(process.env.PORT || 8088)
const HOST = process.env.HOST || '0.0.0.0'
const API_TARGET_HOST = process.env.API_HOST || '127.0.0.1'
const API_TARGET_PORT = Number(process.env.API_PORT || 8080)

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.webp': 'image/webp',
  '.gif': 'image/gif',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.map': 'application/json; charset=utf-8',
  '.txt': 'text/plain; charset=utf-8'
}

if (!existsSync(join(DIST, 'index.html'))) {
  console.error(`[serve] 未找到构建产物：${DIST}`)
  console.error('[serve] 请先在 frontend 目录执行 npm run build')
  process.exit(1)
}

function sendJson(res, status, payload) {
  const body = JSON.stringify(payload)
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(body),
    'Cache-Control': 'no-store'
  })
  res.end(body)
}

/** 把 /api 请求转发到 Spring Boot */
function proxyApi(req, res) {
  const headers = { ...req.headers, host: `${API_TARGET_HOST}:${API_TARGET_PORT}` }
  const upstream = httpRequest(
    {
      host: API_TARGET_HOST,
      port: API_TARGET_PORT,
      method: req.method,
      path: req.url,
      headers
    },
    (upRes) => {
      res.writeHead(upRes.statusCode || 502, upRes.headers)
      upRes.pipe(res)
    }
  )

  upstream.setTimeout(30000, () => {
    upstream.destroy()
    if (!res.headersSent) sendJson(res, 504, { code: 504, message: '后端响应超时' })
  })

  upstream.on('error', (err) => {
    if (res.headersSent) return
    console.error(`[serve] API 代理失败：${err.message}`)
    sendJson(res, 502, {
      code: 502,
      message: '后端服务未启动或无法连接，请先启动 Spring Boot（8080）'
    })
  })

  req.pipe(upstream)
}

/** 静态文件（带目录穿越防护） */
function serveStatic(req, res, urlPath) {
  let relative = decodeURIComponent(urlPath)
  if (relative === '/' || relative === '') relative = '/index.html'

  const target = normalize(join(DIST, relative))
  const inside = target === DIST || target.startsWith(DIST + sep)

  if (inside && existsSync(target) && statSync(target).isFile()) {
    const ext = extname(target).toLowerCase()
    const isHashed = /\/assets\//.test(relative) && /\.[0-9a-zA-Z_-]{8,}\./.test(relative)
    res.writeHead(200, {
      'Content-Type': MIME[ext] || 'application/octet-stream',
      'Cache-Control': isHashed ? 'public, max-age=31536000, immutable' : 'no-cache'
    })
    createReadStream(target).pipe(res)
    return
  }

  // 带扩展名的资源找不到 → 404；否则按 SPA 路由回退
  if (extname(relative)) {
    res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' })
    res.end('404 Not Found')
    return
  }

  res.writeHead(200, { 'Content-Type': MIME['.html'], 'Cache-Control': 'no-cache' })
  createReadStream(join(DIST, 'index.html')).pipe(res)
}

const server = createServer((req, res) => {
  const started = Date.now()
  res.on('finish', () => {
    if (req.url?.startsWith('/api')) {
      console.log(`[serve] ${req.method} ${req.url} -> ${res.statusCode} (${Date.now() - started}ms)`)
    }
  })

  const urlPath = (req.url || '/').split('?')[0]
  if (urlPath === '/api' || urlPath.startsWith('/api/') || urlPath.startsWith('/v3/api-docs') || urlPath.startsWith('/swagger-ui')) {
    proxyApi(req, res)
    return
  }
  serveStatic(req, res, urlPath)
})

server.listen(PORT, HOST, () => {
  const v4 = Object.values(networkInterfaces())
    .flat()
    .filter((i) => i && i.family === 'IPv4' && !i.internal)
    .map((i) => i.address)

  console.log('')
  console.log('  羽球幻想馆 · 生产服务已启动')
  console.log(`  本机访问   http://127.0.0.1:${PORT}`)
  v4.forEach((ip) => console.log(`  局域网访问 http://${ip}:${PORT}`))
  console.log(`  API 代理   /api -> http://${API_TARGET_HOST}:${API_TARGET_PORT}`)
  console.log('')
})
