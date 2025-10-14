import { useEffect, useState } from 'react';
import { Toaster } from '@mirel/ui';
import { toast } from 'sonner';
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
import type { DataElement, StencilConfig } from '../types/api';

/**
 * ProMarker main page component
 * Provides UI for stencil selection, parameter input, and code generation
 * 
 * Phase 1 - Step 5: Complete UI implementation
 * Phase 1 - Step 6: React Hook Form + Zod validation integration
 */
export function ProMarkerPage() {
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
    serial: string
  ) => {
    const result = await suggestMutation.mutateAsync({
      stencilCategoy: category,
      stencilCanonicalName: stencil,
      serialNo: serial,
    });

    if (result.data?.model) {
      const model = result.data.model;

      // Update dropdowns
      if (model.fltStrStencilCategory) {
        // For initial load, clear auto-selection to keep dropdowns disabled
        const categories = category === '*' && stencil === '*' && serial === '*'
          ? { ...model.fltStrStencilCategory, selected: '' }
          : model.fltStrStencilCategory;
        setCategories(categories);
      }
      if (model.fltStrStencilCd) {
        const stencils = stencil === '*' && serial === '*'
          ? { ...model.fltStrStencilCd, selected: '' }
          : model.fltStrStencilCd;
        setStencils(stencils);
      }
      if (model.fltStrSerialNo) {
        const serials = serial === '*'
          ? { ...model.fltStrSerialNo, selected: '' }
          : model.fltStrSerialNo;
        setSerials(serials);
      }

      // Update parameters and stencil config
      if (model.params?.childs) {
        setParameters(model.params.childs);
        // Form will be auto-populated with default values via useParameterForm
      }

      if (model.stencil?.config) {
        setStencilConfig(model.stencil.config);
      }
    }
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
        // Always fetch initial categories (backend will auto-reload if needed)
        console.log('[ProMarker] Fetching initial categories...');
        await fetchSuggestData('*', '*', '*');
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
    await fetchSuggestData(value, '*', '*');
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
    await fetchSuggestData(categories.selected, value, '*');
  };

  const handleSerialChange = async (value: string) => {
    if (!value) return;

    // Update selection state flags
    setSerialNoSelected(false);

    // Fetch full stencil configuration with parameters
    await fetchSuggestData(categories.selected, stencils.selected, value);
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

  const handleClearAll = async () => {
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
    await fetchSuggestData('*', '*', '*');
    toast.success('全てクリアしました');
  };

  const handleClearStencil = async () => {
    // Clear only parameters and stencil config
    setParameters([]);
    setStencilConfig(null);
    parameterForm.reset();
    
    // Re-fetch current stencil data
    await fetchSuggestData(categories.selected, stencils.selected, serials.selected);
    toast.success('ステンシル定義を再取得しました');
  };

  const handleReloadStencilMaster = async () => {
    await reloadMutation.mutateAsync();
    
    // Refresh categories after reload
    await fetchSuggestData('*', '*', '*');
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
      await handleClearAll();
      
      // Set selections step by step
      setCategories(prev => ({ ...prev, selected: data.stencilCategory }));
      setCategoryNoSelected(false);
      await fetchSuggestData(data.stencilCategory, '*', '*');
      await new Promise(resolve => setTimeout(resolve, 500));
      
      setStencils(prev => ({ ...prev, selected: data.stencilCd }));
      setStencilNoSelected(false);
      await fetchSuggestData(data.stencilCategory, data.stencilCd, '*');
      await new Promise(resolve => setTimeout(resolve, 500));
      
      setSerials(prev => ({ ...prev, selected: data.serialNo }));
      setSerialNoSelected(false);
      await fetchSuggestData(data.stencilCategory, data.stencilCd, data.serialNo);
      await new Promise(resolve => setTimeout(resolve, 500));
      
      // Set parameter values
      data.dataElements.forEach((elem) => {
        parameterForm.setValue(elem.id, elem.value);
      });
      
      toast.success('JSONを適用しました');
    } catch (error) {
      toast.error('JSON適用中にエラーが発生しました');
      console.error('JSON apply error:', error);
    }
  };

  const isGenerateDisabled = 
    !serials.selected || 
    parameters.length === 0 ||
    !parameterForm.isValid ||
    suggestMutation.isPending ||
    generateMutation.isPending;

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="space-y-2">
        <h1 className="text-4xl font-bold text-foreground">
          ProMarker 払出画面
        </h1>
        <p className="text-muted-foreground">
          ステンシルテンプレートからコードを自動生成
        </p>
      </div>

      {/* Loading Indicator */}
      {(suggestMutation.isPending || generateMutation.isPending) && (
        <div 
          className="flex items-center justify-center p-4 bg-muted/50 rounded-lg"
          data-testid="loading-indicator"
        >
          <svg
            className="mr-2 h-5 w-5 animate-spin text-primary"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
          >
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="4"
            />
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            />
          </svg>
          <span className="text-sm text-muted-foreground">
            {generateMutation.isPending ? 'コード生成中...' : suggestMutation.isPending ? 'データ読み込み中...' : 'ローディング中...'}
          </span>
        </div>
      )}

      {/* Stencil Selection Section */}
      <div className="border rounded-lg p-6 space-y-4">
        <h2 className="text-xl font-semibold">ステンシル選択</h2>
        <StencilSelector
          categories={categories}
          stencils={stencils}
          serials={serials}
          onCategoryChange={handleCategoryChange}
          onStencilChange={handleStencilChange}
          onSerialChange={handleSerialChange}
          disabled={selectorsDisabled}
        />
      </div>

      {/* Stencil Information */}
      {stencilConfig && (
        <StencilInfo config={stencilConfig} />
      )}

      {/* Parameter Input Section */}
      {parameters.length > 0 && (
        <div className="border rounded-lg p-6">
          <ParameterFields
            parameters={parameters}
            form={parameterForm}
            disabled={inputFieldsDisabled}
          />
        </div>
      )}

      {/* Action Buttons */}
      <div className="border rounded-lg p-6">
        <ActionButtons
          onGenerate={handleGenerate}
          onClearAll={handleClearAll}
          onClearStencil={handleClearStencil}
          onReloadStencilMaster={handleReloadStencilMaster}
          onJsonEdit={() => setJsonEditorOpen(true)}
          generateDisabled={isGenerateDisabled}
          generateLoading={generateMutation.isPending}
          reloadLoading={reloadMutation.isPending}
        />
      </div>

      {/* JSON Editor Dialog */}
      <JsonEditor
        open={jsonEditorOpen}
        onOpenChange={setJsonEditorOpen}
        category={categories.selected || ''}
        stencil={stencils.selected || ''}
        serial={serials.selected || ''}
        parameters={parameters}
        onApply={handleJsonApply}
      />

      <Toaster />
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
