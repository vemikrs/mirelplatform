import { useState } from 'react';
import { Check, ChevronsUpDown, Search } from 'lucide-react';
import * as LucideIcons from 'lucide-react';
import { cn } from '@mirel/ui';
import { Button } from '@/components/ui/button';
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from '@/components/ui/command';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { ScrollArea } from '@/components/ui/scroll-area';

interface IconPickerProps {
  value?: string;
  onChange: (value: string) => void;
  className?: string; // Add className prop
}

const iconList = Object.keys(LucideIcons)
  .filter((key) => key !== 'createLucideIcon' && key !== 'default' && isNaN(Number(key))) // Filter out non-icon exports
  .map((key) => ({
    value: key
      .replace(/([A-Z])/g, '-$1')
      .toLowerCase()
      .replace(/^-/, ''), // Convert PascalCase to kebab-case (e.g. LayoutDashboard -> layout-dashboard)
    label: key,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    Icon: (LucideIcons as any)[key] as React.ElementType,
  }));

// Limit for performance initially, but search will filter
const INITIAL_DISPLAY_LIMIT = 50;

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
        <Command shouldFilter={false}>
            <CommandInput 
                placeholder="アイコンを検索..." 
                value={search}
                onValueChange={setSearch}
            />
            <CommandList>
                <CommandEmpty>アイコンが見つかりません。</CommandEmpty>
                <ScrollArea className="h-[300px]">
                    <div className="p-1 grid grid-cols-4 gap-1">
                        {filteredIcons.slice(0, 100).map((icon) => (
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
                        ))}
                    </div>
                </ScrollArea>
            </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
}
