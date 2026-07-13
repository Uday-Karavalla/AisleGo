import { useEffect } from 'react'

interface SeoProps {
  title: string
  description: string
  canonicalPath: string
  imageUrl?: string
  structuredData?: Record<string, unknown>
}

export function Seo({ title, description, canonicalPath, imageUrl, structuredData }: SeoProps) {
  useEffect(() => {
    document.title = title
    const canonicalUrl = `${window.location.origin}${canonicalPath}`
    function setMeta(selector: string, attribute: 'name' | 'property', key: string, content: string) {
      let element = document.head.querySelector<HTMLMetaElement>(selector)
      if (!element) {
        element = document.createElement('meta')
        element.setAttribute(attribute, key)
        document.head.appendChild(element)
      }
      element.content = content
    }
    setMeta('meta[name="description"]', 'name', 'description', description)
    setMeta('meta[property="og:title"]', 'property', 'og:title', title)
    setMeta('meta[property="og:description"]', 'property', 'og:description', description)
    setMeta('meta[property="og:url"]', 'property', 'og:url', canonicalUrl)
    if (imageUrl) setMeta('meta[property="og:image"]', 'property', 'og:image', imageUrl)
    let canonical = document.head.querySelector<HTMLLinkElement>('link[rel="canonical"]')
    if (!canonical) { canonical = document.createElement('link'); canonical.rel = 'canonical'; document.head.appendChild(canonical) }
    canonical.href = canonicalUrl
    const id = 'aislego-page-structured-data'
    document.getElementById(id)?.remove()
    if (structuredData) {
      const script = document.createElement('script')
      script.id = id
      script.type = 'application/ld+json'
      script.textContent = JSON.stringify(structuredData)
      document.head.appendChild(script)
    }
    return () => { document.getElementById(id)?.remove() }
  }, [title, description, canonicalPath, imageUrl, structuredData])
  return null
}
