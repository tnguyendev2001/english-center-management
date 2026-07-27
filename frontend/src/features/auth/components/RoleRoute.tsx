import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../AuthContext'
import type { AccountRole } from '../authTypes'

export function RoleRoute({ roles }: { roles: AccountRole[] }) {
  const { user } = useAuth()

  if (!user || !roles.includes(user.role)) {
    return <Navigate to="/forbidden" replace />
  }

  return <Outlet />
}
