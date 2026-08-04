import { useNavigate } from '@tanstack/react-router'
import { Card } from '@/shared/ui/card'
import type { McpServerSummary } from '@/api/types'

interface McpCardProps {
  server: McpServerSummary
}

export function McpCard({ server }: McpCardProps) {
  const navigate = useNavigate()

  const goToDetail = () => {
    navigate({ to: '/mcp/$slug', params: { slug: server.slug } })
  }

  return (
    <Card
      role="link"
      tabIndex={0}
      onClick={goToDetail}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault()
          goToDetail()
        }
      }}
      className="flex h-full cursor-pointer flex-col gap-3 rounded-xl border bg-card p-5 transition-colors hover:border-primary/50"
    >
      <div className="flex items-start gap-3">
        {server.iconUrl ? (
          <img
            src={server.iconUrl}
            alt=""
            className="h-10 w-10 shrink-0 rounded-lg border border-border/60 object-cover"
            loading="lazy"
            referrerPolicy="no-referrer"
          />
        ) : (
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-secondary/60 text-sm font-semibold text-muted-foreground">
            {server.displayName.slice(0, 1).toUpperCase()}
          </div>
        )}
        <div className="min-w-0 flex-1">
          <h3 className="truncate font-heading text-base font-semibold text-foreground">
            {server.displayName}
          </h3>
          <p className="truncate text-xs text-muted-foreground">{server.coordinate}</p>
        </div>
      </div>

      <p className="line-clamp-2 text-sm text-foreground/80">{server.summary}</p>

      <div className="mt-auto flex flex-wrap items-center gap-2 pt-1">
        {server.publisherName && (
          <span className="rounded-full bg-secondary/60 px-2.5 py-0.5 text-xs text-muted-foreground">
            {server.publisherName}
          </span>
        )}
        {server.upstreamVersion && (
          <span className="rounded-full border border-border/60 px-2.5 py-0.5 text-xs text-muted-foreground">
            v{server.upstreamVersion}
          </span>
        )}
        {server.categories.slice(0, 3).map((category) => (
          <span
            key={category.id}
            className="rounded-full bg-primary/10 px-2.5 py-0.5 text-xs text-primary"
          >
            {category.displayName}
          </span>
        ))}
      </div>
    </Card>
  )
}
