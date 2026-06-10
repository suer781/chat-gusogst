import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './ui/App'
import './ui/theme.css'      // ← 必须最先加载：声明所有 CSS 变量
import './ui/tailwind.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
