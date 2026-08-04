import { describe, expect, it, vi } from 'vitest'
import { renderToStaticMarkup } from 'react-dom/server'
import { McpCard } from '@/features/mcp-catalog/mcp-card'
import type { McpServerSummary } from '@/api/types'

vi.mock('react-i18next', async () => {
  const actual = await vi.importActual<typeof import('react-i18next')>('react-i18next')
  return {
    ...actual,
    useTranslation: () => ({ t: (key: string) => key }),
  }
})

vi.mock('@tanstack/react-router', () => ({
  useNavigate: () => vi.fn(),
}))

const server: McpServerSummary = {
  id: 1,
  namespace: 'global',
  slug: 'my-mcp',
  coordinate: '@global/my-mcp',
  displayName: 'My MCP',
  summary: 'A test MCP server',
  publisherName: 'Acme',
  categories: [{ id: 1, slug: 'devtools', displayName: 'Dev Tools', sortOrder: 0 }],
  iconUrl: '/api/v1/mcp/servers/global/my-mcp/icon',
  upstreamVersion: '1.2.0',
  updatedAt: '2026-08-01T00:00:00Z',
}

describe('McpCard', () => {
  it('renders display name, coordinate and summary', () => {
    const html = renderToStaticMarkup(<McpCard server={server} />)
    expect(html).toContain('My MCP')
    expect(html).toContain('@global/my-mcp')
    expect(html).toContain('A test MCP server')
  })

  it('renders publisher and version badges', () => {
    const html = renderToStaticMarkup(<McpCard server={server} />)
    expect(html).toContain('Acme')
    expect(html).toContain('v1.2.0')
    expect(html).toContain('Dev Tools')
  })

  it('renders icon without referer', () => {
    const html = renderToStaticMarkup(<McpCard server={server} />)
    expect(html).toContain('referrerPolicy="no-referrer"')
  })

  it('falls back to initial letter when no icon', () => {
    const html = renderToStaticMarkup(<McpCard server={{ ...server, iconUrl: undefined }} />)
    expect(html).toContain('>M<')
  })
})
