import type { MenuProps } from 'antd'
import type { AccountRole } from '../features/auth/authTypes'
import { APP_NAVIGATION, isNavGroup, type NavItem, type NavLeafItem } from './navConfig'

export function filterNavigationByRole(role: AccountRole | undefined): NavItem[] {
  if (!role) {
    return []
  }

  const result: NavItem[] = []
  for (const item of APP_NAVIGATION) {
    if (!item.allowedRoles.includes(role)) {
      continue
    }
    if (!isNavGroup(item)) {
      result.push(item)
      continue
    }
    const children = item.children.filter((child) => child.allowedRoles.includes(role))
    if (children.length === 0) {
      continue
    }
    result.push({ ...item, children })
  }
  return result
}

function collectLeaves(items: NavItem[]): NavLeafItem[] {
  const leaves: NavLeafItem[] = []
  for (const item of items) {
    if (isNavGroup(item)) {
      leaves.push(...item.children)
    } else {
      leaves.push(item)
    }
  }
  return leaves
}

/** Longest matching leaf route for the current pathname (supports /students/:id). */
export function resolveSelectedNavKey(pathname: string, items: NavItem[]): string | undefined {
  const leaves = collectLeaves(items)
  const match = leaves
    .slice()
    .sort((a, b) => b.route.length - a.route.length)
    .find((leaf) => pathname === leaf.route || pathname.startsWith(`${leaf.route}/`))
  return match?.key
}

/** Parent group key for the selected leaf, if any. */
export function resolveOpenNavKeys(pathname: string, items: NavItem[]): string[] {
  const selectedKey = resolveSelectedNavKey(pathname, items)
  if (!selectedKey) {
    return []
  }
  for (const item of items) {
    if (isNavGroup(item) && item.children.some((child) => child.key === selectedKey)) {
      return [item.key]
    }
  }
  return []
}

export function toAntdMenuItems(items: NavItem[]): MenuProps['items'] {
  return items.map((item) => {
    if (isNavGroup(item)) {
      return {
        key: item.key,
        label: item.label,
        title: item.label,
        icon: item.icon,
        children: item.children.map((child) => ({
          key: child.key,
          label: child.label,
          title: child.label,
        })),
      }
    }
    return {
      key: item.key,
      label: item.label,
      icon: item.icon,
      title: item.label,
    }
  })
}

export function findNavLeafByKey(items: NavItem[], key: string): NavLeafItem | undefined {
  return collectLeaves(items).find((leaf) => leaf.key === key)
}
