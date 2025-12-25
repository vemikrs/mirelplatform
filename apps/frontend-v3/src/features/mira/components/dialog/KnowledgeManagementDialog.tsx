import { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
  Label,
} from '@mirel/ui';
import { Database, User, Users } from 'lucide-react';
import { KnowledgeDocumentList } from '../KnowledgeDocumentList';
import { KnowledgeDocumentUploadForm } from '../KnowledgeDocumentUploadForm';
import { cn } from '@/lib/utils/cn';

interface KnowledgeManagementDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function KnowledgeManagementDialog({
  open,
  onOpenChange,
}: KnowledgeManagementDialogProps) {
  const [activeTab, setActiveTab] = useState('upload'); // For Mobile Only
  const [scope, setScope] = useState<'USER' | 'TENANT'>('USER');
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const handleUploadSuccess = () => {
    setRefreshTrigger(prev => prev + 1);
    // On mobile, switch to list view automatically
    setActiveTab('list');
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px] md:max-w-4xl lg:max-w-5xl h-[90vh] md:h-auto flex flex-col">
        <DialogHeader className="flex-shrink-0">
          <DialogTitle className="flex items-center gap-2">
            <Database className="w-5 h-5" />
            ナレッジベース管理
          </DialogTitle>
          <DialogDescription>
            AIが参照するためのドキュメントを登録・管理します。
          </DialogDescription>
        </DialogHeader>

        {/* Global Scope Selector (Always Visible) */}
        <div className="flex-shrink-0 py-2">
            <Label className="mb-2 block text-xs font-medium text-muted-foreground uppercase">ワークスペース選択</Label>
            <div className="grid grid-cols-2 gap-2 bg-muted p-1 rounded-lg">
                <button
                    onClick={() => setScope('USER')}
                    className={cn(
                        "flex items-center justify-center gap-2 py-2 rounded-md text-sm font-medium transition-all",
                        scope === 'USER' 
                            ? "bg-primary text-primary-foreground shadow-sm" 
                            : "text-muted-foreground hover:bg-background/50 hover:text-foreground"
                    )}
                >
                    <User className="w-4 h-4" />
                    自分のみ (Private)
                </button>
                <button
                    onClick={() => setScope('TENANT')}
                    className={cn(
                        "flex items-center justify-center gap-2 py-2 rounded-md text-sm font-medium transition-all",
                        scope === 'TENANT' 
                            ? "bg-primary text-primary-foreground shadow-sm" 
                            : "text-muted-foreground hover:bg-background/50 hover:text-foreground"
                    )}
                >
                    <Users className="w-4 h-4" />
                    テナント共有 (Team)
                </button>
            </div>
        </div>

        {/* Desktop Layout: Side by Side */}
        <div className="hidden md:grid md:grid-cols-12 md:gap-6 flex-1 min-h-0">
            <div className="col-span-4">
                <KnowledgeDocumentUploadForm scope={scope} onUploadSuccess={handleUploadSuccess} />
            </div>
            <div className="col-span-8 flex flex-col min-h-0 bg-muted/20 rounded-lg border p-4">
                 <h3 className="font-semibold mb-4 flex items-center gap-2">
                    <Database className="w-4 h-4 text-muted-foreground" />
                    登録済みドキュメント
                 </h3>
                 <div className="flex-1 overflow-y-auto pr-2">
                    <KnowledgeDocumentList scope={scope} refreshTrigger={refreshTrigger} />
                 </div>
            </div>
        </div>

        {/* Mobile Layout: Tabs */}
        <div className="md:hidden flex-1 min-h-0 flex flex-col">
            <Tabs defaultValue="upload" value={activeTab} onValueChange={setActiveTab} className="flex-1 flex flex-col min-h-0">
                <TabsList className="grid w-full grid-cols-2 flex-shrink-0">
                    <TabsTrigger value="upload">新規登録</TabsTrigger>
                    <TabsTrigger value="list">登録済み一覧</TabsTrigger>
                </TabsList>
                
                <TabsContent value="upload" className="flex-1 overflow-y-auto py-4">
                    <KnowledgeDocumentUploadForm scope={scope} onUploadSuccess={handleUploadSuccess} />
                </TabsContent>
                
                <TabsContent value="list" className="flex-1 overflow-y-auto py-4">
                    <KnowledgeDocumentList scope={scope} refreshTrigger={refreshTrigger} />
                </TabsContent>
            </Tabs>
        </div>

      </DialogContent>
    </Dialog>
  );
}
