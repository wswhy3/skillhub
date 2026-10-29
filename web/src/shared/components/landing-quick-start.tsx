import { useState, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Bot, Check, Copy, Terminal, UserRound } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { useCopyToClipboard } from '@/shared/lib/clipboard'

type LandingQuickStartTabId = 'agent' | 'human' | 'cli'

interface LandingQuickStartTab {
  id: LandingQuickStartTabId
  label: string
  description: string
  command: string
}

const tabIcons: Record<LandingQuickStartTabId, LucideIcon> = {
  agent: Bot,
  human: UserRound,
  cli: Terminal,
}

/**
 * Get the base URL for the application.
 * Prefers the runtime config if set and not localhost.
 * Falls back to the current page origin.
 */
function getAppBaseUrl(): string {
  if (typeof window === 'undefined') {
    return ''
  }
  const runtimeConfig = window.__SKILLHUB_RUNTIME_CONFIG__
  const configuredUrl = runtimeConfig?.appBaseUrl
  // Use configured URL only if it's set and not localhost
  if (configuredUrl && !configuredUrl.includes('localhost')) {
    return configuredUrl
  }
  // Fallback to current page origin
  return `${window.location.protocol}//${window.location.host}`
}

function CompactCopyButton({ text }: { text: string }) {
  const { t } = useTranslation()
  const [copied, copy] = useCopyToClipboard()

  const handleCopy = async () => {
    try {
      await copy(text)
    } catch (err) {
      console.error('Failed to copy:', err)
    }
  }

  const label = copied ? (t('copyButton.copied') || 'Copied') : (t('copyButton.copy') || 'Copy')

  return (
    <button
      type="button"
      onClick={handleCopy}
      aria-label={label}
      title={label}
      className="absolute right-2.5 top-1/2 flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-xl border bg-[hsl(var(--card))] transition-colors hover:bg-[hsl(var(--muted))] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 cursor-pointer"
      style={{ borderColor: 'hsl(var(--border))', color: 'hsl(var(--foreground))' }}
    >
      {copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
    </button>
  )
}

export function LandingQuickStartSection() {
  const { t } = useTranslation()
  const [activeTab, setActiveTab] = useState<LandingQuickStartTabId>('agent')
  const baseUrl = useMemo(() => getAppBaseUrl(), [])

  // Build dynamic agent command with actual registry URL
  const agentCommand = t('landing.quickStart.agent.commandTemplate', {
    defaultValue: t('landing.quickStart.agent.command'),
    url: `${baseUrl}/registry/skill.md`,
  })

  const tabs: LandingQuickStartTab[] = [
    {
      id: 'agent',
      label: t('landing.quickStart.tabs.agent'),
      description: t('landing.quickStart.agent.description'),
      command: agentCommand,
    },
    {
      id: 'human',
      label: t('landing.quickStart.tabs.human'),
      description: t('landing.quickStart.human.description'),
      command: t('landing.quickStart.human.command'),
    },
    {
      id: 'cli',
      label: t('landing.quickStart.tabs.cli'),
      description: t('landing.quickStart.cli.description'),
      command: t('landing.quickStart.cli.command'),
    },
  ]

  const currentTab = tabs.find((tab) => tab.id === activeTab) ?? tabs[0]

  return (
    <section className="relative z-10 w-full px-6 pt-14 pb-32 md:pt-16 md:pb-44 overflow-visible">
      {/* jizuo.png 圆盘底座：绝对定位在 section 底部，居中。
          bottom 取负值让圆盘上半部分嵌入到 Tab 卡片下方、下半部分延伸到 section 之外，
          实现"卡片坐在圆盘上"的视觉效果；宽度 max-w-7xl (1280px) 大气呈现，
          opacity-50 弱化存在感避免喧宾夺主；
          z-0 让圆盘垫在卡片（z-10）之下、整页背景（z-0）之上。
          性能优化：loading=lazy 折叠下方懒加载，decoding=async 不阻塞主线程。 */}
      <img
        aria-hidden="true"
        src="/landing/jizuo.png"
        alt=""
        loading="lazy"
        decoding="async"
        className="absolute left-1/2 -translate-x-1/2 bottom-[-250px] w-full max-w-7xl z-0 pointer-events-none select-none opacity-50"
      />
      <div className="relative z-10 max-w-4xl mx-auto">
        <div className="text-center mb-7 md:mb-8">
          <h2 className="text-3xl md:text-4xl font-bold tracking-tight mb-3" style={{ color: 'hsl(var(--foreground))' }}>
            {t('landing.quickStart.title')}
          </h2>
          <p className="text-base md:text-lg max-w-2xl mx-auto leading-relaxed" style={{ color: 'hsl(var(--text-secondary))' }}>
            {t('landing.quickStart.description', { defaultValue: t('landing.quickStart.subtitle') })}
          </p>
        </div>

        <div
          className="mx-auto max-w-2xl rounded-[28px] border bg-[hsl(var(--card))] p-3 shadow-[0_24px_60px_-28px_rgba(15,23,42,0.25)]"
          style={{ borderColor: 'hsl(var(--border-card))' }}
        >
          <div
            className="grid grid-cols-1 gap-2 rounded-2xl p-1.5 md:grid-cols-3"
            style={{ background: 'linear-gradient(180deg, hsl(var(--muted)/0.95) 0%, hsl(var(--muted)/0.75) 100%)' }}
          >
            {tabs.map((tab) => {
              const isActive = tab.id === currentTab.id
              const Icon = tabIcons[tab.id]

              return (
                <button
                  key={tab.id}
                  type="button"
                  onClick={() => setActiveTab(tab.id)}
                  aria-pressed={isActive}
                  className="flex min-h-11 items-center justify-center gap-2 rounded-[14px] px-4 py-3 text-base font-medium transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 cursor-pointer"
                  style={{
                    background: isActive ? 'hsl(var(--card))' : 'transparent',
                    color: isActive ? 'hsl(var(--foreground))' : 'hsl(var(--muted-foreground))',
                    boxShadow: isActive ? '0 6px 18px rgba(15, 23, 42, 0.08)' : 'none',
                  }}
                >
                  <Icon className="h-4 w-4" strokeWidth={1.75} />
                  <span>{tab.label}</span>
                </button>
              )
            })}
          </div>

          <div className="px-4 pb-4 pt-8 md:px-8 md:pb-6 md:pt-9">
            <p
              className="mx-auto mb-6 max-w-xl text-center text-base font-medium leading-relaxed md:text-lg"
              style={{ color: 'hsl(var(--foreground))' }}
            >
              {currentTab.description}
            </p>

            <div
              className="relative rounded-2xl border bg-[hsl(var(--muted))] px-4 py-3 pr-14 shadow-[inset_0_1px_0_rgba(255,255,255,0.7)]"
              style={{ borderColor: 'hsl(var(--border))' }}
            >
              <div className="overflow-x-auto whitespace-nowrap">
                <code
                  className="font-mono text-sm md:text-base"
                  style={{ color: currentTab.id === 'agent' ? '#16A34A' : '#0F172A' }}
                >
                  {currentTab.command}
                </code>
              </div>
              <CompactCopyButton text={currentTab.command} />
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
