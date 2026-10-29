import { useEffect, useRef } from 'react'
import gsap from 'gsap'

/**
 * 首屏粒子变形文字层：CHANGHONG IT ↔ SKILLHUB 由粒子流衔接的循环变形。
 * fixed 前景层仅首屏可见（滚出渐隐），形态不随滚动变化。
 *
 * 减法后的动效清单（用户拍板）：
 * - 保留：入场动画、波浪呼吸、能量扫描脉冲、粒子变形流
 * - 移除：字母级变形（散开/旋转/缩放/模糊）——字母只做整体淡入淡出，
 *   形态转换完全交给粒子流，视觉语言单一不杂乱
 * - 形态不随滚动变化（历史：曾按全页滚动驱动，已取消）
 *
 * 架构（纯函数式，杜绝状态污染）：
 * - 两组静态字母同时渲染：A=CHANGHONG IT（12 字符），B=SKILLHUB（8 字符）
 * - 所有字母 x 属性 = 0，位置由布局公式计算（layoutX 对称居中）
 * - 不使用 gsap.timeline：raf 循环里按相位纯数学插值，每帧 gsap.set 写入
 * - 变形相位 = 纯时间函数（入场延迟后启动，mod 1 无缝循环）；
 *   滚动只控制容器整体渐隐（滚出首屏），不驱动形态
 *
 * 【坑：不可再用 timeline + fromTo 做 scrub】（历史 bug 记录）
 * 1. fromTo 默认 immediateRender，后创建的 tween 的 from 态会在创建时覆盖初始状态
 * 2. seek 到 tween 起点之前，GSAP 不会把该 tween 重渲回 from 态
 * → 状态依赖渲染历史。任何相位下的状态必须是纯函数。
 *
 * 【历史方案记录】
 * - 曾按全页滚动 0→1 驱动变形（"贯穿整个页面"）：节奏与章节无关、干扰阅读，已取消
 * - 曾有字母级变形（散开/旋转/缩放/模糊 dematerialize）：与粒子流叠加显杂乱，已移除
 *
 * 一轮变形周期（相位 0→1，cycle 10s，A→B 快 / B→A 慢不对称）：
 * - 0.00~0.24  A 稳定显示（扫描脉冲完整扫过一轮，扫完 IT 末字母接爆散）
 * - 0.24~0.32  A 淡出 + 粒子爆出
 * - 0.32~0.46  粒子飞行
 * - 0.46~0.54  B 淡入 + 粒子汇入
 * - 0.54~0.78  B 稳定显示（扫描脉冲完整扫过一轮）
 * - 0.78~0.86  B 淡出 + 粒子爆出
 * - 0.86~1.00  粒子飞行（从容）+ A 淡入，循环闭合（p=0 ≡ p=1）
 */

const WORD_A = 'CHANGHONG IT'
const WORD_B = 'SKILLHUB'

// viewBox: 0 0 1200 300，水平中心 600，文字基线 y=150
const CX = 600
const BASELINE = 150
// 字母间距
const SPACING = 70

/** 布局位置：基于单词长度对称居中（保证任何单词都水平居中） */
function layoutX(index: number, total: number) {
  return CX + (index - (total - 1) / 2) * SPACING
}

const lerp = (from: number, to: number, t: number) => from + (to - from) * t
/** 区间归一化进度（0~1，区间外钳制） */
const seg = (p: number, start: number, end: number) =>
  Math.min(Math.max((p - start) / (end - start), 0), 1)

// --- 首次入场（纯时间函数，挂载后播一次） ---
const INTRO_DUR = 0.75 // 单字母入场时长（秒）
const INTRO_STAGGER = 0.05 // 逐字错峰间隔（秒）
const INTRO_RISE = 60 // 起始下沉量（从下方上浮）
const INTRO_BLUR = 12 // 起始模糊
const INTRO_SCALE_BOOST = 0.2 // 起始额外放大比例
// --- 能量扫描脉冲（唯一保留的稳定期动效）：一个能量团从词左扫到右 ---
// 扫到的字母高斯权重内上抬 + 放大，像能量涌过。纯 raf transform 叠加，零额外渲染开销。
// 【坑：字母波浪上下跳动（IDLE_AMP 正弦漂浮）已按需求移除——与粒子变形叠加显乱；
//   字母稳定期除扫描脉冲外完全静止】
// 【坑：扫描必须绑定循环相位（见 applyState），不能用独立时间时钟——否则与变形不同步，
//   脉冲没扫完字母就爆散。周期 = 稳定期窗口长度（A: 0.24 / B: 0.24 相位 ≈ 2.4s @10s 循环）】
const SWEEP_SIGMA = 90 // 高斯能量团宽度（viewBox units，σ）
const SWEEP_RISE = 12 // 扫过时字母上抬量
const SWEEP_SCALE = 0.16 // 扫过时字母放大比例
// 扫描轨迹：从词左外扫到词右外（稳定期窗口的 0→1）
const SWEEP_X_FROM = -120
const SWEEP_X_TO = 1320

/** 确定性伪随机 [0,1)：hash(n)，纯函数（禁 Math.random，保持状态确定性） */
function hash01(n: number): number {
  const s = Math.sin(n * 127.1 + 311.7) * 43758.5453
  return s - Math.floor(s)
}
// --- 自动变形循环（首屏静止时文字自己播放 CHANGHONGIT↔SKILLHUB 完整变形） ---
// 形态不再跟随滚动（历史：曾按全页滚动 0→1 驱动，节奏与章节无关且干扰阅读，已按需求取消）。
// 循环相位是纯时间函数：tc = ((t - delay) / cycle) mod 1，p=0≡p=1 保证循环无缝。
// 滚动出首屏时整个字母层渐隐（见 tick 中容器淡出），滚回顶部恢复。
const AUTO_IDLE_DELAY = 1.8 // 入场后等多久开始第一轮（秒）——入场动画 ~1.35s 播完即接扫描
const AUTO_CYCLE = 10 // 一轮完整变形（秒）。A→B 段快、B 稳定与回归段慢（不对称分配，见下方时序）
// --- 粒子变换（变形的过渡中介：字母碎成光点飞舞，再汇聚成新词） ---
// 每字母 6 个粒子，共 20×6=120 个小 circle；粒子不带 blur filter（每帧仅 transform/opacity，
// 无滤镜重绘），字母过渡淡出的窗口正好由粒子流补位——视觉上"字碎成了光"。
const P_PER_LETTER = 6
const P_COUNT = 20 * P_PER_LETTER // A 12 + B 8 字母
const P_SCATTER_X = 170 // 散点横向偏移幅度（viewBox units）
const P_SCATTER_Y = 100 // 散点纵向偏移幅度
const P_DRIFT = 8 // 中段漂移幅度（粒子飞行途中自身轻浮动）
// 粒子用纯色（三色系随机），比渐变填充更省
const P_COLORS = ['#6A6DFF', '#C77EFF', '#22D3EE'] as const

interface LetterState {
  opacity: number
}

/**
 * A 组字母（CHANGHONG IT）：稳定显示（含完整扫描脉冲）→(0.24~0.32) 淡出交给粒子
 * →(0.86~1.00) 淡入接回粒子（回归段从宽，B→A 从容）。
 * 字母不做散开/旋转/缩放变形，形态转换完全由粒子流衔接。
 */
function aState(p: number, ease: (t: number) => number): LetterState {
  const tOut = ease(seg(p, 0.24, 0.32))
  const tIn = ease(seg(p, 0.86, 1))
  // 两段区间不重叠（0.32 < 0.86），顺序复合安全：先淡出，再淡入
  let opacity = lerp(1, 0, tOut)
  opacity = lerp(opacity, 1, tIn)
  return { opacity }
}

/** B 组字母（SKILLHUB）：隐藏 →(0.46~0.54) 淡入接回粒子 →(0.78~0.86) 淡出交给粒子 */
function bState(p: number, ease: (t: number) => number): LetterState {
  const tIn = ease(seg(p, 0.46, 0.54))
  const tOut = ease(seg(p, 0.78, 0.86))
  // 两段区间不重叠（0.54 < 0.78），顺序复合安全：B 有 0.54~0.78 完整稳定期
  let opacity = lerp(0, 1, tIn)
  opacity = lerp(opacity, 0, tOut)
  return { opacity }
}

interface ParticleState {
  x: number
  y: number
  opacity: number
}

/**
 * 粒子 j 在循环相位 p 的状态（纯函数）。
 * 两段飞行与字母淡入淡出窗口互补（字母淡出时粒子出现，字母淡入时粒子消失）：
 * - 段1 A→B（快）：p ∈ [0.24, 0.54]  爆出(0.24~0.34) → 漂移(0.34~0.42) → 汇入 B 位(0.42~0.54)
 * - 段2 B→A（慢）：p ∈ [0.78, 1.00]  爆出(0.78~0.88) → 漂移(0.88~0.94) → 汇入 A 位(0.94~1.00)
 * 粒子起点/终点分散到各字母位（j 取模映射），散点由 (j, 段) 哈希决定，全程确定性。
 */
function particleState(j: number, p: number, t: number, ease: (v: number) => number): ParticleState {
  const seg1 = p >= 0.24 && p < 0.66
  const seg2 = p >= 0.78
  if (!seg1 && !seg2) return { x: 0, y: 0, opacity: 0 }
  const fromB = seg2 // 段2 从 B 飞向 A
  // 源/目标字母位（j 取模铺开到各字母，段2 用错位映射避免粒子原路返回）
  const aIdx = j % 12
  const bIdx = (j + 3) % 8
  const srcX = fromB ? layoutX(bIdx, 8) : layoutX(aIdx, 12)
  const dstX = fromB ? layoutX((aIdx + 5) % 12, 12) : layoutX(bIdx, 8)
  const h = (salt: number) => hash01(j * 37 + salt + (fromB ? 991 : 0))
  const srcY = BASELINE + (h(1) - 0.5) * 30
  const dstY = BASELINE + (h(2) - 0.5) * 30
  // 散点：源目标中点 + 大幅哈希偏移
  const scX = (srcX + dstX) / 2 + (h(3) - 0.5) * 2 * P_SCATTER_X
  const scY = (srcY + dstY) / 2 + (h(4) - 0.5) * 2 * P_SCATTER_Y
  const eOut = ease(seg(p, fromB ? 0.78 : 0.24, fromB ? 0.88 : 0.34))
  const eIn = ease(seg(p, fromB ? 0.94 : 0.42, fromB ? 1 : 0.54))
  const mid = seg(p, fromB ? 0.88 : 0.34, fromB ? 0.94 : 0.42)
  // 爆出 → 中段正弦漂移 → 汇入
  let x = lerp(srcX, scX, eOut)
  let y = lerp(srcY, scY, eOut)
  const drift = Math.sin(t * 2.5 + j * 0.9) * P_DRIFT * Math.sin(mid * Math.PI)
  x += drift * 0.7
  y += drift
  x = lerp(x, dstX, eIn)
  y = lerp(y, dstY, eIn)
  const opacity = Math.sin(Math.PI * seg(p, fromB ? 0.78 : 0.24, fromB ? 1 : 0.54))
  return { x, y, opacity }
}

interface MorphBgProps {
  /** 滚动进度 0~1，由父组件写入 */
  progressRef: React.MutableRefObject<number>
}

export function MorphBg({ progressRef }: MorphBgProps) {
  const aRefs = useRef<(SVGTextElement | null)[]>([])
  const bRefs = useRef<(SVGTextElement | null)[]>([])
  const pRefs = useRef<(SVGCircleElement | null)[]>([])
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const aLetters = aRefs.current.filter(Boolean) as SVGTextElement[]
    const bLetters = bRefs.current.filter(Boolean) as SVGTextElement[]
    const particles = pRefs.current.filter(Boolean) as SVGCircleElement[]
    const container = containerRef.current
    if (aLetters.length === 0 || bLetters.length === 0) return

    const ease = gsap.parseEase('power2.inOut') as (t: number) => number
    const introEase = gsap.parseEase('power3.out') as (t: number) => number
    // 入场起点：挂载时刻（timeorigin 与 raf 时间戳一致）
    const start = performance.now()

    // 每帧按相位纯函数计算并写入状态，无 tween 状态机、无历史依赖。
    // 入场效果是纯时间函数（f 仅随挂载后流逝时间增长，播完恒为 1）。
    // 字母位置恒为布局位（layoutX，波浪/扫描只做小幅叠加），变形只动 opacity——形态交给粒子。
    const applyState = (
      letters: SVGTextElement[],
      stateAt: (i: number, p: number) => LetterState,
      total: number,
      pEff: number,
      now: number,
    ) => {
      const t = (now - start) / 1000
      letters.forEach((el, i) => {
        const s = stateAt(i, pEff)
        // 首次入场：错峰（按索引延迟）上浮 + 模糊消散 + 轻微放大落定
        const f = introEase(Math.min(Math.max((t - i * INTRO_STAGGER) / INTRO_DUR, 0), 1))
        // 能量扫描脉冲（绑定循环相位，非独立时钟）：稳定期内恰好完整扫一轮——
        // A 稳定期 p∈[0,0.24] 从词左外扫到词右外（扫完 IT 末字母正好接 0.24 爆散）；
        // B 稳定期 p∈[0.54,0.78] 同理。稳定期外脉冲停在词外（高斯权重≈0，字母静止）。
        // 【坑：曾用独立时间时钟（t/SWEEP_PERIOD），与变形循环不同步——脉冲扫到一半
        //   字母就开始爆散，CHANGHONG 还没扫到 IT 就碎了。必须相位绑定保证顺序感】
        const stable: [number, number] = total === 12 ? [0, 0.24] : [0.54, 0.78]
        const q = seg(pEff, stable[0], stable[1])
        const sweepCx = lerp(SWEEP_X_FROM, SWEEP_X_TO, q)
        const lx = layoutX(i, total)
        const dw = lx - sweepCx
        const w = Math.exp(-(dw * dw) / (2 * SWEEP_SIGMA * SWEEP_SIGMA))
        const y = (1 - f) * INTRO_RISE - w * SWEEP_RISE
        const rotation = 0
        const scale = 1 + (1 - f) * INTRO_SCALE_BOOST + w * SWEEP_SCALE
        const opacity = s.opacity * f
        const blur = (1 - f) * INTRO_BLUR
        gsap.set(el, { x: lx, y, rotation, scale, opacity })
        // 入场模糊消散：纯 CSS blur（不叠辉光滤镜，稳定态 filter 清空、零滤镜开销）
        if (blur > 0.05) {
          el.style.filter = `blur(${blur.toFixed(2)}px)`
        } else {
          el.style.filter = ''
        }
      })
    }

    // --- 变形循环驱动（纯时间函数，见 AUTO_ 常量注释；形态不随滚动变化） ---
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches

    let rafId: number
    const tick = (now: number = performance.now()) => {
      const scrollP = progressRef.current
      const t = (now - start) / 1000
      // 滚动出首屏：整个字母层渐隐（不抢内容阅读）；滚回顶部恢复。
      // 写外层容器 opacity（morph-breath 动画在 svg 上，两者不冲突）。
      if (container) {
        container.style.opacity = (1 - seg(scrollP, 0.02, 0.14)).toFixed(3)
      }
      // 变形相位：入场延迟后启动，纯时间循环；reduced 用户停在 A 稳定态
      const tc =
        reduced || t < AUTO_IDLE_DELAY
          ? 0
          : ((t - AUTO_IDLE_DELAY) / AUTO_CYCLE) % 1
      applyState(aLetters, () => aState(tc, ease), aLetters.length, tc, now)
      applyState(bLetters, () => bState(tc, ease), bLetters.length, tc, now)
      // 粒子流：与字母过渡窗口互补（字母淡出→粒子飞舞→新字母淡入）。
      // 窗口外（稳定期，约 40% 时间）直接跳过，省 120 次无效 DOM 写入
      particles.forEach((el, j) => {
        const s = particleState(j, tc, t, ease)
        if (s.opacity <= 0) return
        gsap.set(el, { x: s.x, y: s.y, opacity: s.opacity })
      })
      rafId = requestAnimationFrame(tick)
    }
    // 立即同步渲染一帧（t=0 时字母 opacity=0，与 SSR 初始态一致，无闪帧），再进入 raf 循环
    tick()

    return () => {
      cancelAnimationFrame(rafId)
    }
  }, [progressRef])

  const renderLetters = (word: string, refs: React.MutableRefObject<(SVGTextElement | null)[]>) =>
    word.split('').map((ch, i) => (
      <text
        key={`${word}-${i}`}
        ref={(el) => { refs.current[i] = el }}
        x={0}
        y={BASELINE}
        textAnchor="middle"
        fill="url(#morphGradient)"
        style={{
          fontFamily: 'system-ui, -apple-system, sans-serif',
          fontSize: '48px',
          fontWeight: 800,
          letterSpacing: '0.05em',
          // SSR 初始隐藏，水合后首帧 tick() 立即摆好，避免闪一帧堆叠字
          opacity: 0,
        }}
      >
        {ch === ' ' ? '\u00A0' : ch}
      </text>
    ))

  // 粒子层：小圆点，静态半径/颜色（哈希确定），位置与透明度由 raf 驱动（见 particleState）
  const renderParticles = () =>
    Array.from({ length: P_COUNT }, (_, j) => (
      <circle
        key={`pt-${j}`}
        ref={(el) => { pRefs.current[j] = el }}
        r={2 + hash01(j * 3) * 1.5}
        fill={P_COLORS[Math.floor(hash01(j * 7) * P_COLORS.length)]}
        opacity={0}
      />
    ))

  return (
    <div
      ref={containerRef}
      // 文字固定在导航栏下方，与 landing.tsx 首屏 pt-[27vh] 配套（1vh≈10.8px@1080p，两处同步增减）
      // z-20 前景穿透层：位于内容(z-10)之上、导航栏(z-50)之下；pointer-events-none 不挡交互。
      // 仅首屏展示：raf 按滚动进度写本容器 opacity（滚出首屏渐隐，形态不随滚动变化）。
      // 【坑：之前 z-0 放内容背后，往下滑后字母被内容完全遮挡，变换效果看不见】
      className="fixed inset-0 z-20 pointer-events-none flex items-start justify-center overflow-hidden pt-[16vh]"
      aria-hidden="true"
    >
      <svg
        viewBox="0 0 1200 300"
        // 高度用 vh 固定：字母中心恒在 7vh + 13vh = 20vh，与视口宽度解耦（meet 缩放居中）
        // morph-breath：整体透明度 0.80↔0.92 缓慢脉动（合成层动画，GPU 零成本），
        // 辉光滤镜已移除（清晰度优先），数值同步见 index.css
        className="w-full max-w-5xl h-[26vh] morph-breath"
        style={{ overflow: 'visible' }}
      >
        <defs>
          {/* 静态三色渐变（靛蓝→品红→青）。
              颜色比品牌色加深提饱和：白底上浅中间调显灰淡（用户反馈"太淡不显眼"），
              #5B5EFF/#C15EFF/#0BB8D6 三个 stop 在浅背景上有明确色彩存在感。
              【坑：曾尝试 SMIL 动画 stop-color 做"颜色流光"——stop-color 连续变化会让
              整个文字层每帧重绘，headed 实测帧率 144→23fps，不可接受，已移除。
              颜色层面的动效一律不要做在 SVG paint 属性上】 */}
          <linearGradient id="morphGradient" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="#5B5EFF" />
            <stop offset="50%" stopColor="#C15EFF" />
            <stop offset="100%" stopColor="#0BB8D6" />
          </linearGradient>
          {/* 【坑：曾用 feGaussianBlur 双层辉光滤镜（小晕+大晕叠字下）营造发光——白底上
              模糊晕直接糊住字边缘，用户反馈"投影发光不清晰"已移除。字母纯渐变填充
              锐利渲染，清晰度优先；氛围感由粒子流 + 渐变色承担，不再靠滤镜】 */}
        </defs>
        {renderLetters(WORD_A, aRefs)}
        {renderLetters(WORD_B, bRefs)}
        {renderParticles()}
      </svg>
    </div>
  )
}
