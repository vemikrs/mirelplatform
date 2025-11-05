import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { ParameterFields } from '../ParameterFields'
import { useParameterForm } from '../../hooks/useParameterForm'
import type { DataElement } from '../../types/api'

function Wrapper({ parameters }: { parameters: DataElement[] }) {
  const form = useParameterForm(parameters)
  return <ParameterFields parameters={parameters} form={form} />
}

describe('ParameterFields', () => {
  const parameters: DataElement[] = [
    {
      id: 'appName',
      name: 'アプリ名',
      valueType: 'text',
      value: '',
      placeholder: '例: order-service',
      note: '生成するサービスの表示名を入力',
      nodeType: 'ELEMENT',
      validation: {
        required: true,
        minLength: 3,
        pattern: '^[a-z]+$',
        errorMessage: '英小文字のみ利用可能',
      },
    },
  ]

  it('shows note and validation hints derived from YAML metadata', () => {
    render(<Wrapper parameters={parameters} />)

    expect(screen.getByText('アプリ名')).toBeInTheDocument()
    expect(screen.getByText('生成するサービスの表示名を入力')).toBeInTheDocument()
    expect(screen.getByText('必須')).toBeInTheDocument()
    expect(screen.getByText(/英小文字のみ利用可能/)).toBeInTheDocument()
  })
})
