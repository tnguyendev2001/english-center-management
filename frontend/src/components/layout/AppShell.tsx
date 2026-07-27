import { MenuFoldOutlined, MenuOutlined, MenuUnfoldOutlined } from '@ant-design/icons'
import { Button, Drawer, Grid, Layout, Space, Typography } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../features/auth/AuthContext'
import { isNavGroup } from '../../navigation/navConfig'
import {
  filterNavigationByRole,
  resolveSelectedNavKey,
} from '../../navigation/navUtils'
import { AppNavMenu } from './AppNavMenu'

const { useBreakpoint } = Grid

type AppShellProps = {
  children: React.ReactNode
}

export function AppShell({ children }: AppShellProps) {
  const navigate = useNavigate()
  const location = useLocation()
  const { user, logout } = useAuth()
  const screens = useBreakpoint()
  const isMobile = !screens.md

  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)

  const navItems = useMemo(() => filterNavigationByRole(user?.role), [user?.role])
  const selectedKey = resolveSelectedNavKey(location.pathname, navItems)

  const headerTrail = useMemo(() => {
    if (!selectedKey) {
      return 'Quản lý trung tâm tiếng Anh'
    }
    for (const item of navItems) {
      if (isNavGroup(item)) {
        const child = item.children.find((c) => c.key === selectedKey)
        if (child) {
          return `${item.label} / ${child.label}`
        }
      } else if (item.key === selectedKey) {
        return item.label
      }
    }
    return 'Quản lý trung tâm tiếng Anh'
  }, [navItems, selectedKey])

  useEffect(() => {
    setMobileOpen(false)
  }, [location.pathname])

  return (
    <Layout className="app-shell">
      {!isMobile ? (
        <Layout.Sider
          theme="light"
          width={240}
          collapsedWidth={64}
          collapsible
          collapsed={collapsed}
          trigger={null}
          className="app-sider"
        >
          <div className={`app-logo ${collapsed ? 'app-logo-collapsed' : ''}`}>
            {collapsed ? (
              <Typography.Title level={5} style={{ margin: 0 }}>
                EC
              </Typography.Title>
            ) : (
              <>
                <Typography.Title level={4} style={{ margin: 0 }}>
                  English Center
                </Typography.Title>
                <Typography.Text type="secondary">Quản lý trung tâm</Typography.Text>
              </>
            )}
          </div>
          <div className="app-sider-menu">
            <AppNavMenu role={user?.role} inlineCollapsed={collapsed} />
          </div>
        </Layout.Sider>
      ) : null}

      <Layout>
        <Layout.Header className="app-header">
          <Space style={{ width: '100%', justifyContent: 'space-between' }} wrap>
            <Space>
              {isMobile ? (
                <Button
                  type="text"
                  aria-label="Mở menu"
                  icon={<MenuOutlined />}
                  onClick={() => setMobileOpen(true)}
                />
              ) : (
                <Button
                  type="text"
                  aria-label={collapsed ? 'Mở rộng menu' : 'Thu gọn menu'}
                  icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
                  onClick={() => setCollapsed((value) => !value)}
                />
              )}
              <span className="app-header-title">{headerTrail}</span>
            </Space>
            <Space>
              <Typography.Text>
                {user?.username} ({user?.role})
              </Typography.Text>
              <Button
                onClick={() => {
                  logout()
                  navigate('/login', { replace: true })
                }}
              >
                Đăng xuất
              </Button>
            </Space>
          </Space>
        </Layout.Header>

        <Layout.Content className="app-content">{children}</Layout.Content>
      </Layout>

      <Drawer
        title="Menu"
        placement="left"
        open={mobileOpen}
        onClose={() => setMobileOpen(false)}
        width={280}
        styles={{ body: { padding: 0 } }}
      >
        <div className="app-mobile-drawer-logo">
          <Typography.Title level={4} style={{ margin: 0 }}>
            English Center
          </Typography.Title>
          <Typography.Text type="secondary">Quản lý trung tâm</Typography.Text>
        </div>
        <AppNavMenu role={user?.role} onNavigate={() => setMobileOpen(false)} />
      </Drawer>
    </Layout>
  )
}
