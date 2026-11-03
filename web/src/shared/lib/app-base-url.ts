/**
 * 获取应用对外的基础 URL（构建 Agent 接入指令用）。
 * 优先运行时配置（非 localhost），否则回退当前页面 origin。
 * SSR / 测试环境（无 window）返回空串。
 */
export function getAppBaseUrl(): string {
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
