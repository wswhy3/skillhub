import { useInfiniteQuery, useQuery } from '@tanstack/react-query'
import { mcpApi } from '@/api/client'
import type { McpSearchParams } from '@/api/types'
import { getI18nCacheKey } from './query-keys'

const MCP_STALE_TIME = 60_000

export function useMcpCategories() {
  return useQuery({
    queryKey: ['mcp', 'categories', getI18nCacheKey()],
    queryFn: () => mcpApi.listCategories(),
    staleTime: MCP_STALE_TIME,
  })
}

/**
 * 公共列表：无限加载。排序已由后端 (updatedAt DESC, id DESC) 稳定收口。
 * URL 查询参数是单一事实来源，由页面传入。
 */
export function useMcpServers(params: Omit<McpSearchParams, 'page' | 'size'>) {
  return useInfiniteQuery({
    queryKey: ['mcp', 'servers', params, getI18nCacheKey()],
    queryFn: ({ pageParam }) =>
      mcpApi.searchServers({ ...params, page: pageParam, size: 24 }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => {
      const loaded = (lastPage.page + 1) * lastPage.size
      return loaded < lastPage.total ? lastPage.page + 1 : undefined
    },
    staleTime: MCP_STALE_TIME,
  })
}

export function useMcpDetail(namespace: string, slug: string) {
  return useQuery({
    queryKey: ['mcp', 'detail', namespace, slug, getI18nCacheKey()],
    queryFn: () => mcpApi.getDetail(namespace, slug),
    enabled: !!namespace && !!slug,
    staleTime: MCP_STALE_TIME,
  })
}

export function useMcpReadme(namespace: string, slug: string, enabled = true) {
  return useQuery({
    queryKey: ['mcp', 'readme', namespace, slug, getI18nCacheKey()],
    queryFn: () => mcpApi.getReadme(namespace, slug),
    enabled: enabled && !!namespace && !!slug,
    staleTime: MCP_STALE_TIME,
  })
}
