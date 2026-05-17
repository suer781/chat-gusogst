import {
  ActionIcon,
  Badge,
  Box,
  Button,
  Combobox,
  type ComboboxProps,
  Divider,
  Flex,
  Group,
  Kbd,
  ScrollArea,
  SegmentedControl,
  SimpleGrid,
  Stack,
  Text,
  TextInput,
  Tooltip,
  useCombobox,
} from '@mantine/core'
import type { ProviderModelInfo } from '@shared/types'
import { IconSearch, IconStar, IconStarFilled, IconSettings } from '@tabler/icons-react'
import clsx from 'clsx'
import { useAtom } from 'jotai'
import { forwardRef, type MouseEvent, type ReactElement, type ReactNode, cloneElement, isValidElement, useCallback, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useProviders } from '@/hooks/useProviders'
import { navigateToSettings } from '@/modals/Settings'
import { collapsedProvidersAtom } from '@/stores/atoms/uiAtoms'
import { ScalableIcon } from '../common/ScalableIcon'
import { ModelItem, SELECTED_BG_CLASS } from './shared'

type FilteredProvider = {
  id: string
  name: string
  isCustom?: boolean
  models?: ProviderModelInfo[]
}

interface DesktopModelSelectorProps {
  children: React.ReactNode
  showAuto?: boolean
  autoText?: string
  selectedProviderId?: string
  selectedModelId?: string
  activeTab: string | null
  search: string
  filteredProviders: FilteredProvider[]
  onTabChange: (tab: string | null) => void
  onSearchChange: (search: string) => void
  onOptionSubmit: (val: string) => void
  onDropdownOpen?: () => void
  modelFilter?: (model: ProviderModelInfo) => boolean
  comboboxProps?: ComboboxProps
  searchPosition?: 'top' | 'bottom'
}

export const DesktopModelSelector = forwardRef<HTMLDivElement, DesktopModelSelectorProps>(
  (
    {
      children,
      showAuto,
      autoText,
      selectedProviderId,
      selectedModelId,
      activeTab,
      search,
      filteredProviders,
      onTabChange,
      onSearchChange,
      onOptionSubmit,
      onDropdownOpen,
      modelFilter,
      comboboxProps,
      searchPosition,
    },
    ref
  ) => {
    const { t } = useTranslation()
    const combobox = useCombobox({
      onDropdownClose: () => {
        combobox.resetSelectedOption()
        onSearchChange('')
      },
      onDropdownOpen: () => {
        onDropdownOpen?.()
      },
    })

    const { providers } = useProviders()

    const [currentSelectedProviderId, setCurrentSelectedProviderId] = useState<string | null>(selectedProviderId ?? null)

    const handleSubmit = useCallback(
      (val: string) => {
        if (val === 'open-settings') {
          navigateToSettings('provider')
        } else {
          onOptionSubmit(val)
        }
        combobox.closeDropdown()
      },
      [onOptionSubmit, combobox]
    )

    useEffect(() => {
      if (currentSelectedProviderId !== selectedProviderId) {
        setCurrentSelectedProviderId(selectedProviderId ?? null)
      }
    }, [selectedProviderId])

    const currentFilteredProvider = filteredProviders.find((p) => p.id === currentSelectedProviderId)

    const displayModels = useMemo(() => {
      if (!currentFilteredProvider) return []
      let models = currentFilteredProvider.models ?? []
      if (modelFilter) models = models.filter(modelFilter)
      return models
    }, [currentFilteredProvider, modelFilter])

    const [favoriteModels, setFavoriteModels] = useAtom(favoriteModelsAtom)

    // Determine which providers to show as tabs
    const tabProviders = useMemo(() => {
      if (activeTab === 'favorite') {
        return filteredProviders.filter((p) => p.models?.some((m) => isModelIdInList(favoriteModels, p.id, m.id)))
      }
      return filteredProviders
    }, [activeTab, filteredProviders, favoriteModels])

    // Auto-select first provider if none selected
    useEffect(() => {
      if (!currentSelectedProviderId && tabProviders.length > 0) {
        setCurrentSelectedProviderId(tabProviders[0].id)
      }
    }, [currentSelectedProviderId, tabProviders])

    const options = (
      <>
        {/* Top bar: search + All/Favorite toggle */}
        <Flex align='center' className='px-xs py-xs'>
          <ScalableIcon icon={IconSearch} className='text-chatbox-tint-gray' />
          <TextInput
            value={search}
            onChange={(event) => onSearchChange(event.currentTarget.value)}
            placeholder={t('Search models') as string}
            variant='unstyled'
            className='flex-1 ml-xs'
            styles={{
              input: {
                padding: 0,
                height: 'auto',
                minHeight: 'auto',
                fontSize: 'var(--mantine-font-size-sm)',
              },
            }}
          />
          <SegmentedControl
            value={activeTab || 'all'}
            onChange={(value) => onTabChange(value)}
            data={[
              { label: t('All'), value: 'all' },
              { label: t('Favorite'), value: 'favorite' },
            ]}
            size='xs'
          />
        </Flex>

        <Divider mx='xs' />

        {/* Provider chips - horizontal 2-row layout */}
        <Box className='px-xs pt-xs pb-4'>
          <SimpleGrid cols={2} spacing={4}>
            {tabProviders.map((provider) => {
              const isActive = currentSelectedProviderId === provider.id
              const iconNode = provider.icon || providers.find((p) => p.id === provider.id)?.icon
              return (
                <Button
                  key={provider.id}
                  size='compact-sm'
                  variant={isActive ? 'filled' : 'default'}
                  color={isActive ? 'chatbox-tint' : undefined}
                  leftSection={
                    iconNode && typeof iconNode === 'string' ? (
                      <img src={iconNode} className='w-4 h-4 rounded-xs' />
                    ) : isValidElement(iconNode) ? (
                      cloneElement(iconNode as ReactElement<{ className?: string }>, {
                        className: 'w-4 h-4',
                      })
                    ) : null
                  }
                  onClick={() => setCurrentSelectedProviderId(provider.id)}
                  className='text-xs'
                  styles={{ root: { height: 28 }, label: { fontWeight: 400, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' } }}
                >
                  {provider.name}
                </Button>
              )
            })}
          </SimpleGrid>
          {tabProviders.length === 0 && (
            <Text size='xs' c='dimmed' className='mx-auto py-sm text-center'>
              {t('No providers available')}
            </Text>
          )}
        </Box>

        <Divider mx='xs' />

        {/* Model list for selected provider */}
        <ScrollArea type='hover' scrollbarSize={6} className='pt-xs' style={{ flex: 1 }}>
          {displayModels.length > 0 ? (
            <Stack gap={0}>
              {displayModels.map((model) => {
                if (!currentSelectedProviderId) return null
                return (
                  <ModelItem
                    key={`${currentSelectedProviderId}/${model.id}`}
                    providerId={currentSelectedProviderId}
                    model={model}
                    selectedModelId={selectedModelId}
                    onSelect={handleSubmit}
                    favoriteModels={favoriteModels}
                    onFavoriteChange={setFavoriteModels}
                  />
                )
              })}
            </Stack>
          ) : (
            <Text size='sm' c='dimmed' className='mx-auto py-lg text-center'>
              {t('No models available')}
            </Text>
          )}
        </ScrollArea>
      </>
    )

    return (
      <Combobox
        ref={ref}
        store={combobox}
        onOptionSubmit={handleSubmit}
        withinPortal={false}
        {...comboboxProps}
        classNames={{
          dropdown: 'chatbox-model-selector-dropdown',
          ...(comboboxProps?.classNames ?? {}),
        }}
        position='bottom'
        width='target'
        styles={{
          dropdown: {
            maxHeight: 'min(calc(100vh - 150px), 500px)',
            overflow: 'hidden',
            display: 'flex',
            flexDirection: 'column',
          },
        }}
      >
        <Combobox.Target>{children}</Combobox.Target>
        <Combobox.Dropdown>{options}</Combobox.Dropdown>
      </Combobox>
    )
  }
) as {
  (props: DesktopModelSelectorProps & { ref?: React.Ref<HTMLDivElement> }): React.JSX.Element
  displayName: string
}

DesktopModelSelector.displayName = 'DesktopModelSelector'
