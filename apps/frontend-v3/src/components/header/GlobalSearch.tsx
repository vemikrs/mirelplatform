import { Search } from 'lucide-react';
import { Input } from '@mirel/ui';

export function GlobalSearch() {
  return (
    <div className="relative hidden md:block w-full md:w-[300px] lg:w-[400px]">
      <Search className="absolute left-2.5 top-2.5 size-4 text-muted-foreground" />
      <Input
        type="search"
        placeholder="検索 (Ctrl+K)"
        className="w-full bg-surface-subtle pl-9"
      />
    </div>
  );
}
