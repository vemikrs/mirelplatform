import { useEffect, useState } from 'react';
import { Toaster } from '@mirel/ui';
import { useSuggest } from '../hooks/useSuggest';
import { useGenerate } from '../hooks/useGenerate';
import { useReloadStencilMaster } from '../hooks/useReloadStencilMaster';
import { StencilSelector, type ValueTextItems } from '../components/StencilSelector';
import { ParameterFields } from '../components/ParameterFields';
import { ActionButtons } from '../components/ActionButtons';
import { StencilInfo } from '../components/StencilInfo';
import type { DataElement, StencilConfig } from '../types/api';

/**
 * ProMarker main page component
 * Provides UI for stencil selection, parameter input, and code generation
 * 
 * Phase 1 - Step 5: Complete UI implementation
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
  const [parameterValues, setParameterValues] = useState<Record<string, string>>({});
  const [stencilConfig, setStencilConfig] = useState<StencilConfig | null>(null);

  useEffect(() => {
    // Set page title for E2E test verification
    document.title = 'ProMarker - 払出画面';

    // Fetch initial category list only (no auto-selection)
    // Pass empty strings to get only categories without selecting first item
    fetchSuggestData('*', '*', '*');
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

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
        
        // Initialize parameter values with defaults
        const initialValues: Record<string, string> = {};
        model.params.childs.forEach((param) => {
          if (param.value) {
            initialValues[param.id] = param.value;
          }
        });
        setParameterValues(initialValues);
      }

      if (model.stencil?.config) {
        setStencilConfig(model.stencil.config);
      }
    }
  };

  const handleCategoryChange = async (value: string) => {
    if (!value) return;

    // Reset dependent selections
    setStencils({ items: [], selected: '' });
    setSerials({ items: [], selected: '' });
    setParameters([]);
    setParameterValues({});
    setStencilConfig(null);

    // Fetch stencils for selected category
    await fetchSuggestData(value, '*', '*');
  };

  const handleStencilChange = async (value: string) => {
    if (!value) return;

    // Reset dependent selections
    setSerials({ items: [], selected: '' });
    setParameters([]);
    setParameterValues({});
    setStencilConfig(null);

    // Fetch serials for selected stencil
    await fetchSuggestData(categories.selected, value, '*');
  };

  const handleSerialChange = async (value: string) => {
    if (!value) return;

    // Fetch full stencil configuration with parameters
    await fetchSuggestData(categories.selected, stencils.selected, value);
  };

  const handleParameterChange = (parameterId: string, value: string) => {
    setParameterValues((prev) => ({
      ...prev,
      [parameterId]: value,
    }));
  };

  const handleGenerate = async () => {
    if (!serials.selected) return;

    await generateMutation.mutateAsync({
      stencilCategoy: categories.selected,
      stencilCanonicalName: stencils.selected,
      serialNo: serials.selected,
      ...parameterValues,
    });
  };

  const handleClearAll = () => {
    // Reset all selections and values
    setCategories({ items: categories.items, selected: '' });
    setStencils({ items: [], selected: '' });
    setSerials({ items: [], selected: '' });
    setParameters([]);
    setParameterValues({});
    setStencilConfig(null);
  };

  const handleReloadStencilMaster = async () => {
    await reloadMutation.mutateAsync();
    
    // Refresh categories after reload
    await fetchSuggestData('*', '*', '*');
  };

  const isGenerateDisabled = 
    !serials.selected || 
    parameters.length === 0 ||
    suggestMutation.isPending ||
    generateMutation.isPending;

  const isLoading = suggestMutation.isPending;

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
      {isLoading && (
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
          <span className="text-sm text-muted-foreground">読み込み中...</span>
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
          disabled={isLoading}
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
            values={parameterValues}
            onValueChange={handleParameterChange}
            disabled={isLoading || generateMutation.isPending}
          />
        </div>
      )}

      {/* Action Buttons */}
      <div className="border rounded-lg p-6">
        <ActionButtons
          onGenerate={handleGenerate}
          onClearAll={handleClearAll}
          onReloadStencilMaster={handleReloadStencilMaster}
          generateDisabled={isGenerateDisabled}
          generateLoading={generateMutation.isPending}
          reloadLoading={reloadMutation.isPending}
        />
      </div>

      <Toaster />
    </div>
  );
}
