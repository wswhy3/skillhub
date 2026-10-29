import { Link, useNavigate } from '@tanstack/react-router'
import { useTranslation } from 'react-i18next'
import { Search as SearchIcon } from 'lucide-react'
import { normalizeSearchQuery } from '@/shared/lib/search-query'
import { cn } from '@/shared/lib/utils'

interface HeroActionsProps {
  /** dark 用于 3D 深色首屏 overlay，light 用于静态降级 Hero */
  theme?: 'light' | 'dark'
}

/** 首屏搜索框 + 探索/发布 CTA，3D 场景与静态降级两种形态共用 */
export function HeroActions({ theme = 'light' }: HeroActionsProps) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const dark = theme === 'dark'

  const handleSearch = (query: string) => {
    navigate({
      to: '/search',
      search: { q: normalizeSearchQuery(query), sort: 'relevance', page: 0, starredOnly: false },
    })
  }

  return (
    <>
      {/* 流光边框：渐变边框环（::before）+ 光带沿边框跑圈（裁剪层把光带锁进 1.5px 环内），
          样式见 index.css .hero-search-beam / -clip / -dot */}
      <div className="hero-search-beam animate-hero-rise delay-5 w-full max-w-2xl mb-6">
        <div className="hero-search-beam-clip">
          <span className="hero-search-beam-dot" aria-hidden="true" />
        </div>
        <div
          className={cn(
            'relative flex items-center rounded-[11px] px-5 py-3.5',
            dark ? 'bg-[#0C0E21]/85 backdrop-blur-md' : 'bg-white shadow-sm',
          )}
        >
          {/* 图标与 placeholder 用 --text-muted（46% 亮度深灰）而非 --text-placeholder（63% 浅灰）：
              浅灰在部分屏幕/暗色插件下接近不可见，用户反复反馈"没显出来"，加深保可见 */}
          <SearchIcon
            className="w-5 h-5 flex-shrink-0 mr-3"
            style={{ color: dark ? 'rgba(255,255,255,0.5)' : 'hsl(var(--text-muted))' }}
            strokeWidth={2}
          />
          <input
            type="text"
            placeholder={t('landing.hero.searchPlaceholder')}
            className={cn(
              'hero-input flex-1 bg-transparent outline-none text-base',
              dark ? 'text-white placeholder:text-white/50' : 'placeholder:text-[hsl(var(--text-muted))]',
            )}
            style={dark ? undefined : { color: 'hsl(var(--foreground))' }}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                handleSearch((e.target as HTMLInputElement).value)
              }
            }}
          />
        </div>
      </div>

      <div className="flex flex-wrap justify-center gap-4">
        {/* 心跳包一层 wrapper 施加（见 index.css .btn-heartbeat）：动画放在按钮自身会
            与 hero-rise 入场的 animation 简写互相覆盖，wrapper 独立元素互不干扰。
            wrapper 必须 flex：否则内部 Link 回落成 inline 元素，垂直 padding 不撑行盒、
            按钮在行内按基线对齐，与右侧 stretch 的发布按钮 y 轴错位【坑】 */}
        <div className="btn-heartbeat flex">
          <Link
            to="/search"
            search={{ q: '', sort: 'relevance', page: 0, starredOnly: false }}
            className="animate-hero-rise delay-6 px-8 py-3.5 rounded-xl text-base font-medium text-white bg-brand-gradient shadow-sm hover:opacity-95 transition-opacity"
          >
            {t('landing.hero.exploreSkills')}
          </Link>
        </div>
        <Link
          to="/dashboard/publish"
          className={cn(
            'animate-hero-rise delay-7 px-8 py-3.5 rounded-xl text-base font-medium border transition-colors',
            dark && 'border-white/25 text-white/85 hover:bg-white/10',
          )}
          style={
            dark
              ? undefined
              : {
                  background: 'hsl(var(--secondary))',
                  borderColor: 'hsl(var(--muted-foreground))',
                  color: 'hsl(var(--muted-foreground))',
                }
          }
        >
          {t('landing.hero.publishSkill')}
        </Link>
      </div>
    </>
  )
}
