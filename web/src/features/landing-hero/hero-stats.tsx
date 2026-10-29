import { useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import gsap from 'gsap'

/**
 * 首屏统计行（1000+ 项目库 / 50K+ 下载量 / 200+ 团队），位于搜索/发布按钮下方。
 *
 * 交互：
 * - 入场：hero-rise 上浮淡入（delay-8 = 0.8s，排在按钮 0.6/0.7s 之后）
 * - 首次进入：数字从 0 滚动增长到目标值（GSAP 驱动，power2.out 越涨越慢）
 * - 数字直接写 DOM 文本（ref 写入，不触发 React 重渲染，与字母层同模式）
 *
 * 兜底：SSR / JS 关闭 / 减少动态偏好时 JSX 直接渲染终值（SEO 与可读性），
 * effect 内先归零再滚动——此时入场动画（0.8s 才开始显形）把归零闪烁遮住。
 */

// 目标值与显示格式：50K+ 以 K 为单位滚动（0→50K+），涨速与 1000+/200+ 视觉一致
const STATS = [
  { target: 1000, format: (n: number) => `${Math.round(n)}+` },
  { target: 50, format: (n: number) => `${Math.round(n)}K+` },
  { target: 200, format: (n: number) => `${Math.round(n)}+` },
] as const
const LABEL_KEYS = ['landing.statsSkills', 'landing.statsDownloads', 'landing.statsTeams'] as const

export function HeroStats() {
  const { t } = useTranslation()
  const numRefs = useRef<(HTMLSpanElement | null)[]>([])

  useEffect(() => {
    // 减少动态偏好：不滚动，保持 JSX 渲染的终值
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
    const tweens = STATS.map((s, i) => {
      const el = numRefs.current[i]
      if (el) el.textContent = s.format(0)
      const obj = { v: 0 }
      return gsap.to(obj, {
        v: s.target,
        duration: 1.8,
        ease: 'power2.out',
        // 0.9s ≈ 入场动画（0.8s 延迟）开始显形时刻，行内三组依次错峰
        delay: 0.9 + i * 0.18,
        onUpdate: () => {
          const cur = numRefs.current[i]
          if (cur) cur.textContent = s.format(obj.v)
        },
      })
    })
    return () => tweens.forEach((tw) => tw.kill())
  }, [])

  // mt-[90px]：原 40px 基础上再下移 50px（用户反馈与按钮太近），后续微调改此值
  return (
    <div className="hero-stats animate-hero-rise delay-8 flex flex-row justify-center gap-12 md:gap-24 mt-[90px]">
      {STATS.map((s, i) => (
        <div key={LABEL_KEYS[i]} className="flex flex-col items-center">
          <span
            ref={(el) => { numRefs.current[i] = el }}
            className="text-3xl md:text-4xl font-bold tracking-tight text-brand-gradient mb-1"
          >
            {s.format(s.target)}
          </span>
          <span className="text-sm font-normal" style={{ color: 'hsl(var(--foreground))' }}>
            {t(LABEL_KEYS[i])}
          </span>
        </div>
      ))}
    </div>
  )
}
