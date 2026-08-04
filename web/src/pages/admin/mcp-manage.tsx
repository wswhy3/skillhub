import { useState } from 'react'
import { useNavigate } from '@tanstack/react-router'
import { useTranslation } from 'react-i18next'
import { toast } from '@/shared/lib/toast'
import { Card } from '@/shared/ui/card'
import { Button } from '@/shared/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/shared/ui/dialog'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/shared/ui/table'
import { SkeletonList } from '@/shared/components/skeleton-loader'
import { EmptyState } from '@/shared/components/empty-state'
import { formatLocalDateTime } from '@/shared/lib/date-time'
import { useMcpAdminServers, useMcpCreateRevisionDraft, useMcpGovernance } from '@/shared/hooks/use-mcp-admin-queries'
import type { McpAdminServer } from '@/api/types'
import { APP_SHELL_PAGE_CLASS_NAME } from '@/app/page-shell-style'

/**
 * MCP 管理列表：治理动作（隐藏/恢复/归档/下架）。
 */
export function AdminMcpManagePage() {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const [deleteTarget, setDeleteTarget] = useState<McpAdminServer | null>(null)
  const serversQuery = useMcpAdminServers(0, 50)
  const createRevisionDraft = useMcpCreateRevisionDraft()
  const { hide, unhide, archive, restore, yank, remove } = useMcpGovernance()

  const servers = serversQuery.data?.items ?? []

  const run = (mutation: { mutate: (v: never, o?: object) => void }, vars: never, successKey: string) => {
    mutation.mutate(vars, {
      onSuccess: () => toast.success(t(successKey)),
      onError: (error: unknown) => toast.error(error instanceof Error ? error.message : t('mcpAdmin.manage.actionFailed')),
    })
  }

  const openDraft = (draftVersionId: number) => {
    void navigate({ to: '/admin/mcp/drafts', search: { draftVersionId } })
  }

  const handleRevise = (latestVersionId: number) => {
    createRevisionDraft.mutate(latestVersionId, {
      onSuccess: (draft) => {
        toast.success(t('mcpAdmin.manage.revisionCreated'))
        openDraft(draft.versionId)
      },
      onError: (error: unknown) => {
        toast.error(error instanceof Error ? error.message : t('mcpAdmin.manage.revisionCreateFailed'))
      },
    })
  }

  const handleDelete = () => {
    if (!deleteTarget) return
    remove.mutate(deleteTarget.id, {
      onSuccess: () => {
        toast.success(t('mcpAdmin.manage.deleted'))
        setDeleteTarget(null)
      },
      onError: (error: unknown) => {
        toast.error(error instanceof Error ? error.message : t('mcpAdmin.manage.deleteFailed'))
      },
    })
  }

  return (
    <div className={APP_SHELL_PAGE_CLASS_NAME}>
      <h1 className="font-heading text-2xl font-bold tracking-tight text-foreground">
        {t('mcpAdmin.manage.title')}
      </h1>

      {serversQuery.isLoading ? (
        <SkeletonList count={4} />
      ) : servers.length === 0 ? (
        <EmptyState title={t('mcpAdmin.manage.empty')} description={t('mcpAdmin.manage.emptyDescription')} />
      ) : (
        <Card>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t('mcpAdmin.manage.colSlug')}</TableHead>
                <TableHead>{t('mcpAdmin.manage.colStatus')}</TableHead>
                <TableHead>{t('mcpAdmin.manage.colVisibility')}</TableHead>
                <TableHead>{t('mcpAdmin.manage.colHidden')}</TableHead>
                <TableHead>{t('mcpAdmin.manage.colUpdatedAt')}</TableHead>
                <TableHead className="text-right">{t('mcpAdmin.manage.colActions')}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {servers.map((server) => (
                <TableRow key={server.id}>
                  <TableCell className="font-medium">@{server.namespace}/{server.slug}</TableCell>
                  <TableCell>{server.status}</TableCell>
                  <TableCell>{server.visibility}</TableCell>
                  <TableCell>{server.hidden ? t('mcpAdmin.manage.yes') : t('mcpAdmin.manage.no')}</TableCell>
                  <TableCell>{formatLocalDateTime(server.updatedAt, i18n.language)}</TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-2">
                      {server.hidden ? (
                        <Button size="sm" variant="outline" onClick={() => run(unhide, server.id as never, 'mcpAdmin.manage.unhidden')}>
                          {t('mcpAdmin.manage.unhide')}
                        </Button>
                      ) : (
                        <Button size="sm" variant="outline" onClick={() => run(hide, { serverId: server.id } as never, 'mcpAdmin.manage.hidden')}>
                          {t('mcpAdmin.manage.hide')}
                        </Button>
                      )}
                      {server.status === 'ARCHIVED' ? (
                        <Button size="sm" variant="outline" onClick={() => run(restore, server.id as never, 'mcpAdmin.manage.restored')}>
                          {t('mcpAdmin.manage.restore')}
                        </Button>
                      ) : (
                        <Button size="sm" variant="outline" onClick={() => run(archive, server.id as never, 'mcpAdmin.manage.archived')}>
                          {t('mcpAdmin.manage.archive')}
                        </Button>
                      )}
                      {server.latestVersionId && (
                        <Button size="sm" variant="ghost" onClick={() => run(yank, { versionId: server.latestVersionId } as never, 'mcpAdmin.manage.yanked')}>
                          {t('mcpAdmin.manage.yank')}
                        </Button>
                      )}
                      {server.draftVersionId ? (
                        <Button size="sm" variant="outline" onClick={() => openDraft(server.draftVersionId!)}>
                          {t('mcpAdmin.manage.editDraft')}
                        </Button>
                      ) : server.latestVersionId && server.status !== 'ARCHIVED' ? (
                        <Button
                          size="sm"
                          onClick={() => handleRevise(server.latestVersionId!)}
                          disabled={createRevisionDraft.isPending}
                        >
                          {createRevisionDraft.isPending
                            ? t('mcpAdmin.manage.creatingRevision')
                            : t('mcpAdmin.manage.createRevision')}
                        </Button>
                      ) : null}
                      <Button size="sm" variant="destructive" onClick={() => setDeleteTarget(server)}>
                        {t('dialog.delete')}
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Card>
      )}

      <Dialog open={!!deleteTarget} onOpenChange={(open) => !open && setDeleteTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('mcpAdmin.manage.deleteDialogTitle')}</DialogTitle>
            <DialogDescription>
              {deleteTarget
                ? t('mcpAdmin.manage.deleteDialogDescription', {
                    slug: `@${deleteTarget.namespace}/${deleteTarget.slug}`,
                  })
                : t('mcpAdmin.manage.deleteDialogDescription', { slug: '' })}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteTarget(null)}>
              {t('dialog.cancel')}
            </Button>
            <Button variant="destructive" onClick={handleDelete} disabled={remove.isPending}>
              {remove.isPending ? t('mcpAdmin.manage.deleting') : t('dialog.delete')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
