import React, { useState } from 'react';
import { 
  ChevronRight, 
  ChevronDown, 
  Folder, 
  File, 
  MoreHorizontal, 
  Plus, 
  Edit, 
  Trash 
} from 'lucide-react';
import { Button, DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@mirel/ui';
import type { OrganizationUnit } from '../types';
import { cn } from '@/lib/utils/cn';

interface OrganizationTreeProps {
  units: OrganizationUnit[];
  onSelect: (unit: OrganizationUnit) => void;
  selectedUnitId?: string;
  onAddSubUnit?: (parentUnit: OrganizationUnit) => void;
  onEditUnit?: (unit: OrganizationUnit) => void;
  onDeleteUnit?: (unit: OrganizationUnit) => void;
}

export function OrganizationTree({ 
  units, 
  onSelect, 
  selectedUnitId,
  onAddSubUnit,
  onEditUnit,
  onDeleteUnit
}: OrganizationTreeProps) {
  return (
    <div className="space-y-1">
      {units.map(unit => (
        <TreeNode 
          key={unit.unitId} 
          unit={unit} 
          onSelect={onSelect} 
          selectedUnitId={selectedUnitId}
          onAddSubUnit={onAddSubUnit}
          onEditUnit={onEditUnit}
          onDeleteUnit={onDeleteUnit}
          level={0}
        />
      ))}
    </div>
  );
}

interface TreeNodeProps {
  unit: OrganizationUnit;
  onSelect: (unit: OrganizationUnit) => void;
  selectedUnitId?: string;
  onAddSubUnit?: (parentUnit: OrganizationUnit) => void;
  onEditUnit?: (unit: OrganizationUnit) => void;
  onDeleteUnit?: (unit: OrganizationUnit) => void;
  level: number;
}

function TreeNode({ 
  unit, 
  onSelect, 
  selectedUnitId,
  onAddSubUnit,
  onEditUnit,
  onDeleteUnit,
  level 
}: TreeNodeProps) {
  const [isExpanded, setIsExpanded] = useState(true);
  const hasChildren = unit.children && unit.children.length > 0;
  const isSelected = unit.unitId === selectedUnitId;

  const handleToggle = (e: React.MouseEvent) => {
    e.stopPropagation();
    setIsExpanded(!isExpanded);
  };

  const handleSelect = () => {
    onSelect(unit);
  };

  return (
    <div>
      <div 
        className={cn(
          "flex items-center group py-1 px-2 rounded-md cursor-pointer hover:bg-accent/50 transition-colors",
          isSelected && "bg-accent text-accent-foreground"
        )}
        style={{ paddingLeft: `${level * 16 + 8}px` }}
        onClick={handleSelect}
      >
        <div 
          className={cn(
            "p-1 rounded-sm hover:bg-muted/50 mr-1",
            !hasChildren && "invisible"
          )}
          onClick={handleToggle}
        >
          {isExpanded ? <ChevronDown className="size-4" /> : <ChevronRight className="size-4" />}
        </div>
        
        <div className="mr-2 text-muted-foreground">
          {hasChildren ? <Folder className="size-4" /> : <File className="size-4" />}
        </div>
        
        <span className="flex-1 truncate text-sm font-medium">
          {unit.name}
        </span>

        <div className="opacity-0 group-hover:opacity-100 transition-opacity">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="h-6 w-6">
                <MoreHorizontal className="size-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={() => onAddSubUnit?.(unit)}>
                <Plus className="mr-2 size-4" />
                配下組織を追加
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => onEditUnit?.(unit)}>
                <Edit className="mr-2 size-4" />
                編集
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => onDeleteUnit?.(unit)} className="text-destructive">
                <Trash className="mr-2 size-4" />
                削除
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>

      {isExpanded && hasChildren && (
        <div>
          {unit.children!.map(child => (
            <TreeNode 
              key={child.unitId} 
              unit={child} 
              onSelect={onSelect} 
              selectedUnitId={selectedUnitId}
              onAddSubUnit={onAddSubUnit}
              onEditUnit={onEditUnit}
              onDeleteUnit={onDeleteUnit}
              level={level + 1}
            />
          ))}
        </div>
      )}
    </div>
  );
}
