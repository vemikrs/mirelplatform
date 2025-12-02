import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { schemaApi } from '../api/schemaApi';
import type { SchDicModel, SchRecord } from '../types/schema';
import { RecordGrid } from '../components/RecordGrid';
import { ModelSelector } from '../components/ModelSelector';
import { SchemaLayout } from '../components/layout/SchemaLayout';

export const SchemaRecordListPage: React.FC = () => {
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

  useEffect(() => {
    // TODO: Fetch available models from API
    // For now, hardcode or fetch if endpoint exists
    // schemaApi.getApps().then(...)
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
      loadModelAndRecords(selectedModelId, 1, pageSize, '');
    }
  }, [selectedModelId]);

  useEffect(() => {
    if (selectedModelId) {
      loadModelAndRecords(selectedModelId, page, pageSize, searchQuery);
    }
  }, [page, pageSize]); // Reload when page/size changes

  const loadModelAndRecords = async (modelId: string, currentPage: number, currentPagesize: number, currentSearchQuery: string) => {
    try {
      setLoading(true);
      const schemaResponse = await schemaApi.listSchema(modelId);
      setFields(schemaResponse.data.schemas);

      const recordsResponse = await schemaApi.list(modelId, currentPage, currentPagesize, currentSearchQuery);
      setRecords(recordsResponse.data.records);
      setTotalRecords(recordsResponse.data.total || 0); // Assuming API returns total count
    } catch (error) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleRowClick = (record: SchRecord) => {
    navigate(`/apps/schema/records/${selectedModelId}/${record.id}`);
  };

  const handleCreateNew = () => {
    if (selectedModelId) {
      navigate(`/apps/schema/records/${selectedModelId}/new`);
    }
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(1); // Reset to first page on search
    loadModelAndRecords(selectedModelId, 1, pageSize, searchQuery);
  };

  const totalPages = Math.ceil(totalRecords / pageSize);

  return (
    <SchemaLayout>
      <div className="p-6 space-y-6">
        <div className="flex items-center gap-4 mb-6">
          <h1 className="text-2xl font-bold text-foreground">データ管理</h1>
        </div>

        <div className="flex justify-between items-center mb-4 p-4 border border-border rounded-lg bg-card shadow-sm">
          <ModelSelector
            models={models}
            selectedModelId={selectedModelId}
            onSelect={setSelectedModelId}
          />
          <button
            onClick={handleCreateNew}
            disabled={!selectedModelId}
            className="px-4 py-2 bg-primary text-primary-foreground rounded hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors shadow-sm"
          >
            新規作成
          </button>
        </div>

        {selectedModelId && (
          <>
            {/* Search Bar */}
            <form onSubmit={handleSearch} className="mb-4 flex gap-2">
              <input
                type="text"
                placeholder="Search..."
                className="p-2 border rounded w-64 border-input bg-background"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
              <button type="submit" className="px-4 py-2 bg-secondary text-secondary-foreground rounded hover:bg-secondary/80">
                Search
              </button>
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
              <button
                onClick={() => setPage(p => Math.max(1, p - 1))}
                disabled={page === 1}
                className="px-3 py-1 border rounded disabled:opacity-50 hover:bg-accent"
              >
                Previous
              </button>
              <span className="text-sm">
                Page {page} of {totalPages || 1}
              </span>
              <button
                onClick={() => setPage(p => p + 1)}
                disabled={page >= totalPages}
                className="px-3 py-1 border rounded disabled:opacity-50 hover:bg-accent"
              >
                Next
              </button>
              <select
                value={pageSize}
                onChange={(e) => setPageSize(Number(e.target.value))}
                className="p-1 border rounded border-input bg-background"
              >
                <option value={10}>10</option>
                <option value={20}>20</option>
                <option value={50}>50</option>
              </select>
            </div>
          </>
        )}
      </div>
    </SchemaLayout>
  );
};
