import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast, Card, CardHeader, CardTitle, CardContent, SectionHeading, Badge, StepIndicator, type StepState } from '@mirel/ui';
import { useSuggest } from '../hooks/useSuggest';
import { useGenerate } from '../hooks/useGenerate';
import { useReloadStencilMaster } from '../hooks/useReloadStencilMaster';
import { useParameterForm } from '../hooks/useParameterForm';
import { StencilSelector, type ValueTextItems } from '../components/StencilSelector';
import { ParameterFields } from '../components/ParameterFields';
import { ActionButtons } from '../components/ActionButtons';
import { StencilInfo } from '../components/StencilInfo';
import { JsonEditor } from '../components/JsonEditor';
import { ErrorBoundary } from '../components/ErrorBoundary';
// import { updateFileNames } from '../utils/parameter';
import type { DataElement, StencilConfig, SuggestResult } from '../types/api';
import type { ApiResponse, ModelWrapper } from '@/lib/api/types';
import { toastMessages } from '../constants/toastMessages';

/**
 * ProMarker main page component
 * Provides UI for stencil selection, parameter input, and code generation
 * 
 * Phase 1 - Step 5: Complete UI implementation
 * Phase 1 - Step 6: React Hook Form + Zod validation integration
 */
export function ProMarkerPage() {
  const navigate = useNavigate();
  const suggestMutation = useSuggest();
  const generateMutation = useGenerate();
  const reloadMutation = useReloadStencilMaster();

  // Selection state
  const [categories, setCategories] = useState<ValueTextItems>({ items: [], selected: '' });
  const [stencils, setStencils] = useState<ValueTextItems>({ items: [], selected: '' });
  const [serials, setSerials] = useState<ValueTextItems>({ items: [], selected: '' });

  // Parameters and stencil config
  const [parameters, setParameters] = useState<DataElement[]>([]);
  const [stencilConfig, setStencilConfig] = useState<StencilConfig | null>(null);

  // File management system (Vue.js compatible) - 準備済み
  // const [fileNames, setFileNames] = useState<Record<string, string>>({});

  // Selection state flags (Vue.js compatible)
  const [, setCategoryNoSelected] = useState(true);
  const [, setStencilNoSelected] = useState(true);
  const [, setSerialNoSelected] = useState(true);

  // JSON Editor state
  const [jsonEditorOpen, setJsonEditorOpen] = useState(false);

  // Form validation (Step 6)
  const parameterForm = useParameterForm(parameters);

  // Disabled state conditions
  const selectorsDisabled = suggestMutation.isPending;
  const inputFieldsDisabled = generateMutation.isPending;

  const fetchSuggestData = async (
    category: string,
    stencil: string,
    serial: string,
    selectFirstIfWildcard: boolean = false
  ) => {
    const result = await suggestMutation.mutateAsync({
      stencilCategoy: category,
      stencilCanonicalName: stencil,
      serialNo: serial,
      selectFirstIfWildcard,
    });

    return updateState(result);
  };

  const updateState = (result: ApiResponse<ModelWrapper<SuggestResult>>) => {
    if (!result.data?.model) return;
    
    const model = result.data.model;

    if (model.fltStrStencilCategory) setCategories(model.fltStrStencilCategory);
    if (model.fltStrStencilCd) setStencils(model.fltStrStencilCd);
    if (model.fltStrSerialNo) setSerials(model.fltStrSerialNo);

    // params / stencil は serial 確定時のみ入る
    if (model.params?.childs) setParameters(model.params.childs);
    else setParameters([]);
    if (model.stencil?.config) {
      console.log('[ProMarker] Stencil config loaded:', model.stencil.config);
      setStencilConfig(model.stencil.config);
    } else {
      console.log('[ProMarker] No stencil config in response');
      setStencilConfig(null);
    }

    return result;
  };

  useEffect(() => {
    // Set page title for E2E test verification
    document.title = 'ProMarker - 払出画面';

    // Prevent double execution in React 18 Strict Mode
    let cancelled = false;
    let initStarted = false;

    const initialize = async () => {
      if (cancelled) return;
      
      // Prevent multiple concurrent initialization attempts
      if (initStarted) {
        console.log('[ProMarker] Initialization already in progress, waiting...');
        return;
      }
      initStarted = true;

      try {
        // 初期ロード: 全未選択、リストのみ取得
        console.log('[ProMarker] Initial suggest (empty)...');
        await fetchSuggestData('*', '*', '*', false);
        if (!cancelled) {
          console.log('[ProMarker] Initial data fetch complete');
        }
      } catch (error) {
        if (!cancelled) {
          console.error('[ProMarker] Initialization error:', error);
        }
      }
    };

    initialize();

    // Cleanup function to prevent state updates after unmount
    return () => {
      cancelled = true;
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleCategoryChange = async (value: string) => {
    if (!value) return;

    // Update selection state flags
    setCategoryNoSelected(false);
    setStencilNoSelected(true);
    setSerialNoSelected(true);

    // Reset dependent selections
    setStencils({ items: [], selected: '' });
    setSerials({ items: [], selected: '' });
    setParameters([]);
    setStencilConfig(null);
    parameterForm.reset();

    // Fetch stencils for selected category
    // 1st call: get stencil list, auto-select first
    const r1 = await fetchSuggestData(value, '*', '*', true);
    const autoStencil = r1?.data?.model?.fltStrStencilCd?.selected;
    if (autoStencil) {
      setStencilNoSelected(false);
      // 2nd call: get serial list & params (auto-select serial)
      await fetchSuggestData(value, autoStencil, '*', true);
    }
  };

  const handleStencilChange = async (value: string) => {
    if (!value) return;

    // Update selection state flags
    setStencilNoSelected(false);
    setSerialNoSelected(true);

    // Reset dependent selections
    setSerials({ items: [], selected: '' });
    setParameters([]);
    setStencilConfig(null);
    parameterForm.reset();

    // Fetch serials for selected stencil
    await fetchSuggestData(categories.selected, value, '*', true);
  };

  const handleSerialChange = async (value: string) => {
    if (!value) return;

    // Update selection state flags
    setSerialNoSelected(false);

    // Fetch full stencil configuration with parameters
    await fetchSuggestData(categories.selected, stencils.selected, value, false);
  };

  const handleGenerate = async () => {
    if (!serials.selected) return;

    // Validate form before submission
    const isValid = await parameterForm.validateAll();
    if (!isValid) {
      return;
    }

    try {
      const formValues = parameterForm.getValues();

      await generateMutation.mutateAsync({
        stencilCategoy: categories.selected,
        stencilCanonicalName: stencils.selected,
        serialNo: serials.selected,
        ...formValues,
      });
      
      console.log('Generate completed successfully - ready for next execution');
    } catch (error) {
      console.error('Generate failed:', error);
      // Error is already handled by useGenerate hook
      // Ensure UI state is ready for next attempt
    }
  };

  const handleClearAll = async ({ silent = false }: { silent?: boolean } = {}) => {
    // Reset all selections and values
    setCategories({ items: categories.items, selected: '' });
    setStencils({ items: [], selected: '' });
    setSerials({ items: [], selected: '' });
    setParameters([]);
    setStencilConfig(null);
    // setFileNames({});
    
    // Reset selection state flags
    setCategoryNoSelected(true);
    setStencilNoSelected(true);
    setSerialNoSelected(true);
    
    parameterForm.clearAll();

    // Refresh data
    await fetchSuggestData('*', '*', '*', false);
    if (!silent) {
      toast({
        ...toastMessages.clearAll,
      });
    }
  };

  const handleClearStencil = async () => {
    // Clear only parameters and stencil config
    setParameters([]);
    setStencilConfig(null);
    parameterForm.reset();
    
    // Re-fetch current stencil data
    await fetchSuggestData(categories.selected, stencils.selected, serials.selected, false);
    toast({
      ...toastMessages.clearStencil,
    });
  };

  const handleReloadStencilMaster = async () => {
    await reloadMutation.mutateAsync();
    
    // Refresh categories after reload
    await fetchSuggestData('*', '*', '*');
  };

  const handleManageStencils = () => {
    navigate('/promarker/stencils');
  };

  // File upload handler (準備済み - 将来のファイルアップロード機能用)
  // const handleFileUploaded = (parameterId: string, fileId: string, fileName: string) => {
  //   parameterForm.setValue(parameterId, fileId);
  //   setFileNames(prev => updateFileNames(prev, fileId, fileName));
  // };

  // JSON Editor handlers
  const handleJsonApply = async (data: {
    stencilCategory: string;
    stencilCd: string;
    serialNo: string;
    dataElements: Array<{id: string; value: string}>;
  }) => {
    try {
      // Clear all first
      await handleClearAll({ silent: true });
      
      // Set selections step by step (explicit, no auto-select except where needed)
      setCategories(prev => ({ ...prev, selected: data.stencilCategory }));
      setCategoryNoSelected(false);
      await fetchSuggestData(data.stencilCategory, '*', '*', true);
      await new Promise(resolve => setTimeout(resolve, 500));
      
      setStencils(prev => ({ ...prev, selected: data.stencilCd }));
      setStencilNoSelected(false);
      await fetchSuggestData(data.stencilCategory, data.stencilCd, '*', true);
      await new Promise(resolve => setTimeout(resolve, 500));
      
      setSerials(prev => ({ ...prev, selected: data.serialNo }));
      setSerialNoSelected(false);
      await fetchSuggestData(data.stencilCategory, data.stencilCd, data.serialNo, false);
      await new Promise(resolve => setTimeout(resolve, 500));
      
      // Set parameter values
      data.dataElements.forEach((elem) => {
        parameterForm.setValue(elem.id, elem.value);
      });
      
    } catch (error) {
      console.error('JSON apply error:', error);
      throw error instanceof Error ? error : new Error('JSON適用中にエラーが発生しました');
    }
  };

  const isGenerateDisabled =
    !serials.selected ||
    parameters.length === 0 ||
    !parameterForm.isValid ||
    suggestMutation.isPending ||
    generateMutation.isPending;

  const stepIndicatorSteps = useMemo(() => {
    const selectionComplete = Boolean(categories.selected && stencils.selected && serials.selected);
    const parametersReady = selectionComplete && parameters.length > 0;
    return [
      {
        id: 'select',
        title: 'ステンシル選択',
        description: 'カテゴリ・ステンシル・シリアルを選びます。',
        state: (selectionComplete ? 'complete' : 'current') as StepState,
      },
      {
        id: 'details',
        title: 'パラメータ入力',
        description: '必須項目と入力ルールを確認しながら値を設定します。',
        state: (parametersReady ? (parameterForm.isValid ? 'complete' : 'current') : selectionComplete ? 'current' : 'upcoming') as StepState,
      },
      {
        id: 'execute',
        title: '生成',
        description: '入力内容でコード生成を実行します。',
        state: (generateMutation.isSuccess ? 'complete' : parametersReady ? 'current' : 'upcoming') as StepState,
      },
    ];
  }, [categories.selected, stencils.selected, serials.selected, parameters.length, parameterForm.isValid, generateMutation.isSuccess]);

  const isLoading = suggestMutation.isPending || generateMutation.isPending;

  return (
    <div className="space-y-8">
      <SectionHeading
        eyebrow="ProMarker"
        title="ProMarker ワークスペース"
        description="ステンシルを選択し、業務アプリケーション向けのコードを生成します。"
      />

      <StepIndicator steps={stepIndicatorSteps} />

      {isLoading ? (
        <div
          className="flex items-center gap-3 rounded-xl border border-outline/60 bg-surface-subtle px-4 py-3 text-sm text-muted-foreground"
          data-testid="loading-indicator"
        >
          <Badge variant="info">処理中</Badge>
          {generateMutation.isPending ? 'コード生成中です…' : 'データを取得しています…'}
        </div>
      ) : null}

      {/* ステンシル選択・説明 と パラメータ入力の2カラムレイアウト */}
      <div className={`grid grid-cols-1 gap-6 ${parameters.length > 0 ? 'md:grid-cols-2' : ''}`}>
        {/* 左側: ステンシル選択 + ステンシル説明 (縦並び) */}
        <div className="space-y-6">
          <Card>
            <CardHeader className="space-y-2">
              <CardTitle>ステンシル選択</CardTitle>
              <p className="text-sm text-muted-foreground">
                カテゴリ → ステンシル → シリアルの順で選択してください。
              </p>
            </CardHeader>
            <CardContent>
              <StencilSelector
                categories={categories}
                stencils={stencils}
                serials={serials}
                onCategoryChange={handleCategoryChange}
                onStencilChange={handleStencilChange}
                onSerialChange={handleSerialChange}
                disabled={selectorsDisabled}
              />
            </CardContent>
          </Card>
          <StencilInfo config={stencilConfig} />
        </div>

        {/* 右側: パラメータ入力 (スティッキー) - パラメータがある場合のみ表示 */}
        {parameters.length > 0 ? (
          <div className="md:sticky md:top-6 md:self-start md:max-h-[calc(100vh-8rem)] md:overflow-y-auto">
            <ParameterFields parameters={parameters} form={parameterForm} disabled={inputFieldsDisabled} />
          </div>
        ) : null}
      </div>

      <ActionButtons
        onGenerate={handleGenerate}
        onClearAll={handleClearAll}
        onClearStencil={handleClearStencil}
        onReloadStencilMaster={handleReloadStencilMaster}
        onJsonEdit={() => setJsonEditorOpen(true)}
        onManageStencils={handleManageStencils}
        generateDisabled={isGenerateDisabled}
        generateLoading={generateMutation.isPending}
        reloadLoading={reloadMutation.isPending}
      />

      <JsonEditor
        open={jsonEditorOpen}
        onOpenChange={setJsonEditorOpen}
        category={categories.selected || ''}
        stencil={stencils.selected || ''}
        serial={serials.selected || ''}
        parameters={parameters}
        onApply={handleJsonApply}
      />
    </div>
  );
}

// Wrap with ErrorBoundary
export default function ProMarkerPageWithErrorBoundary() {
  return (
    <ErrorBoundary>
      <ProMarkerPage />
    </ErrorBoundary>
  );
}
