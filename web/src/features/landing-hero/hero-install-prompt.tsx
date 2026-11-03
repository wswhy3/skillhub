import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Bot, Check, Copy, Sparkles } from 'lucide-react'
import { useCopyToClipboard } from '@/shared/lib/clipboard'
import { getAppBaseUrl } from '@/shared/lib/app-base-url'

// 适配的主流 Agent / 客户端名单（产品名不做 i18n；跑马灯无缝循环需渲染两份列表）
const AGENTS = ['Claude Code', 'Cursor', 'Codex', 'Trae', 'OpenClaw', 'Gemini CLI'] as const

/**
 * 首屏"一键安装"模块（对标 deepskill.market 的 TRY IT NOW 面板，换用 SkillHub 首页色系）。
 * 顶行 = 品牌标（brand 渐变）+ 标题/描述 + 一键复制 Agent 接入指令；
 * 底行 = 适配 Agent 名单横向跑马灯。
 * 复制的指令与"快速开始 - 我是 Agent"页签同源（landing.quickStart.agent.commandTemplate），
 * 两处保持一致避免用户复制到不同口径的接入指令。
 *
 * 交互：
 * - 入场：hero-rise 上浮淡入（delay-8 = 0.8s，接在按钮 0.6/0.7s 之后，原统计行节奏位）
 * - 点击复制：按钮短暂切换为"已复制"（绿色，与快速开始 agent 指令色一致），2s 自动复位
 */
export function HeroInstallPrompt() {
  const { t } = useTranslation()
  const [copied, copy] = useCopyToClipboard()
  const baseUrl = useMemo(() => getAppBaseUrl(), [])

  // Agent 接入指令：运行时拼接真实 registry 地址
  const command = t('landing.quickStart.agent.commandTemplate', {
    defaultValue: t('landing.quickStart.agent.command'),
    url: `${baseUrl}/registry/skill.md`,
  })

  const handleCopy = async () => {
    try {
      await copy(command)
    } catch (err) {
      console.error('Failed to copy:', err)
    }
  }

  const label = copied ? t('landing.installPrompt.copied') : t('landing.installPrompt.copy')

  return (
    <div className="animate-hero-rise delay-8 w-full max-w-2xl mt-12">
      <div
        className="rounded-2xl border overflow-hidden bg-[hsl(var(--card))] shadow-[0_16px_40px_-24px_rgba(15,23,42,0.3)]"
        style={{ borderColor: 'hsl(var(--border-card))' }}
      >
        {/* 顶行：品牌标 + 标题/描述 + 复制按钮 */}
        <div className="flex items-center gap-3.5 p-4 md:p-[18px]">
          <div className="w-10 h-10 shrink-0 rounded-xl bg-brand-gradient flex items-center justify-center shadow-sm">
            <Sparkles className="w-5 h-5 text-white" strokeWidth={1.75} />
          </div>
          <div className="min-w-0 flex-1">
            <h3 className="text-sm font-semibold leading-snug" style={{ color: 'hsl(var(--foreground))' }}>
              {t('landing.installPrompt.title')}
            </h3>
            <p className="mt-0.5 text-xs leading-relaxed" style={{ color: 'hsl(var(--text-muted))' }}>
              {t('landing.installPrompt.description')}
            </p>
          </div>
          <button
            type="button"
            onClick={handleCopy}
            aria-label={label}
            title={label}
            className="shrink-0 flex items-center gap-1.5 rounded-lg border px-3.5 py-2 text-xs font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 cursor-pointer"
            style={{
              background: copied ? 'rgba(22, 163, 74, 0.08)' : 'hsl(var(--primary) / 0.08)',
              borderColor: copied ? 'rgba(22, 163, 74, 0.35)' : 'hsl(var(--primary) / 0.25)',
              color: copied ? '#16A34A' : 'hsl(var(--primary))',
            }}
          >
            {copied ? <Check className="w-3.5 h-3.5" strokeWidth={2} /> : <Copy className="w-3.5 h-3.5" strokeWidth={2} />}
            <span aria-live="polite">{label}</span>
          </button>
        </div>

        {/* 底行：适配 Agent 跑马灯（样式见 index.css .hero-agent-marquee / -track） */}
        <div
          className="flex items-center gap-3 px-4 md:px-[18px] py-3 border-t bg-[hsl(var(--muted))]"
          style={{ borderColor: 'hsl(var(--border-card))' }}
        >
          <span
            className="flex items-center gap-1.5 text-[11px] shrink-0 whitespace-nowrap"
            style={{ color: 'hsl(var(--text-muted))' }}
          >
            <Bot className="w-3.5 h-3.5" strokeWidth={1.75} />
            {t('landing.installPrompt.agentsEyebrow')}
          </span>
          <div className="hero-agent-marquee min-w-0 flex-1 overflow-hidden">
            <div className="hero-agent-track flex w-max items-center gap-6">
              {[...AGENTS, ...AGENTS].map((name, i) => (
                <span
                  key={`${name}-${i}`}
                  className="text-xs font-medium whitespace-nowrap"
                  style={{ color: 'hsl(var(--text-muted))' }}
                >
                  {name}
                </span>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
