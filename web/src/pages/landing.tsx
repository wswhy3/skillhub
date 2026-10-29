import { useNavigate } from '@tanstack/react-router'
import { useTranslation } from 'react-i18next'
import { PackageOpen, Terminal, Shield, Users, GitBranch, Settings, Sparkles } from 'lucide-react'
import { useEffect, useLayoutEffect, useRef, type CSSProperties } from 'react'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import { LandingQuickStartSection } from '@/shared/components/landing-quick-start'
import { SkillCard } from '@/features/skill/skill-card'
import { SkeletonList } from '@/shared/components/skeleton-loader'
import { useSearchSkills } from '@/shared/hooks/use-skill-queries'
import { useInView } from '@/shared/hooks/use-in-view'
import { Button } from '@/shared/ui/button'
import { HeroActions } from '@/features/landing-hero/hero-actions'
import { HeroStats } from '@/features/landing-hero/hero-stats'
import { MorphBg } from '@/features/landing-hero/svg-morph-text'

gsap.registerPlugin(ScrollTrigger)

/**
 * 首页：固定背景层文字循环变形（CHANGHONG IT ↔ SKILLHUB）+ 内容区正常滚动。
 * 背景字母随页面滚动持续拆解/重组，贯穿整个浏览过程。
 */
export function LandingPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const scrollProgressRef = useRef(0)

  // 整页背景高度跟随：bg.png 以 objectFit:fill 拉伸铺满整页，页面总高度突变时
  // （异步数据替换骨架屏、字体换档等）背景会瞬间重拉伸，视觉上"跳一下"。
  // 这里显式接管背景层高度——初始值在首帧绘制前写入（useLayoutEffect，避免从 0 长高的
  // 入场动画），之后经 ResizeObserver 跟随根容器，配合 .landing-bg-follow 的 height
  // 过渡把"瞬间重拉伸"变成"平滑落定"。背景层脱离文档流，写高度不会触发 RO 反馈循环。
  const bgRef = useRef<HTMLDivElement>(null)
  useLayoutEffect(() => {
    const el = bgRef.current
    const parent = el?.parentElement
    if (!el || !parent) return
    const sync = () => {
      el.style.height = `${parent.offsetHeight}px`
    }
    sync()
    if (typeof ResizeObserver === 'undefined') return
    const ro = new ResizeObserver(sync)
    ro.observe(parent)
    return () => ro.disconnect()
  }, [])

  // 滚动进度驱动文字变形
  useEffect(() => {
    const update = () => {
      const max = Math.max(document.documentElement.scrollHeight - window.innerHeight, 1)
      scrollProgressRef.current = Math.min(Math.max(window.scrollY / max, 0), 1)
    }
    update()
    window.addEventListener('scroll', update, { passive: true })
    window.addEventListener('resize', update)
    return () => {
      window.removeEventListener('scroll', update)
      window.removeEventListener('resize', update)
    }
  }, [])

  // 扇形抽卡：牌堆滚入视口后被"钉住"，滚动进度直接驱动逐张向右舒展成扇（pin + scrub）；
  // 展开完毕自动解除钉住，页面才继续往下滚（游戏站角色展示模式）。
  // 手机窄屏 / 系统减少动态：不钉不动画，普通排列。
  useEffect(() => {
    const section = featuresSectionRef.current
    const stage = fanStageRef.current
    if (!section || !stage) return
    const cards = Array.from(stage.querySelectorAll<HTMLElement>('.gacha-card'))
    if (cards.length === 0) return

    const mm = gsap.matchMedia()
    mm.add('(min-width: 640px) and (prefers-reduced-motion: no-preference)', () => {
      // 运行时测量：先清残留 transform（HMR 重挂载会有内联样式残留），再测 flex 原始位置。
      // 用 offsetLeft（相对 stage 的布局位）而非 getBoundingClientRect——后者受 pin 的
      // translate 影响，HMR 在 pin 区间中途重建时会测到视口位导致牌堆错位【坑】
      cards.forEach((el) => gsap.set(el, { clearProps: 'transform' }))

      // 两态几何：
      // 牌堆 = 全部叠到舞台左缘（20px 内边距起），每张右错 6px 下错 5px 微错位
      //   卡 i 落点(相对 stage) = offsetLeft_i + stack.x_i = 20 + i*6 → stack.x_i = 20 + i*6 - offsetLeft_i
      // 扇形 = 弧线站位 + 倾角（负 margin 叠放）
      const geom = cards.map((el, i) => {
        const off = i - 2.5
        return {
          stack: { x: Math.round(20 + i * 6 - el.offsetLeft), y: 26 + i * 5, r: i * 2 - 4 },
          fan: { x: Math.round(off * 6), y: Math.round(off * off * 22 - 40), r: Math.round(off * 9) },
        }
      })

      const tl = gsap.timeline({
        defaults: { ease: 'power1.inOut' },
        scrollTrigger: {
          trigger: section, // 钉整个 section：标题 + 舞台 + 预告面板一起钉住，展开全程标题可见
          start: 'top 12%', // section 顶部滚到视口 12% 处开始钉住（标题完整在屏）
          end: '+=900', // 之后 900px 滚动量全部用于展开
          pin: true,
          pinSpacing: true,
          scrub: 0.5,
          anticipatePin: 1,
          // 扇面完成即挂 fan-done → 扫光（卡片在 0.78 落位，扫光在保持段内播）；
          // 展开开始即摘 hint-in：CSS 动画 fill both 的填充态优先级高于 GSAP 内联样式，
          // 不摘类退场 tween 会被动画盖住【坑】；摘类后 GSAP 内联立即接管（起始值已录为 1）
          onUpdate: (self) => {
            stage.classList.toggle('fan-done', self.progress > 0.8)
            fanHintRef.current?.classList.toggle('hint-in', self.progress < 0.02)
          },
        },
      })
      // 预告面板：展开前 30% 滚动内退场（上浮+旋转+淡出），把右侧空间让给扇面。
      // 必须用 fromTo 显式 from=1：scrub timeline 创建时会预渲染 progress 0，
      // .to() 会把起始值录成当时的 computed（入场动画未播 → 0），之后永远 0→0【坑】
      if (fanHintRef.current) {
        tl.fromTo(
          fanHintRef.current,
          { opacity: 1, y: 0, rotation: 0 },
          { opacity: 0, y: -40, rotation: 8, duration: 0.3, ease: 'power1.out' },
          0,
        )
      }
      geom.forEach((g, i) => {
        // fromTo：from=牌堆态在创建时立即应用（immediateRender），每卡一个 tween 不共享属性，
        // 不踩 morph-text 里"多个 fromTo 共享属性互相覆盖"的坑。
        // zIndex 1→6 递增：扑克扇面左牌压右牌的叠放层次
        cards[i].style.zIndex = String(i + 1)
        // 依次推进：单张 0.45 + 错峰 0.3（重叠仅 33%），从第一张到第六张波浪式展开，
        // 而不是之前 duration 0.7/错峰 0.15 的"一坨一起动"
        tl.fromTo(
          cards[i],
          { x: g.stack.x, y: g.stack.y, rotation: g.stack.r, scale: 0.9 },
          { x: g.fan.x, y: g.fan.y, rotation: g.fan.r, scale: 1, duration: 0.45 },
          i * 0.3,
        )
      })
      // 尾部保持段（容错）：第六张在 1.95s 落位，此空 tween 把总时长撑到 2.5s——
      // 最后 0.55/2.5 ≈ 22% 滚动量（约 200px）内页面仍钉住、扇面静止纯欣赏，
      // 之后才解除钉住继续下滑。空对象 tween 是 GSAP 标准的时长垫片
      tl.to({}, { duration: 0.55 })
      return () => {
        tl.scrollTrigger?.kill()
        tl.kill()
      }
    })
    // 手机窄屏 / 减少动态：清掉内联 transform，预告面板直接显示不退场
    mm.add('(max-width: 639px), (prefers-reduced-motion: reduce)', () => {
      cards.forEach((el) => gsap.set(el, { clearProps: 'transform' }))
      if (fanHintRef.current) gsap.set(fanHintRef.current, { clearProps: 'opacity,transform' })
    })
    return () => mm.revert()
    // fanHintRef 是稳定 ref，只作 GSAP 目标读取，不参与响应式依赖
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])



  const { data: popularSkills, isLoading: isLoadingPopular } = useSearchSkills({
    sort: 'downloads',
    size: 6,
  })

  const { data: latestSkills, isLoading: isLoadingLatest } = useSearchSkills({
    sort: 'newest',
    size: 6,
  })

  const handleSkillClick = (namespace: string, slug: string) => {
    navigate({ to: `/space/${namespace}/${encodeURIComponent(slug)}` })
  }

  // 标题进场：section 滚入视口即淡入（原行为）
  const featuresView = useInView()
  // 扇形抽卡：pin 整个 features section（标题+舞台+预告面板一起钉住，展开全程标题可见）
  const featuresSectionRef = useRef<HTMLElement>(null)
  const fanStageRef = useRef<HTMLDivElement>(null)
  // 预告面板进场：自身进视口挂 hint-in（CSS 动画），退场由 GSAP timeline 管
  const fanHintView = useInView({ threshold: 0.6 })
  const fanHintRef = fanHintView.ref
  const quickStartView = useInView()
  const popularView = useInView()
  const latestView = useInView()

  const features = [
    {
      icon: <Shield className="w-6 h-6 text-white" strokeWidth={2} />,
      title: t('landing.features.secure.title'),
      description: t('landing.features.secure.description'),
    },
    {
      icon: <Users className="w-6 h-6 text-white" strokeWidth={2} />,
      title: t('landing.features.community.title'),
      description: t('landing.features.community.description'),
    },
    {
      icon: <PackageOpen className="w-6 h-6 text-white" strokeWidth={2} />,
      title: t('landing.features.integration.title'),
      description: t('landing.features.integration.description'),
    },
    {
      icon: <GitBranch className="w-6 h-6 text-white" strokeWidth={2} />,
      title: t('landing.features.versionControl.title', { defaultValue: 'Version control' }),
      description: t('landing.features.versionControl.description', { defaultValue: 'Managed release flows keep skill packages traceable and easier to review.' }),
    },
    {
      icon: <Terminal className="w-6 h-6 text-white" strokeWidth={2} />,
      title: t('landing.features.cli.title', { defaultValue: 'CLI tooling' }),
      description: t('landing.features.cli.description', { defaultValue: 'Command-line workflows support publishing, installing, and operating skills quickly.' }),
    },
    {
      icon: <Settings className="w-6 h-6 text-white" strokeWidth={2} />,
      title: t('landing.features.governance.title', { defaultValue: 'Governance' }),
      description: t('landing.features.governance.description', { defaultValue: 'Built-in review and permission flows help teams enforce skill quality.' }),
    },
  ]

  return (
    <div className="relative">
      {/* UI 提供的整页背景 bg.png：objectFit: fill 拉伸铺满整页（不保持纵横比，
          跨视口圆盘位置与内容模块强绑定不会跑位），圆盘底座由快速开始 section 内的
          jizuo.png 独立定位负责。
          性能优化：fetchPriority=high 让浏览器优先拉取（首屏关键图），decoding=async 不阻塞主线程。
          高度不再用 inset-0 自动跟随：由 useLayoutEffect 显式同步根容器高度 +
          CSS height 过渡，消除异步内容到达时页面高度突变导致的背景跳变。 */}
      <div
        aria-hidden="true"
        ref={bgRef}
        className="landing-bg-follow absolute left-0 top-0 z-0 w-full pointer-events-none select-none overflow-hidden"
      >
        <img
          src="/landing/bg.png"
          alt=""
          fetchPriority="high"
          decoding="async"
          className="absolute inset-0 w-full h-full"
          style={{ objectFit: 'fill' }}
        />
      </div>

      {/* 固定背景层：文字循环变形，贯穿整个页面 */}
      <MorphBg progressRef={scrollProgressRef} />

      {/* 首屏内容：水平居中、垂直紧跟背景文字下方，与 MorphBg pt-[16vh] 配套再下移 100px */}
      <section className="relative z-10 min-h-[75vh] flex flex-col items-center justify-start px-4 pt-[27vh]">
        <div className="w-full max-w-2xl flex flex-col items-center">
          <p
            className="animate-hero-rise delay-3 text-xs md:text-sm tracking-[0.35em] uppercase mb-3 text-center"
            style={{ color: 'rgba(106,109,255,0.75)' }}
          >
            {t('landing.badge')}
          </p>
          <h1
            className="animate-hero-rise delay-4 text-2xl md:text-3xl font-bold tracking-tight text-center mb-6"
            style={{ color: 'hsl(var(--foreground))' }}
          >
            {t('landing.hero.title')}
          </h1>
          <HeroActions theme="light" />
          {/* 统计行（原位恢复）：按钮下方，入场后数字从 0 滚动增长到目标值 */}
          <HeroStats />
        </div>
      </section>

      {/* Features Section：扇形抽卡（pin+scrub 滚动驱动展开，见 useEffect）
          结构：标题 + 横向舞台（左牌堆 | 右预告面板），section 整体被钉住 → 展开全程标题可见 */}
      <section ref={featuresSectionRef} className="relative z-10 w-full py-20 md:py-24 px-6">
        <div className="max-w-[1320px] mx-auto">
          <div ref={featuresView.ref} className={`text-center mb-14 scroll-fade-up${featuresView.inView ? ' in-view' : ''}`}>
            <h2 className="text-3xl md:text-4xl font-bold tracking-tight mb-3" style={{ color: 'hsl(var(--foreground))' }}>
              {t('landing.whySkillHub.title', { defaultValue: '为什么选择 SkillHub' })}
            </h2>
            <p className="text-base md:text-lg max-w-2xl mx-auto leading-relaxed" style={{ color: 'hsl(var(--text-secondary))' }}>
              {t('landing.whySkillHub.subtitle', { defaultValue: '专为企业打造的私有化 Agent 技能管理平台' })}
            </p>
          </div>

          {/* 扇形抽卡舞台：独占整行 → 展开终态天然水平居中。
              预告面板绝对定位在右侧（不占 flex 流），滚动展开时退场让位。
              【坑：此前 stage 只占左 2/3（hint 在 flex 流占 300px），展开后扇面偏左】 */}
          <div className="relative">
            {/* 预告面板：牌堆期填补右侧留白；滚动展开时上浮旋出退场（GSAP timeline） */}
            <div
              ref={fanHintRef}
              className={`fan-hint hidden lg:flex absolute right-0 top-6 z-20 flex-col items-center justify-center gap-4 w-[300px] pointer-events-none${fanHintView.inView ? ' hint-in' : ''}`}
              aria-hidden="true"
            >
              <div className="w-16 h-16 rounded-2xl bg-brand-gradient flex items-center justify-center shadow-lg">
                <Sparkles className="w-8 h-8 text-white" strokeWidth={1.75} />
              </div>
              <p className="text-sm tracking-[0.3em] uppercase text-center" style={{ color: 'hsl(var(--text-muted))' }}>
                {t('landing.whySkillHub.fanHint', { defaultValue: '继续下滑 · 展开能力扇面' })}
              </p>
              <div className="w-px h-14 bg-gradient-to-b from-[rgba(106,109,255,0.45)] to-transparent" />
              <p className="text-xs text-center max-w-[240px] leading-relaxed" style={{ color: 'hsl(var(--text-muted))' }}>
                {t('landing.whySkillHub.fanHintDesc', { defaultValue: '六个维度的企业级能力，正等待从牌堆中依次翻开' })}
              </p>
            </div>

            <div ref={fanStageRef} className="gacha-stage relative z-10 flex flex-col sm:flex-row sm:flex-nowrap items-center justify-center gap-0">
              {features.map((feature, idx) => (
                <div
                  key={feature.title}
                  className="gacha-card w-full sm:w-[230px] sm:h-[280px] shrink-0 sm:-ml-[30px] sm:first:ml-0"
                  style={{ '--gacha-delay': `${idx * 120}ms` } as CSSProperties}
                >
                  {/* 内层卡面：视觉/悬停归 CSS，外层 transform/zIndex 归 GSAP scrub，两套互不打架 */}
                  <div
                    className="gacha-card-inner h-full bg-[hsl(var(--card))] rounded-xl p-6 border shadow-sm flex flex-col"
                    style={{ borderColor: 'hsl(var(--border-card))' }}
                  >
                    <div className="feature-icon w-12 h-12 rounded-2xl flex items-center justify-center mb-6 mx-auto bg-brand-gradient">
                      {feature.icon}
                    </div>
                    <h3 className="text-lg font-semibold text-center mb-3" style={{ color: 'hsl(var(--foreground))' }}>
                      {feature.title}
                    </h3>
                    <p className="text-sm text-center leading-relaxed" style={{ color: 'hsl(var(--text-secondary))' }}>
                      {feature.description}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Quick Start */}
      <div ref={quickStartView.ref} className={`scroll-fade-up${quickStartView.inView ? ' in-view' : ''}`}>
        <LandingQuickStartSection />
      </div>

      {/* Popular Downloads Section */}
      <section ref={popularView.ref} className={`relative z-10 w-full py-20 md:py-24 px-6 scroll-fade-up${popularView.inView ? ' in-view' : ''}`}>
        <div className="max-w-6xl mx-auto space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-3xl font-bold tracking-tight mb-2" style={{ color: 'hsl(var(--foreground))' }}>
                {t('home.popularTitle')}
              </h2>
              <p style={{ color: 'hsl(var(--text-secondary))' }}>{t('home.popularDescription')}</p>
            </div>
            <Button
              variant="ghost"
              onClick={() => navigate({ to: '/search', search: { q: '', sort: 'downloads', page: 0, starredOnly: false } })}
            >
              {t('home.viewAll')}
            </Button>
          </div>
          {isLoadingPopular ? (
            <SkeletonList count={6} />
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
              {popularSkills?.items.map((skill, idx) => (
                <div key={skill.id} className={`animate-fade-up delay-${Math.min(idx + 1, 6)}`}>
                  <SkillCard
                    skill={skill}
                    onClick={() => handleSkillClick(skill.namespace, skill.slug)}
                  />
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* Latest Releases Section */}
      <section ref={latestView.ref} className={`relative z-10 w-full py-20 md:py-24 px-6 scroll-fade-up${latestView.inView ? ' in-view' : ''}`}>
        <div className="max-w-6xl mx-auto space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-3xl font-bold tracking-tight mb-2" style={{ color: 'hsl(var(--foreground))' }}>
                {t('home.latestTitle')}
              </h2>
              <p style={{ color: 'hsl(var(--text-secondary))' }}>{t('home.latestDescription')}</p>
            </div>
            <Button
              variant="ghost"
              onClick={() => navigate({ to: '/search', search: { q: '', sort: 'newest', page: 0, starredOnly: false } })}
            >
              {t('home.viewAll')}
            </Button>
          </div>
          {isLoadingLatest ? (
            <SkeletonList count={6} />
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
              {latestSkills?.items.map((skill, idx) => (
                <div key={skill.id} className={`animate-fade-up delay-${Math.min(idx + 1, 6)}`}>
                  <SkillCard
                    skill={skill}
                    onClick={() => handleSkillClick(skill.namespace, skill.slug)}
                  />
                </div>
              ))}
            </div>
          )}
        </div>
      </section>
    </div>
  )
}
