import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { toast } from '@/shared/lib/toast'
import { Card } from '@/shared/ui/card'
import { Input } from '@/shared/ui/input'
import { Button } from '@/shared/ui/button'
import { Label } from '@/shared/ui/label'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/shared/ui/table'
import { SkeletonList } from '@/shared/components/skeleton-loader'
import { EmptyState } from '@/shared/components/empty-state'
import { useMcpAdminCategories, useMcpCategoryMutations } from '@/shared/hooks/use-mcp-admin-queries'
import { APP_SHELL_PAGE_CLASS_NAME } from '@/app/page-shell-style'

/**
 * MCP 分类管理。
 */
export function AdminMcpCategoriesPage() {
  const { t } = useTranslation()
  const categoriesQuery = useMcpAdminCategories()
  const { create, update } = useMcpCategoryMutations()
  const [slug, setSlug] = useState('')
  const [nameEn, setNameEn] = useState('')
  const [nameZh, setNameZh] = useState('')

  const categories = categoriesQuery.data ?? []

  const handleCreate = () => {
    create.mutate(
      { slug, translations: { en: nameEn, zh: nameZh } },
      {
        onSuccess: () => {
          setSlug(''); setNameEn(''); setNameZh('')
          toast.success(t('mcpAdmin.categories.created'))
        },
        onError: (error) => toast.error(error instanceof Error ? error.message : t('mcpAdmin.categories.createFailed')),
      },
    )
  }

  const handleToggle = (categoryId: number, enabled: boolean) => {
    update.mutate({ categoryId, payload: { enabled: !enabled } }, {
      onSuccess: () => toast.success(t('mcpAdmin.categories.updated')),
      onError: (error) => toast.error(error instanceof Error ? error.message : t('mcpAdmin.categories.updateFailed')),
    })
  }

  return (
    <div className={APP_SHELL_PAGE_CLASS_NAME}>
      <h1 className="font-heading text-2xl font-bold tracking-tight text-foreground">
        {t('mcpAdmin.categories.title')}
      </h1>

      <Card className="space-y-4 p-6">
        <div className="grid gap-4 sm:grid-cols-3">
          <div className="space-y-1.5">
            <Label htmlFor="cat-slug">{t('mcpAdmin.categories.slug')}</Label>
            <Input id="cat-slug" value={slug} onChange={(e) => setSlug(e.target.value)} placeholder="devtools" />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="cat-name-en">{t('mcpAdmin.categories.nameEn')}</Label>
            <Input id="cat-name-en" value={nameEn} onChange={(e) => setNameEn(e.target.value)} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="cat-name-zh">{t('mcpAdmin.categories.nameZh')}</Label>
            <Input id="cat-name-zh" value={nameZh} onChange={(e) => setNameZh(e.target.value)} />
          </div>
        </div>
        <Button onClick={handleCreate} disabled={create.isPending || !slug || !nameEn}>
          {t('mcpAdmin.categories.create')}
        </Button>
      </Card>

      {categoriesQuery.isLoading ? (
        <SkeletonList count={3} />
      ) : categories.length === 0 ? (
        <EmptyState title={t('mcpAdmin.categories.empty')} description={t('mcpAdmin.categories.emptyDescription')} />
      ) : (
        <Card>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t('mcpAdmin.categories.colSlug')}</TableHead>
                <TableHead>{t('mcpAdmin.categories.colName')}</TableHead>
                <TableHead>{t('mcpAdmin.categories.colEnabled')}</TableHead>
                <TableHead className="text-right">{t('mcpAdmin.categories.colActions')}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {categories.map((category) => (
                <TableRow key={category.id}>
                  <TableCell className="font-medium">{category.slug}</TableCell>
                  <TableCell>{category.displayName}</TableCell>
                  <TableCell>{t('mcpAdmin.categories.enabled')}</TableCell>
                  <TableCell className="text-right">
                    <Button size="sm" variant="outline" onClick={() => handleToggle(category.id, true)}>
                      {t('mcpAdmin.categories.toggle')}
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Card>
      )}
    </div>
  )
}
