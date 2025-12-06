import { useState } from 'react';
import { ChevronsUpDown } from 'lucide-react';
import * as LucideIcons from 'lucide-react';
import { Button, cn, Input, Popover, PopoverContent, PopoverTrigger } from '@mirel/ui';

interface IconPickerProps {
  value?: string;
  onChange: (value: string) => void;
  className?: string;
}

const iconList = Object.keys(LucideIcons)
  .filter((key) => key !== 'createLucideIcon' && key !== 'default' && isNaN(Number(key)))
  .map((key) => ({
    value: key
      .replace(/([A-Z])/g, '-$1')
      .toLowerCase()
      .replace(/^-/, ''),
    label: key,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    Icon: (LucideIcons as any)[key] as React.ElementType,
  }));

export function IconPicker({ value, onChange, className }: IconPickerProps) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState('');

  const selectedIcon = iconList.find((icon) => icon.value === value) || iconList.find((icon) => icon.value === 'circle');
  const SelectedIconComponent = selectedIcon?.Icon;

  const filteredIcons = iconList.filter((icon) =>
    icon.value.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          role="combobox"
          aria-expanded={open}
          className={cn('w-full justify-between', className)}
        >
          {value ? (
            <div className="flex items-center gap-2">
              {SelectedIconComponent && <SelectedIconComponent className="h-4 w-4" />}
              <span className="truncate">{value}</span>
            </div>
          ) : (
            'アイコンを選択...'
          )}
          <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-[300px] p-0" align="start">
          <div className="p-2 border-b">
            <Input
                placeholder="アイコンを検索..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="h-8"
            />
          </div>
          <div className="h-[300px] overflow-y-auto p-1 grid grid-cols-4 gap-1">
               {filteredIcons.length === 0 ? (
                   <div className="col-span-4 py-4 text-center text-sm text-muted-foreground">
                       アイコンが見つかりません。
                   </div>
               ) : (
                   filteredIcons.slice(0, 100).map((icon) => (
                       <Button
                           key={icon.value}
                           variant="ghost"
                           className={cn(
                               "h-10 w-10 p-0",
                               value === icon.value && "bg-accent text-accent-foreground"
                           )}
                           onClick={() => {
                               onChange(icon.value);
                               setOpen(false);
                           }}
                           title={icon.value}
                       >
                           <icon.Icon className="h-6 w-6" />
                       </Button>
                   ))
               )}
          </div>
      </PopoverContent>
    </Popover>
  );
}
