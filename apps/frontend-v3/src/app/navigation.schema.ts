import { z } from 'zod'

const navigationLinkSchema: z.ZodType<{
  id: string
  label: string
  path: string
  icon?: string
  badge?: {
    label: string
    tone: 'info' | 'success' | 'warning' | 'neutral'
  }
  external?: boolean
  description?: string
  children?: Array<any>
  permissions?: string[]
}> = z.object({
  id: z.string(),
  label: z.string(),
  path: z.string(),
  icon: z.string().optional(),
  badge: z
    .object({
      label: z.string(),
      tone: z.enum(['info', 'success', 'warning', 'neutral']).default('neutral'),
    })
    .optional(),
  external: z.boolean().optional(),
  description: z.string().optional(),
  children: z.lazy(() => navigationLinkSchema.array()).optional(),
  permissions: z.array(z.string()).optional(),
})

const navigationActionSchema = z.discriminatedUnion('type', [
  z.object({
    id: z.string(),
    type: z.literal('theme'),
  }),
  z.object({
    id: z.string(),
    type: z.literal('notifications'),
    path: z.string().optional(),
  }),
  z.object({
    id: z.string(),
    type: z.literal('profile'),
    initials: z.string().optional(),
  }),
  z.object({
    id: z.string(),
    type: z.literal('help'),
    path: z.string().optional(),
  }),
  z.object({
    id: z.string(),
    type: z.literal('custom'),
    icon: z.string().optional(),
    path: z.string().optional(),
    label: z.string().optional(),
  }),
])

export const navigationConfigSchema = z.object({
  brand: z.object({
    name: z.string(),
    shortName: z.string().optional(),
    tagline: z.string().optional(),
  }),
  primary: navigationLinkSchema.array(),
  secondary: navigationLinkSchema.array().default([]),
  quickLinks: navigationLinkSchema.array().default([]),
  globalActions: navigationActionSchema.array().default([]),
})

export type NavigationConfig = z.infer<typeof navigationConfigSchema>
export type NavigationLink = z.infer<typeof navigationLinkSchema>
export type NavigationAction = z.infer<typeof navigationActionSchema>

export async function loadNavigationConfig(): Promise<NavigationConfig> {
  const response = await fetch('/mock/navigation.json')
  if (!response.ok) {
    throw new Error('Failed to load navigation configuration')
  }
  const data = await response.json()
  return navigationConfigSchema.parse(data)
}
