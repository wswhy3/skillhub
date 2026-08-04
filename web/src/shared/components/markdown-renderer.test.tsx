import { describe, expect, it, vi } from 'vitest'
import { renderToStaticMarkup } from 'react-dom/server'
import { MarkdownRenderer } from '@/shared/components/markdown-renderer'

vi.mock('react-i18next', async () => {
  const actual = await vi.importActual<typeof import('react-i18next')>('react-i18next')
  return {
    ...actual,
    useTranslation: () => ({ t: (key: string) => key }),
  }
})

describe('MarkdownRenderer', () => {
  it('adds noopener noreferrer to external links', () => {
    const html = renderToStaticMarkup(
      <MarkdownRenderer content="[site](https://example.com/docs)" />,
    )
    expect(html).toContain('target="_blank"')
    expect(html).toContain('rel="noopener noreferrer"')
    expect(html).toContain('https://example.com/docs')
  })

  it('keeps relative links in the same tab', () => {
    const html = renderToStaticMarkup(
      <MarkdownRenderer content="[home](/search)" />,
    )
    expect(html).toContain('href="/search"')
    expect(html).not.toContain('target="_blank"')
  })

  it('drops non-https images', () => {
    const html = renderToStaticMarkup(
      <MarkdownRenderer content="![x](http://insecure.example.com/x.png)" />,
    )
    expect(html).not.toContain('insecure.example.com')
  })

  it('renders https images without referer', () => {
    const html = renderToStaticMarkup(
      <MarkdownRenderer content="![logo](https://example.com/logo.png)" />,
    )
    expect(html).toContain('referrerPolicy="no-referrer"')
    expect(html).toContain('https://example.com/logo.png')
  })

  it('strips raw html and scripts', () => {
    const html = renderToStaticMarkup(
      <MarkdownRenderer content={'<script>alert(1)</script>\n\nhello'} />,
    )
    expect(html).not.toContain('<script>')
    expect(html).toContain('hello')
  })
})
