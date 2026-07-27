import { Menu } from 'antd'
import type { MenuProps } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import type { AccountRole } from '../../features/auth/authTypes'
import {
  filterNavigationByRole,
  findNavLeafByKey,
  resolveOpenNavKeys,
  resolveSelectedNavKey,
  toAntdMenuItems,
} from '../../navigation/navUtils'

type AppNavMenuProps = {
  role: AccountRole | undefined
  mode?: MenuProps['mode']
  inlineCollapsed?: boolean
  onNavigate?: () => void
}

/**
 * Shared Ant Design Menu driven by React Router location.
 * Open group keys follow the URL; manual expand is accordion (one group at a time).
 */
export function AppNavMenu({
  role,
  mode = 'inline',
  inlineCollapsed = false,
  onNavigate,
}: AppNavMenuProps) {
  const navigate = useNavigate()
  const location = useLocation()

  const navItems = useMemo(() => filterNavigationByRole(role), [role])
  const menuItems = useMemo(() => toAntdMenuItems(navItems), [navItems])

  const selectedKey = resolveSelectedNavKey(location.pathname, navItems)
  const routeOpenKeys = useMemo(
    () => resolveOpenNavKeys(location.pathname, navItems),
    [location.pathname, navItems],
  )

  const [openKeys, setOpenKeys] = useState<string[]>(routeOpenKeys)

  useEffect(() => {
    setOpenKeys(routeOpenKeys)
  }, [routeOpenKeys])

  return (
    <Menu
      mode={mode}
      theme="light"
      inlineCollapsed={inlineCollapsed}
      selectedKeys={selectedKey ? [selectedKey] : []}
      openKeys={inlineCollapsed ? undefined : openKeys}
      onOpenChange={(keys) => {
        const latest = keys.find((key) => !openKeys.includes(key))
        setOpenKeys(latest ? [latest] : [])
      }}
      items={menuItems}
      onClick={(event) => {
        const leaf = findNavLeafByKey(navItems, event.key)
        if (!leaf) {
          return
        }
        navigate(leaf.route)
        onNavigate?.()
      }}
      style={{ borderInlineEnd: 'none' }}
    />
  )
}
