import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getData, deleteData } from '@/lib/api/data';
import { getModel } from '@/lib/api/model';
import { Button, Card } from '@mirel/ui';
import { Plus, Trash2, Edit, ArrowLeft } from 'lucide-react';
import {
  useReactTable,
  getCoreRowModel,
  flexRender,
  createColumnHelper,
} from '@tanstack/react-table';
import { toast } from 'sonner';

export const StudioDataListPage: React.FC = () => {
  const { modelId } = useParams<{ modelId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // Fetch Model Definition to know columns
  const { data: modelResponse } = useQuery({
    queryKey: ['model', modelId],
    queryFn: () => modelId ? getModel(modelId) : Promise.resolve(null),
    enabled: !!modelId,
  });

  // Fetch Data
  const { data: dataResponse, isLoading } = useQuery({
    queryKey: ['data', modelId],
    queryFn: () => modelId ? getData(modelId) : Promise.resolve(null),
    enabled: !!modelId,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteData(modelId!, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['data', modelId] });
      toast.success('Record deleted');
    },
  });

  const model = modelResponse?.data;
  const data = dataResponse?.data || [];

  // Dynamic Columns
  const columns = React.useMemo(() => {
    if (!model?.fields) return [];
    
    const helper = createColumnHelper<any>();
    
    const fieldCols = model.fields.map((field: any) => 
      helper.accessor(field.fieldCode, {
        header: field.fieldName,
        cell: info => info.getValue(),
      })
    );

    const actionCol = helper.display({
      id: 'actions',
      header: 'Actions',
      cell: (info) => (
        <div className="flex gap-2">
          <Button 
            variant="ghost" 
            size="icon"
            onClick={() => navigate(`/apps/studio/${modelId}/data/${info.row.original.id}`)}
          >
            <Edit className="size-4" />
          </Button>
          <Button 
            variant="ghost" 
            size="icon"
            className="text-red-500 hover:bg-red-50"
            onClick={() => {
              if (confirm('Are you sure?')) {
                deleteMutation.mutate(info.row.original.id);
              }
            }}
          >
            <Trash2 className="size-4" />
          </Button>
        </div>
      ),
    });

    return [...fieldCols, actionCol];
  }, [model, modelId, navigate, deleteMutation]);

  const table = useReactTable({
    data,
    columns,
    getCoreRowModel: getCoreRowModel(),
  });

  if (!modelId) return <div>Invalid Model ID</div>;
  if (isLoading) return <div>Loading...</div>;

  return (
    <div className="h-[calc(100vh-4rem)] flex flex-col">
      <div className="h-14 border-b bg-background flex items-center justify-between px-4">
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="icon" onClick={() => navigate(`/apps/studio/${modelId}`)}>
            <ArrowLeft className="size-4" />
          </Button>
          <h1 className="font-semibold text-lg">Data Browser: {model?.header?.modelName}</h1>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={() => {
            if (modelId) {
              import('@/lib/api/data').then(({ exportDataCsv }) => {
                exportDataCsv(modelId).then((response) => {
                  const url = window.URL.createObjectURL(new Blob([response.data]));
                  const link = document.createElement('a');
                  link.href = url;
                  link.setAttribute('download', `${modelId}.csv`);
                  document.body.appendChild(link);
                  link.click();
                  link.remove();
                });
              });
            }
          }}>
            Export CSV
          </Button>
          <div className="relative">
            <input
              type="file"
              accept=".csv"
              className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file && modelId) {
                  import('@/lib/api/data').then(({ importDataCsv }) => {
                    importDataCsv(modelId, file).then(() => {
                      queryClient.invalidateQueries({ queryKey: ['data', modelId] });
                      toast.success('CSV Imported');
                    }).catch(() => toast.error('Import failed'));
                  });
                }
                e.target.value = ''; // Reset
              }}
            />
            <Button variant="outline">Import CSV</Button>
          </div>
          <Button onClick={() => navigate(`/apps/studio/${modelId}/data/new`)} className="gap-2">
            <Plus className="size-4" />
            New Record
          </Button>
        </div>
      </div>

      <div className="flex-1 overflow-auto p-8 bg-muted/30">
        <Card className="max-w-6xl mx-auto overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead className="bg-muted text-muted-foreground uppercase">
                {table.getHeaderGroups().map(headerGroup => (
                  <tr key={headerGroup.id}>
                    {headerGroup.headers.map(header => (
                      <th key={header.id} className="px-6 py-3 font-medium">
                        {flexRender(header.column.columnDef.header, header.getContext())}
                      </th>
                    ))}
                  </tr>
                ))}
              </thead>
              <tbody className="divide-y divide-border">
                {table.getRowModel().rows.map(row => (
                  <tr key={row.id} className="bg-card hover:bg-muted/50">
                    {row.getVisibleCells().map(cell => (
                      <td key={cell.id} className="px-6 py-4 whitespace-nowrap text-foreground">
                        {flexRender(cell.column.columnDef.cell, cell.getContext())}
                      </td>
                    ))}
                  </tr>
                ))}
                {data.length === 0 && (
                  <tr>
                    <td colSpan={columns.length} className="px-6 py-8 text-center text-muted-foreground">
                      No data found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </Card>
      </div>
    </div>
  );
};
