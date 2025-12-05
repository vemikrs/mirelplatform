import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  DragEndEvent,
  DragOverlay,
  DragStartEvent,
} from '@dnd-kit/core';
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  verticalListSortingStrategy,
  useSortable,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { MenuDto } from '@/lib/api/menu';
import { cn } from '@mirel/ui';
import * as LucideIcons from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Plus, GripVertical } from 'lucide-react';
import { useState, useMemo } from 'react';

// --- Sortable Item Component --- //
interface SortableMenuItemProps {
  menu: MenuDto;
  depth?: number;
  isSelected?: boolean;
  onSelect: (menu: MenuDto) => void;
  onAddSubItem: (parentId: string) => void;
  isOverlay?: boolean;
}

function SortableMenuItem({
  menu,
  depth = 0,
  isSelected,
  onSelect,
  onAddSubItem,
  isOverlay,
}: SortableMenuItemProps) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: menu.id, data: { ...menu, depth } });

  const style = {
    transform: CSS.Translate.toString(transform),
    transition,
    marginLeft: `${depth * 24}px`, // Visual indentation
  };

  const IconComponent = menu.icon
    ? (LucideIcons as any)[menu.icon.replace(/-([a-z])/g, (g) => g[1].toUpperCase())]
    : null;

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={cn(
        'group flex items-center gap-2 rounded-md border border-transparent p-2 hover:bg-muted/50',
        isSelected && 'bg-muted border-border',
        isDragging && 'opacity-50',
        isOverlay && 'bg-background border-primary shadow-lg'
      )}
      onClick={() => onSelect(menu)}
    >
      <div
        {...attributes}
        {...listeners}
        className="cursor-move p-1 text-muted-foreground hover:text-foreground"
      >
        <GripVertical className="h-4 w-4" />
      </div>

      <div className="flex items-center gap-2 flex-1 min-w-0">
        {IconComponent && <IconComponent className="h-4 w-4 shrink-0" />}
        <span className="font-medium truncate">{menu.label}</span>
        <span className="text-xs text-muted-foreground truncate">{menu.id}</span>
      </div>

      <div className="opacity-0 group-hover:opacity-100 transition-opacity">
        <Button
            variant="ghost"
            size="icon"
            className="h-6 w-6"
            onClick={(e) => {
                e.stopPropagation();
                onAddSubItem(menu.id);
            }}
            title="子メニューを追加"
        >
            <Plus className="h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}

// --- Main Tree Component --- //
interface MenuTreeProps {
  menus: MenuDto[];
  selectedId?: string;
  onSelect: (menu: MenuDto) => void;
  onReorder: (menus: MenuDto[]) => void;
  onAddSubItem: (parentId?: string) => void;
}

export function MenuTree({
  menus,
  selectedId,
  onSelect,
  onReorder,
  onAddSubItem,
}: MenuTreeProps) {
  const [activeId, setActiveId] = useState<string | null>(null);

  // Flatten the tree for SortableContext, tracking depth
  // Note: This simple implementation flattens the list for visual reordering.
  // Re-treeing happens on drop.


  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  const handleDragStart = (event: DragStartEvent) => {
    setActiveId(event.active.id as string);
  };

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;

    if (over && active.id !== over.id) {
        // Recursive search to find the array containing the item
        const findContainer = (items: MenuDto[]): MenuDto[] | undefined => {
            if (items.find(i => i.id === active.id)) return items;
            for (const item of items) {
                if (item.children) {
                    const found = findContainer(item.children);
                    if (found) return found;
                }
            }
            return undefined;
        };

        const activeContainer = findContainer(menus);
        // We only support reordering within the same container for now (siblings)
        // Check if over is also in the same container
        if (activeContainer && activeContainer.find(i => i.id === over.id)) {
             const oldIndex = activeContainer.findIndex(i => i.id === active.id);
             const newIndex = activeContainer.findIndex(i => i.id === over.id);

             if (oldIndex !== -1 && newIndex !== -1) {
                 // Clone tree to avoid mutation
                 const cloneTree = (items: MenuDto[]): MenuDto[] => {
                     return items.map(item => ({
                         ...item,
                         children: item.children ? cloneTree(item.children) : undefined
                     }));
                 };
                 const newTree = cloneTree(menus);
                 
                 // Find the container in the NEW tree
                 const findNewContainer = (items: MenuDto[]): MenuDto[] | undefined => {
                    if (items.find(i => i.id === active.id)) return items;
                    for (const item of items) {
                        if (item.children) {
                            const found = findNewContainer(item.children);
                            if (found) return found;
                        }
                    }
                    return undefined;
                };

                 const newContainer = findNewContainer(newTree);
                 if (newContainer) {
                     const reordered = arrayMove(newContainer, oldIndex, newIndex);
                     // Update sortOrders
                     reordered.forEach((item, index) => {
                         item.sortOrder = (index + 1) * 10;
                     });
                     
                     // Replace the container contents in the new tree
                     // Wait, newContainer is a reference to the array inside newTree (mapped array)
                     // Because map returns new array but objects are shallow copied? 
                     // No, I did recursive deep clone of children arrays above.
                     // But `const newContainer` is the array itself.
                     // If I modify `newContainer`'s contents (mutation), it updates `newTree`?
                     // No, `arrayMove` returns a NEW array.
                     // So I need to splice it back in? Or just mutate existing array?
                     
                     // Easier: mutate the newContainer array?
                     // arrayMove returns new array.
                     // I need to set the property on the parent.
                     
                     // Let's retry recursion map to update specifically.
                     
                     const updateRecursive = (items: MenuDto[]): MenuDto[] => {
                         if (items.find(i => i.id === active.id)) {
                             // This is the container
                             const updatedItems = arrayMove(items, oldIndex, newIndex);
                             updatedItems.forEach((item, index) => {
                                 item.sortOrder = (index + 1) * 10;
                             });
                             return updatedItems;
                         }
                         return items.map(item => ({
                             ...item,
                             children: item.children ? updateRecursive(item.children) : undefined
                         }));
                     };

                     const finalTree = updateRecursive(menus); // Use original menus as base for map
                     onReorder(finalTree);
                 }
             }
        }
    }
    setActiveId(null);
  };
  
  // WRAPPER for Recursive implementation
  // Actually, let's just do a flat list for now to act as a "Sitemap view" 
  // and if user wants to reparent, they edit.
  // BUT the prompt asked for Tree View.
  
  // Let's try Recursive RENDER, but maybe DnD is limited to siblings if I can't easily do cross-level.
  // Use `dnd-kit` generic `SortableContext` on the root list of IDs.
  
  // Let's go with visual flat list but disable reordering for now if it's too risky, 
  // OR just render the tree and allow selection.
  // The plan said "Drag & Drop".
  // I will implement specific sortable contexts for each children array.
  
  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCenter}
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
    >
        <div className="space-y-1">
            <RecursiveSortableList 
                items={menus} 
                onSelect={onSelect} 
                onAddSubItem={onAddSubItem}
                selectedId={selectedId}
            />
        </div>
        <DragOverlay>
            {activeId ? (
                <div className="p-2 border bg-background rounded shadow">
                    Moving...
                </div>
            ) : null}
        </DragOverlay>
    </DndContext>
  );
}

function RecursiveSortableList({ 
    items, 
    onSelect,
    onAddSubItem, 
    selectedId,
    depth = 0 
}: { 
    items: MenuDto[], 
    onSelect: any, 
    onAddSubItem: any,
    selectedId?: string,
    depth?: number 
}) {
    // SortableContext needs a list of IDs.
    // If we want to sort strictly siblings, this works.
    const ids = useMemo(() => items.map(i => i.id), [items]);

    return (
        <SortableContext items={ids} strategy={verticalListSortingStrategy}>
            {items.map(item => (
                <div key={item.id} className="space-y-1">
                    <SortableMenuItem 
                        menu={item} 
                        depth={depth} 
                        onSelect={onSelect}
                        isSelected={selectedId === item.id}
                        onAddSubItem={onAddSubItem}
                    />
                    {item.children && item.children.length > 0 && (
                        <RecursiveSortableList 
                            items={item.children} 
                            onSelect={onSelect}
                            onAddSubItem={onAddSubItem}
                            selectedId={selectedId}
                            depth={depth + 1}
                        />
                    )}
                </div>
            ))}
        </SortableContext>
    )
}
