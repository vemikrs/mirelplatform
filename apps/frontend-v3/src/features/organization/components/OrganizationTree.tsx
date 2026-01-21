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
import type { Organization } from '../types';
import { cn } from '@/lib/utils/cn';

interface OrganizationTreeProps {
  organizations: Organization[];
  onSelect: (org: Organization) => void;
  selectedOrgId?: string;
  onAddSubOrg?: (parentOrg: Organization) => void;
  onEditOrg?: (org: Organization) => void;
  onDeleteOrg?: (org: Organization) => void;
}

export function OrganizationTree({ 
  organizations, 
  onSelect, 
  selectedOrgId,
  onAddSubOrg,
  onEditOrg,
  onDeleteOrg
}: OrganizationTreeProps) {
  return (
    <div className="space-y-1">
      {organizations.map(org => (
        <TreeNode 
          key={org.id} 
          org={org} 
          onSelect={onSelect} 
          selectedOrgId={selectedOrgId}
          onAddSubOrg={onAddSubOrg}
          onEditOrg={onEditOrg}
          onDeleteOrg={onDeleteOrg}
          level={0}
        />
      ))}
    </div>
  );
}

interface TreeNodeProps {
  org: Organization;
  onSelect: (org: Organization) => void;
  selectedOrgId?: string;
  onAddSubOrg?: (parentOrg: Organization) => void;
  onEditOrg?: (org: Organization) => void;
  onDeleteOrg?: (org: Organization) => void;
  level: number;
}

function TreeNode({ 
  org, 
  onSelect, 
  selectedOrgId,
  onAddSubOrg,
  onEditOrg,
  onDeleteOrg,
  level 
}: TreeNodeProps) {
  const [isExpanded, setIsExpanded] = useState(true);
  const hasChildren = org.children && org.children.length > 0;
  const isSelected = org.id === selectedOrgId;

  const handleToggle = (e: React.MouseEvent) => {
    e.stopPropagation();
    setIsExpanded(!isExpanded);
  };

  const handleSelect = () => {
    onSelect(org);
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
          {org.displayName || org.name}
        </span>

        <div className="opacity-0 group-hover:opacity-100 transition-opacity">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="h-6 w-6">
                <MoreHorizontal className="size-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={() => onAddSubOrg?.(org)}>
                <Plus className="mr-2 size-4" />
                配下組織を追加
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => onEditOrg?.(org)}>
                <Edit className="mr-2 size-4" />
                編集
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => onDeleteOrg?.(org)} className="text-destructive">
                <Trash className="mr-2 size-4" />
                削除
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>

      {isExpanded && hasChildren && (
        <div>
          {org.children!.map(child => (
            <TreeNode 
              key={child.id} 
              org={child} 
              onSelect={onSelect} 
              selectedOrgId={selectedOrgId}
              onAddSubOrg={onAddSubOrg}
              onEditOrg={onEditOrg}
              onDeleteOrg={onDeleteOrg}
              level={level + 1}
            />
          ))}
        </div>
      )}
    </div>
  );
}
