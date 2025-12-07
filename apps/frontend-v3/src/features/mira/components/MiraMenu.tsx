import {
  Button,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
  cn,
} from '@mirel/ui';
import {
  Download,
  Keyboard,
  Plus,
  Settings,
  Settings2,
  Trash2,
} from 'lucide-react';

interface MiraMenuProps {
  onNewConversation: () => void;
  onOpenContextEditor: () => void;
  onOpenShortcuts: () => void;
  onExport: () => void;
  onClearConversation: () => void;
  isExporting: boolean;
  hasMessages: boolean;
  className?: string;
}

export function MiraMenu({
  onNewConversation,
  onOpenContextEditor,
  onOpenShortcuts,
  onExport,
  onClearConversation,
  isExporting,
  hasMessages,
  className,
}: MiraMenuProps) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          className={cn("h-8 w-8", className)}
          title="Miraメニュー"
        >
          <Settings className="w-4 h-4" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent className="w-56" align="end" forceMount>
        <DropdownMenuGroup>
          <DropdownMenuLabel>一般</DropdownMenuLabel>
          <DropdownMenuItem onClick={onNewConversation}>
            <Plus className="mr-2 h-4 w-4" />
            <span>新しい会話</span>
            <span className="ml-auto text-xs tracking-widest text-muted-foreground">⌘N</span>
          </DropdownMenuItem>
          <DropdownMenuItem onClick={onOpenContextEditor}>
            <Settings2 className="mr-2 h-4 w-4" />
            <span>コンテキスト設定</span>
          </DropdownMenuItem>
          <DropdownMenuItem onClick={onOpenShortcuts}>
            <Keyboard className="mr-2 h-4 w-4" />
            <span>ショートカット</span>
            <span className="ml-auto text-xs tracking-widest text-muted-foreground">?</span>
          </DropdownMenuItem>
        </DropdownMenuGroup>
        
        <DropdownMenuSeparator />
        
        <DropdownMenuGroup>
          <DropdownMenuLabel>データ</DropdownMenuLabel>
          <DropdownMenuItem onClick={onExport} disabled={isExporting}>
            <Download className="mr-2 h-4 w-4" />
            <span>エクスポート</span>
          </DropdownMenuItem>
        </DropdownMenuGroup>
        
        {hasMessages && (
          <>
            <DropdownMenuSeparator />
            <DropdownMenuItem 
              onClick={onClearConversation}
              className="text-destructive focus:text-destructive focus:bg-destructive/10"
            >
              <Trash2 className="mr-2 h-4 w-4" />
              <span>会話をクリア</span>
            </DropdownMenuItem>
          </>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
