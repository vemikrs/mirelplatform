import React from 'react';
import type { SchDicModel, SchRecord } from '../types/schema';

interface RecordGridProps {
  fields: SchDicModel[];
  records: SchRecord[];
  onRowClick: (record: SchRecord) => void;
}

export const RecordGrid: React.FC<RecordGridProps> = ({ fields, records, onRowClick }) => {
  const headerFields = fields.filter((f) => f.isHeader);

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full bg-white border">
        <thead>
          <tr>
            {headerFields.map((field) => (
              <th key={field.fieldId} className="px-4 py-2 border-b text-left">
                {field.fieldName}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {records.map((record) => (
            <tr
              key={record.id}
              onClick={() => onRowClick(record)}
              className="cursor-pointer hover:bg-gray-100"
            >
              {headerFields.map((field) => (
                <td key={field.fieldId} className="px-4 py-2 border-b">
                  {record.recordData[field.fieldId]?.toString() || ''}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
