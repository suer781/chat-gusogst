import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import './Layout.css'

const tabs = [
  { path: '/', label: '聊天', icon: '💬' },
  { path: '/persona', label: '人设', icon: '👤' },
]

export default function Layout() {
  const location = useLocation()
  const navigate = useNavigate()
  const hideTabs = location.pathname === '/settings'

  return (
    <div className="layout">
      <div className="layout-content">
        <Outlet />
      </div>
      {!hideTabs && (
        <nav className="tab-bar">
          {tabs.map(tab => (
            <button
              key={tab.path}
              className={'tab-item' + (location.pathname === tab.path ? ' active' : '')}
              onClick={() => navigate(tab.path)}
            >
              <span className="tab-icon">{tab.icon}</span>
              <span className="tab-label">{tab.label}</span>
            </button>
          ))}
        </nav>
      )}
    </div>
  )
}