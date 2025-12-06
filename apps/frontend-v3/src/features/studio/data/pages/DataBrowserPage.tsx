import { useState, useEffect, useCallback } from 'react';
import { modelerApi } from '../../modeler/api/modelerApi';
import type { SchDicModel, SchRecord } from '../../modeler/types/modeler';
import { RecordGrid } from '../../modeler/components/RecordGrid';
import { ModelSelector } from '../../modeler/components/ModelSelector';
import { StudioLayout } from '../../layouts';
import { StudioContextBar } from '../../components';
import { Button, Input } from '@mirel/ui';
import { StudioNavigation } from '../../components/StudioNavigation';

export const DataBrowserPage: React.FC = () => {
  const [models, setModels] = useState<{ value: string; text: string }[]>([]);
  const [selectedModelId, setSelectedModelId] = useState<string>('');
  const [fields, setFields] = useState<SchDicModel[]>([]);
  const [records, setRecords] = useState<SchRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [searchQuery, setSearchQuery] = useState('');
  const [totalRecords, setTotalRecords] = useState(0);

  const loadModelAndRecords = useCallback(async (modelId: string, currentPage: number, currentPagesize: number, currentSearchQuery: string) => {
    try {
      setLoading(true);
      const modelerResponse = await modelerApi.listModel(modelId);
      setFields(modelerResponse.data.modelers);

      const recordsResponse = await modelerApi.list(modelId, currentPage, currentPagesize, currentSearchQuery);
      setRecords(recordsResponse.data.records);
      setTotalRecords(recordsResponse.data.total || 0); // Assuming API returns total count
    } catch (error) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const fetchModels = async () => {
      try {
         const res = await modelerApi.listModels();
         setModels(res.data.models);
      } catch (e) {
         console.error('Failed to load models', e);
         // Fallback or empty
         setModels([]);
      }
    };
    fetchModels();
  }, []);

  useEffect(() => {
    if (selectedModelId) {
      // Reset pagination and search when model changes
      setPage(1);
      setSearchQuery('');
      loadModelAndRecords(selectedModelId, 1, pageSize, '');
    }
  }, [selectedModelId, pageSize, loadModelAndRecords]);

  useEffect(() => {
    if (selectedModelId) {
      loadModelAndRecords(selectedModelId, page, pageSize, searchQuery);
    }
  }, [page, pageSize, searchQuery, selectedModelId, loadModelAndRecords]);

  const handleRowClick = (record: SchRecord) => {
    // The existing router config still has `StudioDataEditPage` for `:modelId/data/:recordId` (old?).
    // No, I kept `StudioDataEditPage` route in legacy, but what about new?
    // I didn't create `DataEditPage` in Task 2.2.
    // I will use `navigate` to a query param or a sub-route if I had one.
    // For now, I will use a placeholder alert or rely on the old route if accessible.
    // Actually, `DataBrowserPage` is likely a list. Edit logic is usually separate.
    // I will navigate to `details/${record.id}` and hope I implement it later?
    // Or just alert.
    alert(`Navigate to record ${record.id} (Not implemented in Phase 2 Router yet)`);
  };

  const handleCreateNew = () => {
    if (selectedModelId) {
      alert(`Create new record for ${selectedModelId} (Not implemented in Phase 2 Router yet)`);
    }
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(1); // Reset to first page on search
    loadModelAndRecords(selectedModelId, 1, pageSize, searchQuery);
  };

  const totalPages = Math.ceil(totalRecords / pageSize);

  return (
    <StudioLayout 
      hideContextBar={true}
      navigation={<StudioNavigation className="h-full" />}
    >
      <div className="flex flex-col h-full overflow-hidden">
        <StudioContextBar
          breadcrumbs={[
            { label: 'Studio', href: '/apps/studio' },
            { label: 'データ', href: '/apps/studio/data' },
          ]}
          title="データブラウザ"
        >
          <Button
            onClick={handleCreateNew}
            disabled={!selectedModelId}
            size="sm"
          >
            新規レコード
          </Button>
        </StudioContextBar>

        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          <div className="flex justify-between items-center p-4 border border-border rounded-lg bg-card shadow-sm">
            <ModelSelector
              models={models}
              selectedModelId={selectedModelId}
              onSelect={setSelectedModelId}
            />
          </div>

          {selectedModelId && (
            <>
              {/* Search Bar */}
              <form onSubmit={handleSearch} className="flex gap-2">
                <Input
                  type="text"
                  placeholder="検索..."
                  className="w-64"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
                <Button type="submit" variant="secondary">
                  検索
                </Button>
              </form>

              <div className="border border-border rounded-lg bg-card shadow-sm overflow-hidden">
                {loading ? (
                  <div className="p-8 text-center text-muted-foreground">読み込み中...</div>
                ) : (
                  <RecordGrid fields={fields} records={records} onRowClick={handleRowClick} />
                )}
              </div>

              {/* Pagination Controls */}
              <div className="flex justify-end items-center gap-4 mt-4">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(p => Math.max(1, p - 1))}
                  disabled={page === 1}
                >
                  前へ
                </Button>
                <span className="text-sm">
                  {page} / {totalPages || 1} ページ
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(p => p + 1)}
                  disabled={page >= totalPages}
                >
                  次へ
                </Button>
                <select
                  value={pageSize}
                  onChange={(e) => setPageSize(Number(e.target.value))}
                  className="p-1 border rounded border-input bg-background text-sm"
                >
                  <option value={10}>10</option>
                  <option value={20}>20</option>
                  <option value={50}>50</option>
                </select>
              </div>
            </>
          )}
        </div>
      </div>
    </StudioLayout>
  );
};
