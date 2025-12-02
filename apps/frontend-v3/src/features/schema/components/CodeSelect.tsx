import React, { useEffect, useState } from 'react';
import { schemaApi } from '../api/schemaApi';
import type { SchDicCode } from '../types/schema';

interface CodeSelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  codeGroupId: string;
}

export const CodeSelect = React.forwardRef<HTMLSelectElement, CodeSelectProps>(
  ({ codeGroupId, className, ...props }, ref) => {
    const [options, setOptions] = useState<SchDicCode[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
      if (codeGroupId) {
        loadOptions();
      }
    }, [codeGroupId]);

    const loadOptions = async () => {
      try {
        setLoading(true);
        const res = await schemaApi.listCode(codeGroupId);
        setOptions(res.data.valueTexts);
      } catch (error) {
        console.error('Failed to load code options:', error);
      } finally {
        setLoading(false);
      }
    };

    return (
      <select
        ref={ref}
        className={className}
        disabled={loading || props.disabled}
        {...props}
      >
        <option value="">Select...</option>
        {options.map((opt) => (
          <option key={opt.code} value={opt.code}>
            {opt.text || opt.code}
          </option>
        ))}
      </select>
    );
  }
);

CodeSelect.displayName = 'CodeSelect';
