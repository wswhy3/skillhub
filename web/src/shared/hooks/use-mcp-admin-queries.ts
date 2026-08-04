import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminMcpApi } from '@/api/client'
import type { McpDraftPayload, McpCategoryPayload } from '@/api/types'
import { getI18nCacheKey } from './query-keys'

const MCP_ADMIN_STALE = 30_000

export function useMcpAdminSubmissions(status?: string, page = 0, size = 20) {
  return useQuery({
    queryKey: ['mcp-admin', 'submissions', status, page, size],
    queryFn: () => adminMcpApi.listSubmissions({ status, page, size }),
    staleTime: MCP_ADMIN_STALE,
  })
}

export function useMcpAdminServers(page = 0, size = 20) {
  return useQuery({
    queryKey: ['mcp-admin', 'servers', page, size],
    queryFn: () => adminMcpApi.listServers({ page, size }),
    staleTime: MCP_ADMIN_STALE,
  })
}

export function useMcpAdminCategories() {
  return useQuery({
    queryKey: ['mcp-admin', 'categories', getI18nCacheKey()],
    queryFn: () => adminMcpApi.listCategories(),
    staleTime: MCP_ADMIN_STALE,
  })
}

export function useMcpDraftDetail(versionId?: number) {
  return useQuery({
    queryKey: ['mcp-admin', 'draft', versionId],
    queryFn: () => adminMcpApi.getDraft(versionId!),
    staleTime: MCP_ADMIN_STALE,
    enabled: versionId !== undefined,
  })
}

export function useMcpDraftReadme(versionId?: number) {
  return useQuery({
    queryKey: ['mcp-admin', 'draft-readme', versionId],
    queryFn: () => adminMcpApi.getDraftReadme(versionId!),
    staleTime: MCP_ADMIN_STALE,
    enabled: versionId !== undefined,
  })
}

function useInvalidateMcpAdmin() {
  const queryClient = useQueryClient()
  return () => {
    queryClient.invalidateQueries({ queryKey: ['mcp-admin'] })
    queryClient.invalidateQueries({ queryKey: ['mcp'] })
  }
}

export function useMcpCreateDraft() {
  const invalidate = useInvalidateMcpAdmin()
  return useMutation({
    mutationFn: (payload: McpDraftPayload) => adminMcpApi.createDraft(payload),
    onSuccess: invalidate,
  })
}

export function useMcpUpdateDraft() {
  const invalidate = useInvalidateMcpAdmin()
  return useMutation({
    mutationFn: ({ versionId, payload }: { versionId: number; payload: McpDraftPayload }) =>
      adminMcpApi.updateDraft(versionId, payload),
    onSuccess: invalidate,
  })
}

export function useMcpCreateRevisionDraft() {
  const invalidate = useInvalidateMcpAdmin()
  return useMutation({
    mutationFn: (versionId: number) => adminMcpApi.createRevisionDraft(versionId),
    onSuccess: invalidate,
  })
}

export function useMcpImportDraft() {
  const invalidate = useInvalidateMcpAdmin()
  return useMutation({
    mutationFn: ({ versionId, sourceType, sourceUrl, sourceRevision }: {
      versionId: number; sourceType: string; sourceUrl: string; sourceRevision?: string
    }) => adminMcpApi.importDraft(versionId, sourceType, sourceUrl, sourceRevision),
    onSuccess: invalidate,
  })
}

export function useMcpSaveManualReadme() {
  const invalidate = useInvalidateMcpAdmin()
  return useMutation({
    mutationFn: ({ versionId, readme }: { versionId: number; readme: string }) =>
      adminMcpApi.saveManualReadme(versionId, readme),
    onSuccess: invalidate,
  })
}

export function useMcpSubmitDraft() {
  const invalidate = useInvalidateMcpAdmin()
  return useMutation({
    mutationFn: (versionId: number) => adminMcpApi.submitDraft(versionId),
    onSuccess: invalidate,
  })
}

export function useMcpPublishDraft() {
  const invalidate = useInvalidateMcpAdmin()
  return useMutation({
    mutationFn: (versionId: number) => adminMcpApi.publishDraft(versionId),
    onSuccess: invalidate,
  })
}

export function useMcpApprove() {
  const invalidate = useInvalidateMcpAdmin()
  return useMutation({
    mutationFn: ({ submissionId, lockVersion, comment }: {
      submissionId: number; lockVersion: number; comment?: string
    }) => adminMcpApi.approveSubmission(submissionId, lockVersion, comment),
    onSuccess: invalidate,
  })
}

export function useMcpReject() {
  const invalidate = useInvalidateMcpAdmin()
  return useMutation({
    mutationFn: ({ submissionId, lockVersion, comment }: {
      submissionId: number; lockVersion: number; comment?: string
    }) => adminMcpApi.rejectSubmission(submissionId, lockVersion, comment),
    onSuccess: invalidate,
  })
}

export function useMcpWithdraw() {
  const invalidate = useInvalidateMcpAdmin()
  return useMutation({
    mutationFn: (submissionId: number) => adminMcpApi.withdrawSubmission(submissionId),
    onSuccess: invalidate,
  })
}

export function useMcpGovernance() {
  const invalidate = useInvalidateMcpAdmin()
  const yank = useMutation({
    mutationFn: ({ versionId, reason }: { versionId: number; reason?: string }) =>
      adminMcpApi.yankVersion(versionId, reason),
    onSuccess: invalidate,
  })
  const hide = useMutation({
    mutationFn: ({ serverId, reason }: { serverId: number; reason?: string }) =>
      adminMcpApi.hideServer(serverId, reason),
    onSuccess: invalidate,
  })
  const unhide = useMutation({
    mutationFn: (serverId: number) => adminMcpApi.unhideServer(serverId),
    onSuccess: invalidate,
  })
  const archive = useMutation({
    mutationFn: (serverId: number) => adminMcpApi.archiveServer(serverId),
    onSuccess: invalidate,
  })
  const restore = useMutation({
    mutationFn: (serverId: number) => adminMcpApi.restoreServer(serverId),
    onSuccess: invalidate,
  })
  const remove = useMutation({
    mutationFn: (serverId: number) => adminMcpApi.deleteServer(serverId),
    onSuccess: invalidate,
  })
  return { yank, hide, unhide, archive, restore, remove }
}

export function useMcpCategoryMutations() {
  const invalidate = useInvalidateMcpAdmin()
  const create = useMutation({
    mutationFn: (payload: McpCategoryPayload) => adminMcpApi.createCategory(payload),
    onSuccess: invalidate,
  })
  const update = useMutation({
    mutationFn: ({ categoryId, payload }: { categoryId: number; payload: McpCategoryPayload }) =>
      adminMcpApi.updateCategory(categoryId, payload),
    onSuccess: invalidate,
  })
  return { create, update }
}
