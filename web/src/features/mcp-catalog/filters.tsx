import { useTranslation } from 'react-i18next'
import { useMcpCategories } from '@/shared/hooks/use-mcp-queries'
import { cn } from '@/shared/lib/utils'

interface McpFiltersProps {
  category?: string
  sort: string
  onCategoryChange: (category: string | undefined) => void
  onSortChange: (sort: string) => void
}

export function McpFilters({ category, sort, onCategoryChange, onSortChange }: McpFiltersProps) {
  const { t } = useTranslation()
  const { data: categories } = useMcpCategories()

  return (
    <div className="flex flex-wrap items-center gap-3">
      <div className="flex flex-wrap items-center gap-2">
        <FilterChip
          active={!category}
          label={t('mcp.filters.all')}
          onClick={() => onCategoryChange(undefined)}
        />
        {(categories ?? []).map((item) => (
          <FilterChip
            key={item.id}
            active={category === item.slug}
            label={item.displayName}
            onClick={() => onCategoryChange(item.slug)}
          />
        ))}
      </div>

      <div className="ml-auto flex items-center gap-2 text-sm text-muted-foreground">
        <span>{t('mcp.sort.label')}</span>
        <select
          value={sort}
          onChange={(event) => onSortChange(event.target.value)}
          className="rounded-lg border border-border/60 bg-background px-2.5 py-1.5 text-sm text-foreground"
          aria-label={t('mcp.sort.label')}
        >
          <option value="updated">{t('mcp.sort.updated')}</option>
          <option value="name">{t('mcp.sort.name')}</option>
        </select>
      </div>
    </div>
  )
}

function FilterChip({ active, label, onClick }: { active: boolean; label: string; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'rounded-full border px-3 py-1 text-xs transition-colors',
        active
          ? 'border-primary bg-primary/10 text-primary'
          : 'border-border/60 bg-card text-muted-foreground hover:border-primary/40 hover:text-foreground',
      )}
    >
      {label}
    </button>
  )
}
