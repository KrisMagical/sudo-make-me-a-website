export interface VideoMatch {
  provider: string
  embedUrl: string
  sourceUrl: string
}


export function parseVideoUrl(url: string): VideoMatch | null {
  const trimmed = url.trim()

  // --- YouTube ---
  // 常规: https://www.youtube.com/watch?v=xxx
  // 短链: https://youtu.be/xxx
  // 嵌入: https://www.youtube.com/embed/xxx
  const ytReg = /(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube\.com\/embed\/)([a-zA-Z0-9_-]{11})/
  let match = trimmed.match(ytReg)
  if (match) {
    const videoId = match[1]
    return {
      provider: 'youtube',
      sourceUrl: trimmed,
      embedUrl: `https://www.youtube.com/embed/${videoId}`
    }
  }

  // --- Bilibili ---
  // 标准页: https://www.bilibili.com/video/BVxxx
  // 短链: https://b23.tv/xxx
  // 已转化: //player.bilibili.com/player.html?aid=xxx&bvid=BVxxx
  const biliReg = /(?:bilibili\.com\/video\/|b23\.tv\/|player\.bilibili\.com\/player\.html\?.*?bvid=)([a-zA-Z0-9]+)/
  match = trimmed.match(biliReg)
  if (match) {
    const bvid = match[1].startsWith('BV') ? match[1] : `BV${match[1]}`
    return {
      provider: 'bilibili',
      sourceUrl: trimmed,
      embedUrl: `//player.bilibili.com/player.html?bvid=${bvid}&page=1`
    }
  }

  // --- Vimeo ---
  const vimeoReg = /vimeo\.com\/(\d+)/
  match = trimmed.match(vimeoReg)
  if (match) {
    const videoId = match[1]
    return {
      provider: 'vimeo',
      sourceUrl: trimmed,
      embedUrl: `https://player.vimeo.com/video/${videoId}`
    }
  }

  return null
}

export function splitTextByVideoLinks(text: string): Array<{ type: 'text' | 'video'; value?: string; embedUrl?: string; provider?: string }> {
  const parts: Array<{ type: 'text' | 'video'; value?: string; embedUrl?: string; provider?: string }> = []
  let remaining = text
  // 匹配常见的 http/https 链接（非 Markdown 包裹）
  const urlRegex = /https?:\/\/[^\s]+/g
  let match: RegExpExecArray | null
  let lastIndex = 0

  while ((match = urlRegex.exec(remaining)) !== null) {
    const url = match[0]
    const index = match.index

    // 前面的普通文本
    if (index > lastIndex) {
      parts.push({ type: 'text', value: remaining.slice(lastIndex, index) })
    }

    const videoInfo = parseVideoUrl(url)
    if (videoInfo) {
      parts.push({
        type: 'video',
        embedUrl: videoInfo.embedUrl,
        provider: videoInfo.provider,
        value: url
      })
    } else {
      // 这里不处理，因为普通链接会由后续的 Markdown/wiki 规则捕获，避免重复
      parts.push({ type: 'text', value: url })
    }

    lastIndex = index + url.length
  }

  if (lastIndex < remaining.length) {
    parts.push({ type: 'text', value: remaining.slice(lastIndex) })
  }

  // 如果没有匹配到任何链接，返回原始文本片段
  if (parts.length === 0) {
    parts.push({ type: 'text', value: text })
  }

  return parts
}