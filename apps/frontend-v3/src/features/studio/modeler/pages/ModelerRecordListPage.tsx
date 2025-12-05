import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { modelerApi } from '../api/modelerApi';
import type { SchDicModel, SchRecord } from '../types/modeler';
import { RecordGrid } from '../components/RecordGrid';
import { ModelSelector } from '../components/ModelSelector';
import { StudioLayout } from '../../layouts';
import { StudioContextBar } from '../../components';
import { Button, Input } from '@mirel/ui';

export const ModelerRecordListPage: React.FC = () => {
  const navigate = useNavigate();
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
    // TODO: Fetch available models from API
    // For now, hardcode or fetch if endpoint exists
    // modelerApi.getApps().then(...)
    setModels([
      { value: 'sample_model', text: 'Sample Model' },
      // Add more models as needed
    ]);
  }, []);

  useEffect(() => {
    if (selectedModelId) {
      // Reset pagination and search when model changes
      setPage(1);
      setSearchQuery('');
      // Need to call load here or depend on page/size change?
      // Actually page change effect below handles it if we reset page.
      // But if page was already 1, effect below might not trigger if we depend on [page].
      // Let's rely on the below effect or explicity call.
      // If we change selectedModelId, we reset page to 1.
      // If page is already 1, the below effect [page, pageSize] won't fire if pageSize didn't change?
      // Actually below effect depends on `selectedModelId` too? Original code didn't.
      // Let's explicitly call load here, but simpler: depend on everything in one effect?
      // Original logic separated them.
      // I will keep logic but fix deps.
      loadModelAndRecords(selectedModelId, 1, pageSize, '');
    }
  }, [selectedModelId, pageSize, loadModelAndRecords]); // added loadModelAndRecords, pageSize

  useEffect(() => {
    if (selectedModelId) {
      loadModelAndRecords(selectedModelId, page, pageSize, searchQuery);
    }
  }, [page, pageSize, searchQuery, selectedModelId, loadModelAndRecords]); // added missing deps

  const handleRowClick = (record: SchRecord) => {
    navigate(`/apps/modeler/records/${selectedModelId}/${record.id}`);
  };

  const handleCreateNew = () => {
    if (selectedModelId) {
      navigate(`/apps/modeler/records/${selectedModelId}/new`);
    }
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(1); // Reset to first page on search
    loadModelAndRecords(selectedModelId, 1, pageSize, searchQuery);
  };

  const totalPages = Math.ceil(totalRecords / pageSize);

  return (
    <StudioLayout hideContextBar={true}>
      <div className="flex flex-col h-full overflow-hidden">
        <StudioContextBar
          breadcrumbs={[
            { label: 'Studio', href: '/apps/studio' },
            { label: 'Modeler', href: '/apps/studio/modeler' },
            { label: 'データ管理' },
          ]}
          title="データ管理"
        >
          <Button
            onClick={handleCreateNew}
            disabled={!selectedModelId}
            size="sm"
          >
            新規作成
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
                  placeholder="Search..."
                  className="w-64"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
                <Button type="submit" variant="secondary">
                  Search
                </Button>
              </form>

              <div className="border border-border rounded-lg bg-card shadow-sm overflow-hidden">
                {loading ? (
                  <div className="p-8 text-center text-muted-foreground">Loading...</div>
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
                  Previous
                </Button>
                <span className="text-sm">
                  Page {page} of {totalPages || 1}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(p => p + 1)}
                  disabled={page >= totalPages}
                >
                  Next
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
