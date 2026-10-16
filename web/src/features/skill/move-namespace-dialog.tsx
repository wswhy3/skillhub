import { useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { adminApi } from '@/api/client'
import { toast } from '@/shared/lib/toast'
import { Button } from '@/shared/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/shared/ui/dialog'
import { Label } from '@/shared/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/ui/select'

interface MoveNamespaceDialogProps {
  /** Skill 主键，用于调用管理员移动接口。 */
  skillId: number
  /** 移动成功后的回调，参数为目标命名空间 slug（不含 @ 前缀）。 */
  onMoved: (targetNamespaceSlug: string) => void
  children: React.ReactNode
}

/**
 * 管理员把 Global 命名空间中的 skill 移动到团队命名空间的确认对话框。
 *
 * 命名空间列表来自 GET /api/v1/admin/namespaces，仅展示 ACTIVE 的 TEAM 命名空间；
 * 实际移动调用 POST /api/v1/admin/skills/{skillId}/move-namespace。
 */
export function MoveNamespaceDialog({ skillId, onMoved, children }: MoveNamespaceDialogProps) {
  const { t } = useTranslation()
  const [open, setOpen] = useState(false)
  const [selectedSlug, setSelectedSlug] = useState('')

  const { data: namespaces, isLoading: isLoadingNamespaces } = useQuery({
    queryKey: ['admin', 'namespaces'],
    queryFn: () => adminApi.getNamespaces(),
    enabled: open,
  })

  const moveMutation = useMutation({
    mutationFn: (namespaceSlug: string) => adminApi.moveSkill(skillId, namespaceSlug),
  })

  // 移动仅支持团队命名空间，且目标必须处于 ACTIVE 状态（冻结/归档由后端拒绝，这里提前过滤）。
  const targetNamespaces = (namespaces ?? []).filter(
    (namespace) => namespace.type === 'TEAM' && namespace.status === 'ACTIVE',
  )

  const resetDialog = () => {
    setSelectedSlug('')
    moveMutation.reset()
  }

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen)
    if (!nextOpen) {
      resetDialog()
    }
  }

  const handleMove = async () => {
    if (!selectedSlug) return

    try {
      await moveMutation.mutateAsync(selectedSlug)
      toast.success(
        t('skillDetail.moveNamespaceSuccessTitle'),
        t('skillDetail.moveNamespaceSuccessDescription', { namespace: selectedSlug }),
      )
      setOpen(false)
      onMoved(selectedSlug)
    } catch (error) {
      toast.error(
        t('skillDetail.moveNamespaceErrorTitle'),
        error instanceof Error ? error.message : '',
      )
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>{children}</DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t('skillDetail.moveNamespaceDialogTitle')}</DialogTitle>
          <DialogDescription>{t('skillDetail.moveNamespaceDialogDescription')}</DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-2">
          <div className="rounded-lg border border-amber-500/20 bg-amber-500/10 p-3 text-sm text-amber-700 dark:text-amber-400">
            {t('skillDetail.moveNamespaceWarning')}
          </div>

          <div className="space-y-2">
            <Label htmlFor="move-namespace-target">{t('skillDetail.moveNamespaceTargetLabel')}</Label>
            {isLoadingNamespaces ? (
              <p className="text-sm text-muted-foreground">{t('skillDetail.moveNamespaceLoading')}</p>
            ) : targetNamespaces.length > 0 ? (
              <Select value={selectedSlug} onValueChange={setSelectedSlug}>
                <SelectTrigger id="move-namespace-target">
                  <SelectValue placeholder={t('skillDetail.moveNamespaceSelectPlaceholder')} />
                </SelectTrigger>
                <SelectContent>
                  {targetNamespaces.map((namespace) => (
                    <SelectItem key={namespace.slug} value={namespace.slug}>
                      {namespace.displayName ? `${namespace.displayName} (@${namespace.slug})` : `@${namespace.slug}`}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            ) : (
              <p className="text-sm text-muted-foreground">{t('skillDetail.moveNamespaceNoTargets')}</p>
            )}
          </div>
        </div>

        {moveMutation.error ? (
          <p className="text-sm text-red-600">{moveMutation.error.message}</p>
        ) : null}

        <DialogFooter className="sm:justify-end">
          <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
            {t('dialog.cancel')}
          </Button>
          <Button
            type="button"
            onClick={handleMove}
            disabled={!selectedSlug || moveMutation.isPending}
          >
            {moveMutation.isPending ? t('skillDetail.moveNamespaceMoving') : t('skillDetail.moveNamespaceConfirmAction')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
