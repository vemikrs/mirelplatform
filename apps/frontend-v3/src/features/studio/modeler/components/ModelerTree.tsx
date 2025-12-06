import React, { useEffect, useState } from 'react';
import { NavLink, useParams } from 'react-router-dom';
import { ChevronRight, ChevronDown, Database, Code, Search, Plus, FileText, Table } from 'lucide-react';
import { Input, Button, cn } from '@mirel/ui';
import { modelerApi } from '../api/modelerApi';

interface TreeNode {
  id: string;
  label: string;
  type: 'entity' | 'view' | 'code' | 'folder';
  children?: TreeNode[];
  path?: string;
}

export const ModelerTree: React.FC<{ className?: string }> = ({ className }) => {
  const { entityId, codeId } = useParams<{ entityId?: string; codeId?: string }>();
  // Expand All by default for now
  const [expanded, setExpanded] = useState<Record<string, boolean>>({
    'entities': true,
    'codes': true,
    'views': true
  });
  const [nodes, setNodes] = useState<TreeNode[]>([]);
  const [searchTerm, setSearchTerm] = useState('');

  const toggle = (id: string) => {
    setExpanded(prev => ({ ...prev, [id]: !prev[id] }));
  };

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [modelsRes, codesRes] = await Promise.all([
          modelerApi.listModels(),
          modelerApi.listCodeGroups()
        ]);
        
        // Transform API response to TreeNodes
        // modelerApi.listModels returns { data: { models: { value: string, text: string }[] } }
        
        const entities: TreeNode[] = (modelsRes.data?.models || []).map((m: any) => ({
          id: m.value,
          label: m.text,
          type: 'entity',
          path: `/apps/studio/modeler/entities/${m.value}`
        }));

        const codes: TreeNode[] = (codesRes.data?.groups || []).map((c: any) => ({
          id: c.groupId,
          label: c.groupName || c.groupId,
          type: 'code',
          path: `/apps/studio/modeler/codes?id=${c.groupId}` // Codes page might need ID param or route
        }));

        setNodes([
          {
            id: 'entities',
            label: 'Entities',
            type: 'folder',
            children: entities,
            path: '/apps/studio/modeler/entities'
          },
        //   {
        //     id: 'views',
        //     label: 'Views',
        //     type: 'folder',
        //     children: [], // Views unimplemented
        //     path: '/apps/studio/modeler/relations' // Placeholder
        //   },
          {
            id: 'codes',
            label: 'Code Groups',
            type: 'folder',
            children: codes,
            path: '/apps/studio/modeler/codes'
          }
        ]);
      } catch (err) {
        console.error("Failed to fetch modeler tree data", err);
      }
    };
    fetchData();
  }, []);

  const renderNode = (node: TreeNode, level: number = 0) => {
    const isExpanded = expanded[node.id];
    const hasChildren = node.children && node.children.length > 0;
    const isActive = (node.type === 'entity' && node.id === entityId) || 
                     (node.type === 'code' && node.id === codeId);

    // Search filter
    if (searchTerm && node.type !== 'folder' && !node.label.toLowerCase().includes(searchTerm.toLowerCase())) {
        return null;
    }

    // Determine Icon
    let Icon = FileText;
    if (node.type === 'folder') {
        if (node.id === 'entities') Icon = Table;
        if (node.id === 'codes') Icon = Code;
        if (node.id === 'views') Icon = Database;
    }

    return (
      <div key={node.id}>
        <div 
          className={cn(
            "flex items-center py-1 px-2 hover:bg-accent/50 cursor-pointer text-sm select-none",
            isActive && "bg-accent text-accent-foreground font-medium",
            level > 0 && "ml-4"
          )}
        >
          {hasChildren ? (
            <div onClick={(e) => { e.stopPropagation(); toggle(node.id); }} className="p-1 mr-1 hover:bg-muted rounded">
                {isExpanded ? <ChevronDown className="size-3" /> : <ChevronRight className="size-3" />}
            </div>
          ) : <div className="w-5" />}
          
          <NavLink 
            to={node.path || '#'} 
            className="flex items-center gap-2 flex-1 truncate"
            onClick={(e) => { if (!node.path) e.preventDefault(); }}
          >
            <Icon className="size-4 opacity-70" />
            <span>{node.label}</span>
          </NavLink>
        </div>
        {hasChildren && isExpanded && (
          <div>
            {node.children!.map(child => renderNode(child, level + 1))}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className={cn("flex flex-col h-full bg-card border-r border-border", className)}>
      <div className="p-2 border-b border-border space-y-2">
        <div className="flex items-center gap-2">
          <div className="relative flex-1">
            <Search className="absolute left-2 top-2.5 size-3 text-muted-foreground" />
            <Input 
              placeholder="Search..." 
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="h-8 text-xs pl-8"
            />
          </div>
          <Button size="icon" variant="ghost" className="size-8">
            <Plus className="size-4" />
          </Button>
        </div>
      </div>
      <div className="flex-1 overflow-y-auto p-2">
        {nodes.map(node => renderNode(node))}
      </div>
    </div>
  );
};
