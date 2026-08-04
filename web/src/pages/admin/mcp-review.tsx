import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { toast } from '@/shared/lib/toast'
import { Card } from '@/shared/ui/card'
import { Button } from '@/shared/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/shared/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/shared/ui/table'
import { SkeletonList } from '@/shared/components/skeleton-loader'
import { EmptyState } from '@/shared/components/empty-state'
import { formatLocalDateTime } from '@/shared/lib/date-time'
import {
  useMcpAdminSubmissions,
  useMcpApprove,
  useMcpReject,
  useMcpWithdraw,
} from '@/shared/hooks/use-mcp-admin-queries'
import { APP_SHELL_PAGE_CLASS_NAME } from '@/app/page-shell-style'

/**
 * MCP 审核队列：通过 / 拒绝 / 撤回。
 */
export function AdminMcpReviewPage() {
  const { t, i18n } = useTranslation()
  const [status, setStatus] = useState<string>('PENDING')
  const submissionsQuery = useMcpAdminSubmissions(status, 0, 50)
  const approve = useMcpApprove()
  const reject = useMcpReject()
  const withdraw = useMcpWithdraw()

  const submissions = submissionsQuery.data?.items ?? []

  const handleApprove = (id: number, lockVersion: number) => {
    approve.mutate({ submissionId: id, lockVersion }, {
      onSuccess: () => toast.success(t('mcpAdmin.review.approved')),
      onError: (error) => toast.error(error instanceof Error ? error.message : t('mcpAdmin.review.actionFailed')),
    })
  }

  const handleReject = (id: number, lockVersion: number) => {
    const comment = window.prompt(t('mcpAdmin.review.rejectPrompt')) ?? ''
    reject.mutate({ submissionId: id, lockVersion, comment }, {
      onSuccess: () => toast.success(t('mcpAdmin.review.rejected')),
      onError: (error) => toast.error(error instanceof Error ? error.message : t('mcpAdmin.review.actionFailed')),
    })
  }

  const handleWithdraw = (id: number) => {
    withdraw.mutate(id, {
      onSuccess: () => toast.success(t('mcpAdmin.review.withdrawn')),
      onError: (error) => toast.error(error instanceof Error ? error.message : t('mcpAdmin.review.actionFailed')),
    })
  }

  return (
    <div className={APP_SHELL_PAGE_CLASS_NAME}>
      <div className="flex items-center justify-between">
        <h1 className="font-heading text-2xl font-bold tracking-tight text-foreground">
          {t('mcpAdmin.review.title')}
        </h1>
        <Select value={status} onValueChange={setStatus}>
          <SelectTrigger className="w-44"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value="PENDING">{t('mcpAdmin.review.statusPending')}</SelectItem>
            <SelectItem value="APPROVED">{t('mcpAdmin.review.statusApproved')}</SelectItem>
            <SelectItem value="REJECTED">{t('mcpAdmin.review.statusRejected')}</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {submissionsQuery.isLoading ? (
        <SkeletonList count={4} />
      ) : submissions.length === 0 ? (
        <EmptyState title={t('mcpAdmin.review.empty')} description={t('mcpAdmin.review.emptyDescription')} />
      ) : (
        <Card>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t('mcpAdmin.review.colName')}</TableHead>
                <TableHead>{t('mcpAdmin.review.colCoordinate')}</TableHead>
                <TableHead>{t('mcpAdmin.review.colSubmitter')}</TableHead>
                <TableHead>{t('mcpAdmin.review.colSubmittedAt')}</TableHead>
                <TableHead>{t('mcpAdmin.review.colStatus')}</TableHead>
                <TableHead className="text-right">{t('mcpAdmin.review.colActions')}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {submissions.map((submission) => (
                <TableRow key={submission.id}>
                  <TableCell className="font-medium">{submission.displayName}</TableCell>
                  <TableCell>@{submission.namespace}/{submission.slug}</TableCell>
                  <TableCell>{submission.submittedBy}</TableCell>
                  <TableCell>{formatLocalDateTime(submission.submittedAt, i18n.language)}</TableCell>
                  <TableCell>{submission.status}</TableCell>
                  <TableCell className="text-right">
                    {submission.status === 'PENDING' && (
                      <div className="flex justify-end gap-2">
                        <Button size="sm" variant="outline" onClick={() => handleApprove(submission.id, submission.lockVersion)}>
                          {t('mcpAdmin.review.approve')}
                        </Button>
                        <Button size="sm" variant="outline" onClick={() => handleReject(submission.id, submission.lockVersion)}>
                          {t('mcpAdmin.review.reject')}
                        </Button>
                        <Button size="sm" variant="ghost" onClick={() => handleWithdraw(submission.id)}>
                          {t('mcpAdmin.review.withdraw')}
                        </Button>
                      </div>
                    )}
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
