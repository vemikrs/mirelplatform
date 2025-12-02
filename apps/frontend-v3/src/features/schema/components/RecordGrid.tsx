import React, { useMemo } from 'react';
import { DataTable } from '@mirel/ui';
import type { ColumnDef } from '@tanstack/react-table';
import type { SchDicModel, SchRecord } from '../types/schema';

interface RecordGridProps {
  fields: SchDicModel[];
  records: SchRecord[];
  onRowClick: (record: SchRecord) => void;
}

export const RecordGrid: React.FC<RecordGridProps> = ({ fields, records, onRowClick }) => {
  const columns = useMemo<ColumnDef<SchRecord>[]>(() => {
    const headerFields = fields.filter((f) => f.isHeader);
    
    // If no header fields defined, use first 5 fields
    const displayFields = headerFields.length > 0 ? headerFields : fields.slice(0, 5);

    return displayFields.map((field) => ({
      accessorKey: `recordData.${field.fieldId}`,
      header: field.fieldName,
      cell: (info) => {
        const value = info.getValue();
        return value !== undefined && value !== null ? String(value) : '';
      },
    }));
  }, [fields]);

  return (
    <div className="w-full">
      <DataTable
        columns={columns}
        data={records}
        onRowClick={onRowClick}
      />
    </div>
  );
};
