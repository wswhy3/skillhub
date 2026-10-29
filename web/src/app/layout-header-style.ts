import { cn } from '@/shared/lib/utils'

export const APP_HEADER_BASE_CLASS_NAME =
  // bg-[hsl(var(--card))]：浅色 = 白（原样），首页 dark 下 = 深蓝黑（科技感 + 不突兀）
  'sticky top-0 z-50 flex items-center justify-between border-b bg-[hsl(var(--card))] px-6 py-4 transition-shadow duration-200 md:px-12'

export const APP_HEADER_ELEVATED_CLASS_NAME = 'shadow-[0_10px_24px_-20px_rgba(15,23,42,0.32)]'

export function getAppHeaderClassName(isElevated: boolean): string {
  return cn(APP_HEADER_BASE_CLASS_NAME, isElevated && APP_HEADER_ELEVATED_CLASS_NAME)
}
