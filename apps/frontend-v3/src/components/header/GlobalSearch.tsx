import { Search } from 'lucide-react';
import { Input } from '@mirel/ui';

export function GlobalSearch() {
  return (
    <div className="relative w-full">
      <Search className="absolute left-2.5 top-2.5 size-4 text-muted-foreground" />
      <Input
        type="search"
        placeholder="検索 (Ctrl+K)"
        className="w-full bg-surface-subtle pl-9"
      />
    </div>
  );
}
