import { useParams } from '@tanstack/react-router'
import { useTranslation } from 'react-i18next'
import { ExternalLink } from 'lucide-react'
import { MarkdownRenderer } from '@/shared/components/markdown-renderer'
import { SkeletonList } from '@/shared/components/skeleton-loader'
import { EmptyState } from '@/shared/components/empty-state'
import { CopyButton } from '@/shared/components/copy-button'
import { useMcpDetail, useMcpReadme } from '@/shared/hooks/use-mcp-queries'
import { APP_SHELL_PAGE_CLASS_NAME } from '@/app/page-shell-style'

/**
 * MCP 详情页：@global 命名空间下的 MCP Server 快照。
 */
export function McpDetailPage() {
  const { t } = useTranslation()
  const { slug } = useParams({ from: '/mcp/$slug' })
  const namespace = 'global'

  const detailQuery = useMcpDetail(namespace, slug)
  const readmeQuery = useMcpReadme(namespace, slug, detailQuery.isSuccess)

  if (detailQuery.isLoading) {
    return (
      <div className={APP_SHELL_PAGE_CLASS_NAME}>
        <SkeletonList count={3} />
      </div>
    )
  }

  if (detailQuery.isError || !detailQuery.data) {
    return (
      <div className={APP_SHELL_PAGE_CLASS_NAME}>
        <EmptyState title={t('mcp.detail.notFound')} description={t('mcp.detail.notFoundDescription')} />
      </div>
    )
  }

  const detail = detailQuery.data

  return (
    <div className={APP_SHELL_PAGE_CLASS_NAME}>
      <header className="space-y-3">
        <div className="flex items-start gap-4">
          {detail.iconUrl ? (
            <img
              src={detail.iconUrl}
              alt=""
              className="h-14 w-14 rounded-xl border border-border/60 object-cover"
              referrerPolicy="no-referrer"
            />
          ) : (
            <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-secondary/60 text-xl font-semibold text-muted-foreground">
              {detail.displayName.slice(0, 1).toUpperCase()}
            </div>
          )}
          <div className="min-w-0 flex-1">
            <h1 className="font-heading text-3xl font-bold tracking-tight text-foreground">
              {detail.displayName}
            </h1>
            <p className="text-sm text-muted-foreground">{detail.coordinate}</p>
          </div>
          {detail.upstreamVersion && (
            <span className="rounded-full border border-border/60 px-3 py-1 text-xs text-muted-foreground">
              v{detail.upstreamVersion}
            </span>
          )}
        </div>
        <p className="max-w-3xl text-[15px] leading-7 text-foreground/85">{detail.summary}</p>
      </header>

      <section className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <div className="min-w-0 rounded-xl border border-border/60 bg-card/60 p-6">
          {readmeQuery.isLoading ? (
            <SkeletonList count={2} />
          ) : readmeQuery.isError ? (
            <p className="text-sm text-destructive">{t('mcp.detail.readmeError')}</p>
          ) : (
            <MarkdownRenderer content={readmeQuery.data ?? ''} />
          )}
        </div>

        <aside className="space-y-6">
          <div className="rounded-xl border border-border/60 bg-card/60 p-5">
            <h2 className="mb-3 text-sm font-semibold text-foreground">{t('mcp.detail.about')}</h2>
            <dl className="space-y-2 text-sm">
              {detail.publisherName && (
                <MetaRow label={t('mcp.detail.publisher')} value={detail.publisherName} href={detail.publisherUrl} />
              )}
              {detail.homepageUrl && (
                <MetaRow label={t('mcp.detail.homepage')} value={detail.homepageUrl} href={detail.homepageUrl} link />
              )}
              {detail.repositoryUrl && (
                <MetaRow label={t('mcp.detail.repository')} value={detail.repositoryUrl} href={detail.repositoryUrl} link />
              )}
              {detail.documentationUrl && (
                <MetaRow label={t('mcp.detail.docs')} value={detail.documentationUrl} href={detail.documentationUrl} link />
              )}
              {(detail.licenseSpdx || detail.licenseName) && (
                <MetaRow
                  label={t('mcp.detail.license')}
                  value={detail.licenseSpdx ?? detail.licenseName ?? ''}
                  href={detail.licenseUrl}
                />
              )}
              {detail.sourceRevision && (
                <MetaRow label={t('mcp.detail.sourceRevision')} value={detail.sourceRevision} />
              )}
            </dl>
          </div>

          {detail.categories.length > 0 && (
            <div className="rounded-xl border border-border/60 bg-card/60 p-5">
              <h2 className="mb-3 text-sm font-semibold text-foreground">{t('mcp.detail.categories')}</h2>
              <div className="flex flex-wrap gap-2">
                {detail.categories.map((category) => (
                  <span key={category.id} className="rounded-full bg-primary/10 px-3 py-1 text-xs text-primary">
                    {category.displayName}
                  </span>
                ))}
              </div>
            </div>
          )}

          <div className="rounded-xl border border-border/60 bg-card/60 p-5">
            <h2 className="mb-3 text-sm font-semibold text-foreground">{t('mcp.detail.coordinate')}</h2>
            <div className="flex items-center gap-2">
              <code className="flex-1 break-all rounded-lg bg-secondary/50 px-3 py-2 text-xs">{detail.coordinate}</code>
              <CopyButton text={detail.coordinate} ariaLabel={t('mcp.detail.copyCoordinate')} />
            </div>
          </div>
        </aside>
      </section>
    </div>
  )
}

function MetaRow({ label, value, href, link }: { label: string; value: string; href?: string; link?: boolean }) {
  return (
    <div className="flex flex-col gap-0.5">
      <dt className="text-xs uppercase tracking-wide text-muted-foreground">{label}</dt>
      <dd className="break-words text-foreground/85">
        {link && href ? (
          <a
            href={href}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1 text-primary underline decoration-primary/30 underline-offset-4 hover:text-primary/80"
          >
            <span className="break-all">{value}</span>
            <ExternalLink className="h-3 w-3 shrink-0" />
          </a>
        ) : href ? (
          <a
            href={href}
            target="_blank"
            rel="noopener noreferrer"
            className="text-primary underline decoration-primary/30 underline-offset-4 hover:text-primary/80"
          >
            {value}
          </a>
        ) : (
          value
        )}
      </dd>
    </div>
  )
}
