import React from 'react';
import { Card } from '@mirel/ui';
import { Play, Square, ArrowRightCircle, Mail, Database } from 'lucide-react';

interface PaletteItemProps {
  type: string;
  label: string;
  icon: React.ReactNode;
}

const PaletteItem: React.FC<PaletteItemProps> = ({ type, label, icon }) => {
  const onDragStart = (event: React.DragEvent, nodeType: string) => {
    event.dataTransfer.setData('application/reactflow', nodeType);
    event.dataTransfer.effectAllowed = 'move';
  };

  return (
    <div
      className="mb-2 cursor-move"
      onDragStart={(event) => onDragStart(event, type)}
      draggable
    >
      <Card className="p-3 text-sm font-medium hover:bg-muted transition-colors flex items-center gap-2">
        {icon}
        {label}
      </Card>
    </div>
  );
};

export const NodePalette: React.FC = () => {
  return (
    <div className="w-64 border-r bg-background flex flex-col h-full">
      <div className="p-4 border-b">
        <h3 className="font-semibold text-foreground">Flow Nodes</h3>
        <p className="text-xs text-muted-foreground mt-1">Drag to add to flow</p>
      </div>
      <div className="p-4 flex-1 overflow-y-auto">
        <PaletteItem type="input" label="Start" icon={<Play className="size-4" />} />
        <PaletteItem type="default" label="Process" icon={<Square className="size-4" />} />
        <PaletteItem type="output" label="End" icon={<ArrowRightCircle className="size-4" />} />
        <div className="my-2 border-t" />
        <PaletteItem type="email" label="Send Email" icon={<Mail className="size-4" />} />
        <PaletteItem type="db" label="Update Record" icon={<Database className="size-4" />} />
      </div>
    </div>
  );
};
