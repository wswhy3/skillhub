export type MySkillFilter = 'ALL' | 'PENDING_REVIEW' | 'PUBLISHED' | 'REJECTED' | 'ARCHIVED' | 'HIDDEN' | 'REQUIRES_API_KEY'

export function getMySkillFilters(isSuperAdmin: boolean): MySkillFilter[] {
  if (isSuperAdmin) {
    return ['ALL', 'PUBLISHED', 'HIDDEN', 'ARCHIVED', 'REQUIRES_API_KEY']
  }
  return ['ALL', 'PENDING_REVIEW', 'PUBLISHED', 'REJECTED', 'ARCHIVED', 'REQUIRES_API_KEY']
}

export function getMySkillEmptyStateKey(filter: MySkillFilter): { title: string; description: string } {
  if (filter === 'ALL') {
    return {
      title: 'mySkills.emptyTitle',
      description: 'mySkills.emptyDescription',
    }
  }

  return {
    title: 'mySkills.emptyFilteredTitle',
    description: `mySkills.emptyFilteredDescription.${filter}`,
  }
}
