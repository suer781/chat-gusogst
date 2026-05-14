import React, { useState } from 'react'
import SettingsList from './SettingsList'
import ModelSettings from './ModelSettings'
import SearchSettings from './SearchSettings'
import ChatSettings from './ChatSettings'
import DocumentSettings from './DocumentSettings'
import GeneralSettings from './GeneralSettings'
import AboutView from './AboutView'

type SubPage = 'list' | 'model' | 'search' | 'chat' | 'document' | 'general' | 'about'

/** 设置页路由容器 — 仿 Chatbox 多级菜单 */
export default function SettingsView() {
  const [page, setPage] = useState<SubPage>('list')

  const goBack = () => setPage('list')

  switch (page) {
    case 'model':
      return <ModelSettings onBack={goBack} />
    case 'search':
      return <SearchSettings onBack={goBack} />
    case 'chat':
      return <ChatSettings onBack={goBack} />
    case 'document':
      return <DocumentSettings onBack={goBack} />
    case 'general':
      return <GeneralSettings onBack={goBack} />
    case 'about':
      return <AboutView onBack={goBack} />
    default:
      return <SettingsList onNavigate={setPage} />
  }
}
