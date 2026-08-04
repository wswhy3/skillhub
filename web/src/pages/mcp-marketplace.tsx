import { startTransition, useEffect, useRef, useState } from 'react'
import { useNavigate, useSearch } from '@tanstack/react-router'
import { useTranslation } from 'react-i18next'
import { Loader2 } from 'lucide-react'
import { McpCard } from '@/features/mcp-catalog/mcp-card'
import { McpFilters } from '@/features/mcp-catalog/filters'
import { SkeletonList } from '@/shared/components/skeleton-loader'
import { EmptyState } from '@/shared/components/empty-state'
import { useMcpServers } from '@/shared/hooks/use-mcp-queries'
import { Button } from '@/shared/ui/button'
import { APP_SHELL_PAGE_CLASS_NAME } from '@/app/page-shell-style'

/**
 * MCP 广场：公共目录列表。
 * 关键词、分类、排序写入 URL；无限加载 + “加载更多”按钮作为无障碍/失败回退。
 */
export function McpMarketplacePage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const searchParams = useSearch({ from: '/mcp' })

  const keyword = typeof searchParams.keyword === 'string' ? searchParams.keyword : ''
  const category = typeof searchParams.category === 'string' && searchParams.category ? searchParams.category : undefined
  const sort = typeof searchParams.sort === 'string' && searchParams.sort ? searchParams.sort : 'updated'

  const [keywordInput, setKeywordInput] = useState(keyword)
  useEffect(() => {
    setKeywordInput(keyword)
  }, [keyword])

  const debounceRef = useRef<number | undefined>(undefined)
  const handleKeywordChange = (value: string) => {
    setKeywordInput(value)
    window.clearTimeout(debounceRef.current)
    debounceRef.current = window.setTimeout(() => {
      startTransition(() => {
        navigate({ to: '/mcp', search: { keyword: value, category, sort }, replace: true })
      })
    }, 250)
  }

  const serversQuery = useMcpServers({ keyword: keyword || undefined, category, sort })
  const servers = serversQuery.data?.pages.flatMap((page) => page.items) ?? []

  // 无限加载：底部 IntersectionObserver
  const sentinelRef = useRef<HTMLDivElement | null>(null)
  useEffect(() => {
    const sentinel = sentinelRef.current
    if (!sentinel) return
    if (!serversQuery.hasNextPage || serversQuery.isFetchingNextPage) return
    const observer = new IntersectionObserver((entries) => {
      if (entries[0]?.isIntersecting) {
        serversQuery.fetchNextPage()
      }
    })
    observer.observe(sentinel)
    return () => observer.disconnect()
  }, [serversQuery])

  return (
    <div className={APP_SHELL_PAGE_CLASS_NAME}>
      <header className="space-y-2">
        <h1 className="font-heading text-3xl font-bold tracking-tight text-foreground">
          {t('mcp.title')}
        </h1>
        <p className="text-sm text-muted-foreground">{t('mcp.subtitle')}</p>
      </header>

      <div className="flex flex-col gap-4">
        <input
          type="search"
          value={keywordInput}
          onChange={(event) => handleKeywordChange(event.target.value)}
          placeholder={t('mcp.searchPlaceholder')}
          aria-label={t('mcp.searchPlaceholder')}
          className="w-full rounded-xl border border-border/60 bg-background px-4 py-2.5 text-sm text-foreground"
        />
        <McpFilters
          category={category}
          sort={sort}
          onCategoryChange={(next) =>
            navigate({ to: '/mcp', search: { keyword, category: next, sort }, replace: true })
          }
          onSortChange={(next) =>
            navigate({ to: '/mcp', search: { keyword, category, sort: next }, replace: true })
          }
        />
      </div>

      {serversQuery.isLoading ? (
        <SkeletonList count={6} />
      ) : serversQuery.isError ? (
        <div className="rounded-2xl border border-destructive/30 bg-destructive/5 px-5 py-6 text-sm text-destructive">
          <p>{t('mcp.loadError')}</p>
          <Button variant="outline" size="sm" className="mt-3" onClick={() => serversQuery.refetch()}>
            {t('mcp.retry')}
          </Button>
        </div>
      ) : servers.length === 0 ? (
        <EmptyState title={t('mcp.empty.title')} description={t('mcp.empty.description')} />
      ) : (
        <>
          <div className="grid grid-cols-1 gap-5 md:grid-cols-2 lg:grid-cols-3">
            {servers.map((server) => (
              <McpCard key={server.id} server={server} />
            ))}
          </div>

          <div ref={sentinelRef} className="flex justify-center py-4">
            {serversQuery.isFetchingNextPage ? (
              <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
            ) : serversQuery.hasNextPage ? (
              <Button
                variant="outline"
                size="sm"
                onClick={() => serversQuery.fetchNextPage()}
              >
                {t('mcp.loadMore')}
              </Button>
            ) : null}
          </div>
        </>
      )}
    </div>
  )
}
