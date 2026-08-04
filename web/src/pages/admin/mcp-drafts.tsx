import { useEffect, useState } from 'react'
import { useNavigate, useSearch } from '@tanstack/react-router'
import { useTranslation } from 'react-i18next'
import { toast } from '@/shared/lib/toast'
import { Card } from '@/shared/ui/card'
import { Input } from '@/shared/ui/input'
import { Button } from '@/shared/ui/button'
import { Label } from '@/shared/ui/label'
import { Textarea } from '@/shared/ui/textarea'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/shared/ui/select'
import {
  useMcpAdminCategories,
  useMcpAdminServers,
  useMcpCreateDraft,
  useMcpDraftDetail,
  useMcpDraftReadme,
  useMcpImportDraft,
  useMcpPublishDraft,
  useMcpSaveManualReadme,
  useMcpUpdateDraft,
} from '@/shared/hooks/use-mcp-admin-queries'
import type { McpDraft, McpDraftPayload } from '@/api/types'
import { APP_SHELL_PAGE_CLASS_NAME } from '@/app/page-shell-style'

const EMPTY_FORM = {
  slug: '',
  displayName: '',
  summary: '',
  upstreamVersion: '',
  publisherName: '',
  publisherUrl: '',
  homepageUrl: '',
  repositoryUrl: '',
  documentationUrl: '',
  licenseSpdx: '',
  licenseName: '',
  licenseUrl: '',
  licenseNote: '',
}

/**
 * MCP 运营后台：草稿创建、修改、导入、提交。
 */
export function AdminMcpDraftsPage() {
  const { t } = useTranslation()
  const search = useSearch({ from: '/admin/mcp/drafts' })
  const navigate = useNavigate()
  const [form, setForm] = useState(EMPTY_FORM)
  const [selectedDraftVersionId, setSelectedDraftVersionId] = useState<string>('')
  const [categoryIds, setCategoryIds] = useState<number[]>([])
  const [draft, setDraft] = useState<McpDraft | null>(null)
  const [importSourceType, setImportSourceType] = useState('MANUAL')
  const [importSourceUrl, setImportSourceUrl] = useState('')
  const [manualReadme, setManualReadme] = useState('')

  const serversQuery = useMcpAdminServers()
  const categoriesQuery = useMcpAdminCategories()
  const draftDetailQuery = useMcpDraftDetail(selectedDraftVersionId ? Number(selectedDraftVersionId) : undefined)
  const draftReadmeQuery = useMcpDraftReadme(
    draftDetailQuery.data?.contentStatus === 'READY' ? draftDetailQuery.data.versionId : undefined,
  )
  const createDraft = useMcpCreateDraft()
  const updateDraft = useMcpUpdateDraft()
  const importDraft = useMcpImportDraft()
  const saveManualReadme = useMcpSaveManualReadme()
  const publishDraft = useMcpPublishDraft()

  useEffect(() => {
    if (search.draftVersionId) {
      setSelectedDraftVersionId(String(search.draftVersionId))
    }
  }, [search.draftVersionId])

  useEffect(() => {
    if (!draftDetailQuery.data) return
    const loadedDraft = draftDetailQuery.data
    setDraft(loadedDraft)
    setForm({
      slug: loadedDraft.slug,
      displayName: loadedDraft.displayName,
      summary: loadedDraft.summary,
      upstreamVersion: loadedDraft.upstreamVersion ?? '',
      publisherName: loadedDraft.publisherName ?? '',
      publisherUrl: loadedDraft.publisherUrl ?? '',
      homepageUrl: loadedDraft.homepageUrl ?? '',
      repositoryUrl: loadedDraft.repositoryUrl ?? '',
      documentationUrl: loadedDraft.documentationUrl ?? '',
      licenseSpdx: loadedDraft.licenseSpdx ?? '',
      licenseName: loadedDraft.licenseName ?? '',
      licenseUrl: loadedDraft.licenseUrl ?? '',
      licenseNote: loadedDraft.licenseNote ?? '',
    })
    setCategoryIds(loadedDraft.categoryIds ?? [])
    setImportSourceType(loadedDraft.sourceType ?? 'MANUAL')
    setImportSourceUrl(loadedDraft.sourceUrl ?? '')
    if (loadedDraft.contentStatus !== 'READY') {
      setManualReadme('')
    }
  }, [draftDetailQuery.data])

  useEffect(() => {
    if (draftReadmeQuery.data !== undefined) {
      setManualReadme(draftReadmeQuery.data)
    }
  }, [draftReadmeQuery.data])

  const availableDrafts = (serversQuery.data?.items ?? []).filter((server) => server.draftVersionId)

  const set = (key: keyof typeof EMPTY_FORM) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setForm((prev) => ({ ...prev, [key]: e.target.value }))

  const toggleCategory = (id: number) =>
    setCategoryIds((prev) => (prev.includes(id) ? prev.filter((c) => c !== id) : [...prev, id]))

  const buildPayload = (): McpDraftPayload => {
    return {
      slug: form.slug,
      displayName: form.displayName,
      summary: form.summary,
      upstreamVersion: form.upstreamVersion || undefined,
      publisherName: form.publisherName || undefined,
      publisherUrl: form.publisherUrl || undefined,
      homepageUrl: form.homepageUrl || undefined,
      repositoryUrl: form.repositoryUrl || undefined,
      documentationUrl: form.documentationUrl || undefined,
      licenseSpdx: form.licenseSpdx || undefined,
      licenseName: form.licenseName || undefined,
      licenseUrl: form.licenseUrl || undefined,
      licenseNote: form.licenseNote || undefined,
      categoryIds,
    }
  }

  const handleCreate = () => {
    const payload = buildPayload()
    createDraft.mutate(payload, {
      onSuccess: (created) => {
        setSelectedDraftVersionId(String(created.versionId))
        setDraft(created)
        toast.success(t('mcpAdmin.drafts.created'))
      },
      onError: (error) => toast.error(error instanceof Error ? error.message : t('mcpAdmin.drafts.createFailed')),
    })
  }

  const handleUpdate = () => {
    if (!draft) return
    updateDraft.mutate(
      { versionId: draft.versionId, payload: buildPayload() },
      {
        onSuccess: (updated) => {
          setDraft(updated)
          toast.success(t('mcpAdmin.drafts.updated'))
        },
        onError: (error) => toast.error(error instanceof Error ? error.message : t('mcpAdmin.drafts.updateFailed')),
      },
    )
  }

  const handleImport = () => {
    if (!draft) return
    importDraft.mutate(
      { versionId: draft.versionId, sourceType: importSourceType, sourceUrl: importSourceUrl },
      {
        onSuccess: (updated) => {
          setDraft(updated)
          toast.success(t('mcpAdmin.drafts.imported'))
        },
        onError: (error) => toast.error(error instanceof Error ? error.message : t('mcpAdmin.drafts.importFailed')),
      },
    )
  }

  const handleSaveManualReadme = () => {
    if (!draft) return
    saveManualReadme.mutate(
      { versionId: draft.versionId, readme: manualReadme },
      {
        onSuccess: (updated) => {
          setDraft(updated)
          toast.success(t('mcpAdmin.drafts.manualReadmeSaved'))
        },
        onError: (error) => toast.error(error instanceof Error ? error.message : t('mcpAdmin.drafts.manualReadmeSaveFailed')),
      },
    )
  }

  const handleSubmit = () => {
    if (!draft) return
    publishDraft.mutate(draft.versionId, {
      onSuccess: () => {
        toast.success(t('mcpAdmin.drafts.published'))
        navigate({ to: '/admin/mcp/manage' })
      },
      onError: (error) => toast.error(error instanceof Error ? error.message : t('mcpAdmin.drafts.publishFailed')),
    })
  }

  return (
    <div className={APP_SHELL_PAGE_CLASS_NAME}>
      <h1 className="font-heading text-2xl font-bold tracking-tight text-foreground">
        {t('mcpAdmin.drafts.title')}
      </h1>

      <Card className="space-y-4 p-6">
        <div className="space-y-1.5">
          <Label htmlFor="mcp-existing-draft">{t('mcpAdmin.drafts.existingDraft')}</Label>
          <Select value={selectedDraftVersionId} onValueChange={setSelectedDraftVersionId}>
            <SelectTrigger id="mcp-existing-draft">
              <SelectValue placeholder={t('mcpAdmin.drafts.existingDraftPlaceholder')} />
            </SelectTrigger>
            <SelectContent>
              {availableDrafts.length > 0 ? (
                availableDrafts.map((server) => (
                  <SelectItem key={server.draftVersionId} value={String(server.draftVersionId)}>
                    {server.slug}{server.draftDisplayName ? ` - ${server.draftDisplayName}` : ''}
                  </SelectItem>
                ))
              ) : (
                <SelectItem value="__no_draft__" disabled>{t('mcpAdmin.drafts.noExistingDraft')}</SelectItem>
              )}
            </SelectContent>
          </Select>
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-1.5">
            <Label htmlFor="mcp-slug">{t('mcpAdmin.drafts.slug')}</Label>
            <Input id="mcp-slug" value={form.slug} onChange={set('slug')} placeholder="my-mcp" disabled={!!draft} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="mcp-display-name">{t('mcpAdmin.drafts.displayName')}</Label>
            <Input id="mcp-display-name" value={form.displayName} onChange={set('displayName')} />
          </div>
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="mcp-summary">{t('mcpAdmin.drafts.summary')}</Label>
          <Textarea
            id="mcp-summary"
            value={form.summary}
            onChange={set('summary')}
            rows={3}
          />
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-1.5">
            <Label htmlFor="mcp-upstream-version">{t('mcpAdmin.drafts.upstreamVersion')}</Label>
            <Input id="mcp-upstream-version" value={form.upstreamVersion} onChange={set('upstreamVersion')} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="mcp-publisher-name">{t('mcpAdmin.drafts.publisherName')}</Label>
            <Input id="mcp-publisher-name" value={form.publisherName} onChange={set('publisherName')} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="mcp-repository">{t('mcpAdmin.drafts.repositoryUrl')}</Label>
            <Input id="mcp-repository" value={form.repositoryUrl} onChange={set('repositoryUrl')} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="mcp-license-spdx">{t('mcpAdmin.drafts.licenseSpdx')}</Label>
            <Input id="mcp-license-spdx" value={form.licenseSpdx} onChange={set('licenseSpdx')} placeholder="MIT" />
          </div>
        </div>

        <div className="space-y-1.5">
          <Label>{t('mcpAdmin.drafts.categories')}</Label>
          <div className="flex flex-wrap gap-2">
            {(categoriesQuery.data ?? []).length > 0 ? (
              (categoriesQuery.data ?? []).map((category) => (
                <button
                  key={category.id}
                  type="button"
                  onClick={() => toggleCategory(category.id)}
                  className={
                    categoryIds.includes(category.id)
                      ? 'rounded-full border border-primary bg-primary/10 px-3 py-1 text-xs text-primary'
                      : 'rounded-full border border-border/60 px-3 py-1 text-xs text-muted-foreground'
                  }
                >
                  {category.displayName}
                </button>
              ))
            ) : (
              <p className="text-sm text-muted-foreground">{t('mcpAdmin.drafts.noCategories')}</p>
            )}
          </div>
        </div>

        <Button onClick={handleCreate} disabled={createDraft.isPending || !!draft || !form.slug || !form.displayName || !form.summary}>
          {createDraft.isPending ? t('mcpAdmin.drafts.creating') : t('mcpAdmin.drafts.create')}
        </Button>
      </Card>

      {draft && (
        <Card className="space-y-4 p-6">
          <h2 className="text-lg font-semibold text-foreground">
            {t('mcpAdmin.drafts.draftTitle')} — {draft.slug} (rev {draft.revisionNo})
          </h2>
          <p className="text-sm text-muted-foreground">
            {t('mcpAdmin.drafts.contentStatus')}: {draft.contentStatus}
            {draft.contentErrorMessage ? ` — ${draft.contentErrorMessage}` : ''}
          </p>

          <div className="flex flex-wrap gap-3">
            <Button variant="outline" onClick={handleUpdate} disabled={updateDraft.isPending}>
              {updateDraft.isPending ? t('mcpAdmin.drafts.updating') : t('mcpAdmin.drafts.update')}
            </Button>
          </div>

          <div className="grid gap-4 sm:grid-cols-[180px_1fr_auto]">
            <Select value={importSourceType} onValueChange={setImportSourceType}>
              <SelectTrigger><SelectValue placeholder={t('mcpAdmin.drafts.sourceType')} /></SelectTrigger>
              <SelectContent>
                <SelectItem value="MANUAL">{t('mcpAdmin.drafts.sourceTypeManual')}</SelectItem>
                <SelectItem value="GITHUB_REPOSITORY">GitHub</SelectItem>
                <SelectItem value="REMOTE_HTTP">HTTP</SelectItem>
              </SelectContent>
            </Select>
            {importSourceType === 'MANUAL' ? (
              <>
                <div className="sm:col-span-2 space-y-1.5">
                  <Label htmlFor="mcp-manual-readme">{t('mcpAdmin.drafts.manualReadme')}</Label>
                  <Textarea
                    id="mcp-manual-readme"
                    value={manualReadme}
                    onChange={(e) => setManualReadme(e.target.value)}
                    rows={16}
                    placeholder={t('mcpAdmin.drafts.manualReadmePlaceholder')}
                  />
                </div>
                <Button
                  variant="outline"
                  onClick={handleSaveManualReadme}
                  disabled={saveManualReadme.isPending || !manualReadme.trim()}
                >
                  {saveManualReadme.isPending ? t('mcpAdmin.drafts.savingManualReadme') : t('mcpAdmin.drafts.saveManualReadme')}
                </Button>
              </>
            ) : (
              <>
                <Input
                  value={importSourceUrl}
                  onChange={(e) => setImportSourceUrl(e.target.value)}
                  placeholder="https://github.com/owner/repo"
                />
                <Button variant="outline" onClick={handleImport} disabled={importDraft.isPending || !importSourceUrl}>
                  {importDraft.isPending ? t('mcpAdmin.drafts.importing') : t('mcpAdmin.drafts.import')}
                </Button>
              </>
            )}
          </div>

          <Button onClick={handleSubmit} disabled={publishDraft.isPending || draft.contentStatus !== 'READY'}>
            {publishDraft.isPending ? t('mcpAdmin.drafts.publishing') : t('mcpAdmin.drafts.publish')}
          </Button>
        </Card>
      )}
    </div>
  )
}
