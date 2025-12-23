
import { useState, useEffect } from 'react';
import {
  Card,
  CardContent,
  CardHeader,
  Button,
  Input,
  Label,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Badge,
  Textarea,
  Separator,
  useToast,
  Slider,
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow
} from '@mirel/ui';
import { 
  Search, 
  Loader2,
  Database,
  Activity,
  Target,
  Users,
  Building2,
  Globe,
  Filter,
  AlertCircle,
  ExternalLink,
  Ban,
  CheckCircle2,
  FileJson,
  Info,
  Cpu,
  Save,
  Trash2,
  FileText,
  SearchCode
} from 'lucide-react';
import { apiClient } from '@/lib/api/client';

interface DebuggerStats {
  totalDocumentCount: number;
  countByScope: Record<string, number>;
  isIndexEmpty: boolean;
  activeEmbeddingModel?: string;
}

interface DebuggerAnalytics {
  totalIndexCount: number;
  stepCounts: {
    vectorSearchRaw: number;
    keywordSearchRaw: number;
    afterScopeFilter: number;
    afterThresholdFilter: number;
  };
  results: any[];
  rejectedDocuments: {
    id: string;
    fileName: string;
    reason: string;
    score: number;
    vectorRank?: number;
    vectorSimilarity?: number;
    keywordRank?: number;
    termFrequency?: number;
    metadata: any;
  }[];
}

export function PlaygroundIndexDebuggerPanel() {
  const { toast } = useToast();
  const [loading, setLoading] = useState(false);
  const [stats, setStats] = useState<DebuggerStats | null>(null);
  const [analytics, setAnalytics] = useState<DebuggerAnalytics | null>(null);
  const [selectedMetadata, setSelectedMetadata] = useState<any | null>(null);

  const [params, setParams] = useState({
    query: '',
    scope: 'USER',
    targetTenantId: '',
    targetUserId: '',
    topK: 5,
    threshold: 0.0
  });

  /* Document Manager State */
  const [activeTab, setActiveTab] = useState('debug');
  const [documents, setDocuments] = useState<any[]>([]);

  useEffect(() => {
    fetchStats();
    if (activeTab === 'documents') {
        fetchDocuments();
    }
  }, [activeTab, params.scope]);

  const fetchDocuments = async () => {
    setLoading(true);
    try {
        const res = await apiClient.post('/api/mira/debugger/documents/list', {
            scope: params.scope,
            tenantId: params.targetTenantId,
            userId: params.targetUserId
        });
        setDocuments(res.data);
    } catch (error) {
        console.error('Failed to fetch documents', error);
        toast({
            title: 'Fetch Failed',
            description: 'Could not load documents for this scope.',
            variant: 'destructive'
        });
    } finally {
        setLoading(false);
    }
  };

  const handleDeleteDocument = async (fileId: string) => {
    setConfirmDialog({
        isOpen: true,
        title: 'Delete Document',
        description: 'Are you sure you want to delete this document from the index? This action cannot be undone.',
        onConfirm: async () => {
            setLoading(true);
            try {
                await apiClient.post('/api/mira/debugger/documents/delete', { fileId });
                toast({
                    title: 'Document Deleted',
                    description: 'The document has been removed from the index.'
                });
                fetchDocuments();
                fetchStats();
            } catch (error: any) {
                toast({
                    title: 'Delete Failed',
                    description: error.response?.data?.message || 'Failed to delete document',
                    variant: 'destructive'
                });
            } finally {
                setLoading(false);
            }
        }
    });
  };

  const fetchStats = async () => {
    try {
      const res = await apiClient.get('/api/mira/debugger/stats');
      setStats(res.data);
    } catch (error) {
      console.error('Failed to fetch stats', error);
      // Silent fail or toast? Just log for stats.
    }
  };
  


  /* Confirm replaced with Dialog */
  const [confirmDialog, setConfirmDialog] = useState<{
      isOpen: boolean;
      title: string;
      description: string;
      onConfirm: () => void;
  }>({
      isOpen: false,
      title: '',
      description: '',
      onConfirm: () => {},
  });

  const handleReindex = async () => {
    setConfirmDialog({
        isOpen: true,
        title: 'Re-index Scope',
        description: `Are you sure you want to re-index all documents in ${params.scope} scope? This might take a while.`,
        onConfirm: async () => {
            setLoading(true);
            try {
              const res = await apiClient.post('/api/mira/debugger/reindex', params);
              toast({
                title: 'Re-index Started',
                description: res.data.message || 'Processing in background...',
              });
              // Refresh stats after a short delay
              setTimeout(fetchStats, 2000);
            } catch (error: any) {
              console.error(error);
              toast({
                title: 'Re-index Failed',
                description: error.response?.data?.message || 'Failed to trigger re-index',
                variant: 'destructive'
              });
            } finally {
              setLoading(false);
            }
        }
    });
  };

  const handleAnalyze = async () => {
    if (!params.query.trim()) {
      toast({
        title: 'Validation Error',
        description: 'Please enter a search query',
        variant: 'destructive'
      });
      return;
    }

    setLoading(true);
    setAnalytics(null);

    try {
      const res = await apiClient.post('/api/mira/debugger/analyze', params);
      setAnalytics(res.data);
      
      const resCount = res.data.results?.length || 0;
      toast({
        title: 'Analysis Complete',
        description: `Found ${resCount} accepted, ${res.data.rejectedDocuments?.length || 0} rejected.`
      });

    } catch (error: any) {
      console.error(error);
      toast({
        title: 'Analysis Failed',
        description: error.response?.data?.message || 'Failed to execute analysis',
        variant: 'destructive'
      });
    } finally {
      setLoading(false);
    }
  };

  const handleSaveSettings = async () => {
      setConfirmDialog({
          isOpen: true,
          title: 'Save Configuration',
          description: `Save current Top K (${params.topK}) and Threshold (${params.threshold}) as default for ${params.scope} scope?`,
          onConfirm: async () => {
              try {
                  // Use params which contains scope, tenantId, userId, topK, threshold
                  const res = await apiClient.post('/api/mira/debugger/settings', params);
                  toast({
                      title: 'Settings Saved',
                      description: res.data.message || 'Configuration updated successfully.',
                  });
              } catch (error: any) {
                  console.error(error);
                  toast({
                      title: 'Save Failed',
                      description: error.response?.data?.message || 'Failed to save settings',
                      variant: 'destructive'
                  });
              }
          }
      });
  };

  const openDocument = (fileId: string) => {
      if (!fileId) return;
      // Open in new tab or navigate
      window.open(`/admin/mira/knowledge/view/${fileId}`, '_blank');
  };

  return (
    <TooltipProvider delayDuration={0}>
    <Tabs value={activeTab} onValueChange={setActiveTab} className="flex h-full w-full bg-background flex-col">
      <div className="border-b px-4 bg-muted/20 flex-none h-10 flex items-center">
        <TabsList className="bg-transparent h-9 p-0 gap-4">
             <TabsTrigger value="debug" className="data-[state=active]:bg-background data-[state=active]:shadow-sm h-8 px-3 text-xs">
                 <SearchCode className="w-3.5 h-3.5 mr-2" />
                 Query Debugger
             </TabsTrigger>
             <TabsTrigger value="documents" className="data-[state=active]:bg-background data-[state=active]:shadow-sm h-8 px-3 text-xs">
                 <FileText className="w-3.5 h-3.5 mr-2" />
                 Document Manager
             </TabsTrigger>
        </TabsList>
      </div>

      <TabsContent value="debug" className="flex-1 flex overflow-hidden mt-0">
         {/* Left Pane: Config & Stats */}
         <div className="w-80 border-r bg-muted/10 flex flex-col h-full">
        {/* Index Stats */}
        <div className="p-4 border-b bg-card/50">
           <h3 className="font-semibold flex items-center gap-2 text-sm mb-3">
             <Activity className="w-4 h-4 text-primary" />
             Index Health
           </h3>
           {stats ? (
               <div className="space-y-3">
                   <div className="flex justify-between items-center text-sm">
                       <span className="text-muted-foreground flex items-center gap-1">
                           <Database className="w-3 h-3" /> Total Docs
                       </span>
                       <span className={`font-mono font-bold ${stats.isIndexEmpty ? 'text-destructive' : 'text-foreground'}`}>
                           {stats.totalDocumentCount.toLocaleString()}
                       </span>
                   </div>
                   {stats.isIndexEmpty && (
                       <div className="text-xs text-destructive flex items-center gap-1 bg-destructive/10 p-2 rounded">
                           <AlertCircle className="w-3 h-3" /> Index is empty!
                       </div>
                   )}
                   {(stats as any).activeEmbeddingModel && (
                       <div className={`text-xs flex items-center justify-between p-2 rounded ${(stats as any).activeEmbeddingModel === 'mock' ? 'bg-amber-100 text-amber-800' : 'bg-muted'}`}>
                          <span className="flex items-center gap-1"><Cpu className="w-3 h-3" /> Model</span>
                          <Badge variant={(stats as any).activeEmbeddingModel === 'mock' ? 'outline' : 'secondary'} className="h-5 text-[10px]">
                              {(stats as any).activeEmbeddingModel}
                          </Badge>
                       </div>
                   )}
                   <div className="grid grid-cols-3 gap-1 text-[10px] text-muted-foreground">
                        <div className="bg-muted p-1 rounded text-center">
                            <div>SYS</div>
                            <div className="font-bold text-foreground">{stats.countByScope['SYSTEM'] || 0}</div>
                        </div>
                        <div className="bg-muted p-1 rounded text-center">
                            <div>TNT</div>
                            <div className="font-bold text-foreground">{stats.countByScope['TENANT'] || 0}</div>
                        </div>
                        <div className="bg-muted p-1 rounded text-center">
                            <div>USR</div>
                            <div className="font-bold text-foreground">{stats.countByScope['USER'] || 0}</div>
                        </div>
                   </div>
               </div>
           ) : (
               <div className="flex justify-center py-2"><Loader2 className="w-4 h-4 animate-spin text-muted-foreground" /></div>
           )}
        <div className="flex gap-2 w-full mt-2">
           <Button variant="ghost" size="sm" className="flex-1 h-6 text-xs" onClick={fetchStats}>Refresh Stats</Button>
           <Button variant="outline" size="sm" className="flex-1 h-6 text-xs text-muted-foreground hover:text-destructive" onClick={handleReindex} disabled={loading}>
               Re-index Scope
           </Button>
        </div>
        </div>

        <div className="p-4 flex flex-col gap-6 overflow-y-auto flex-1">
          <div className="space-y-4">
            <h3 className="font-semibold flex items-center gap-2 text-sm">
              <Search className="w-4 h-4" />
              Debug Parameters
            </h3>

            <div className="space-y-2">
              <Label>Search Query</Label>
              <Textarea 
                placeholder="Enter search phrase..." 
                value={params.query}
                onChange={(e) => setParams(p => ({ ...p, query: e.target.value }))}
                rows={3}
                className="resize-none"
              />
            </div>

            <div className="space-y-2">
              <Label className="flex items-center gap-1 cursor-help">
                Scope
                <TooltipProvider>
                  <Tooltip>
                    <TooltipTrigger><Info className="w-3 h-3 text-muted-foreground hover:text-foreground" /></TooltipTrigger>
                    <TooltipContent className="max-w-[250px]">
                      <p className="text-xs">Access Control Scope. SYSTEM (Global), TENANT (Org-wide), USER (Personal).</p>
                    </TooltipContent>
                  </Tooltip>
                </TooltipProvider>
              </Label>
              <Select 
                value={params.scope} 
                onValueChange={(v) => setParams(p => ({ ...p, scope: v }))}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select scope" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="SYSTEM"><div className="flex items-center gap-2"><Globe className="w-3 h-3"/> SYSTEM</div></SelectItem>
                  <SelectItem value="TENANT"><div className="flex items-center gap-2"><Building2 className="w-3 h-3"/> TENANT</div></SelectItem>
                  <SelectItem value="USER"><div className="flex items-center gap-2"><Users className="w-3 h-3"/> USER</div></SelectItem>
                </SelectContent>
              </Select>
            </div>
            
            <Separator />
            
            <div className="space-y-2">
               <Label className="flex items-center gap-1"><Target className="w-3 h-3"/> Impersonation Target</Label>
               <Input 
                 placeholder="Target Tenant ID" 
                 className="text-xs font-mono h-8"
                 value={params.targetTenantId}
                 onChange={(e) => setParams(p => ({ ...p, targetTenantId: e.target.value }))}
               />
               <Input 
                 placeholder="Target User ID" 
                 className="text-xs font-mono h-8"
                 value={params.targetUserId}
                 onChange={(e) => setParams(p => ({ ...p, targetUserId: e.target.value }))}
               />
            </div>

            <Separator />

             <div className="space-y-4">
              <div className="space-y-2">
                 <div className="flex justify-between">
                   <Label className="flex items-center gap-1 cursor-help">
                     Top K
                     <TooltipProvider>
                      <Tooltip>
                        <TooltipTrigger><Info className="w-3 h-3 text-muted-foreground hover:text-foreground" /></TooltipTrigger>
                        <TooltipContent className="max-w-[250px]">
                          <p className="text-xs">Results to fetch from Vector Store (Initial Candidates). Higher = Better Recall but slower.</p>
                        </TooltipContent>
                      </Tooltip>
                     </TooltipProvider>
                   </Label>
                   <span className="text-xs text-muted-foreground">{params.topK}</span>
                 </div>
                 <Slider
                   min={1}
                   max={20}
                   step={1}
                   value={[params.topK]}
                   onValueChange={([v]) => setParams(p => ({ ...p, topK: v ?? 5 }))}
                 />
              </div>

              <div className="space-y-2">
                 <div className="flex justify-between">
                   <Label className="flex items-center gap-1 cursor-help">
                     Threshold
                     <TooltipProvider>
                      <Tooltip>
                        <TooltipTrigger><Info className="w-3 h-3 text-muted-foreground hover:text-foreground" /></TooltipTrigger>
                        <TooltipContent className="max-w-[250px]">
                          <p className="text-xs">RRF Score Cutoff. Results below this score (0.0 - 1.0) are discarded.</p>
                        </TooltipContent>
                      </Tooltip>
                     </TooltipProvider>
                   </Label>
                   <span className="text-xs text-muted-foreground">{params.threshold.toFixed(2)}</span>
                 </div>
                 <Slider
                   min={0}
                   max={1}
                   step={0.01}
                   value={[params.threshold]}
                   onValueChange={([v]) => setParams(p => ({ ...p, threshold: v ?? 0.0 }))}
                 />
              </div>
            </div>

            <div className="flex flex-col gap-2 mt-4">
                <Button onClick={handleAnalyze} disabled={loading} className="w-full">
                  {loading ? <Loader2 className="w-4 h-4 animate-spin mr-2" /> : <Activity className="w-4 h-4 mr-2" />}
                  Analyze Trace
                </Button>
                <Button onClick={handleSaveSettings} disabled={loading} variant="outline" className="w-full">
                  <Save className="w-4 h-4 mr-2" />
                  Save Configuration
                </Button>
            </div>

          </div>
        </div>
      </div>

      {/* Right Pane: Analysis Results */}
      <div className="flex-1 p-6 overflow-y-auto bg-muted/5">
        
        {!analytics && !loading && (
            <div className="flex flex-col items-center justify-center h-full text-muted-foreground">
                <Search className="w-12 h-12 mb-4 opacity-20" />
                <p>Enter a query and run analysis to see the search funnel breakdown.</p>
            </div>
        )}

        {analytics && (
            <div className="space-y-8 max-w-4xl mx-auto">
                {/* 1. Funnel View */}
                <div>
                    <h3 className="font-bold text-lg mb-4 flex items-center gap-2">
                        <Filter className="w-5 h-5 text-primary" />
                        Search Funnel
                    </h3>
                    <div className="grid grid-cols-4 gap-4">
                        <Card className="bg-background">
                            <CardContent className="p-4 flex flex-col items-center">
                                <div className="text-xs text-muted-foreground mb-1 uppercase tracking-wider">Raw Hits</div>
                                <div className="text-2xl font-bold">{analytics.stepCounts.vectorSearchRaw + analytics.stepCounts.keywordSearchRaw}</div>
                                <div className="text-[10px] text-muted-foreground flex gap-2 mt-1">
                                    <span>Vec: {analytics.stepCounts.vectorSearchRaw}</span>
                                    <span>Key: {analytics.stepCounts.keywordSearchRaw}</span>
                                </div>
                            </CardContent>
                        </Card>
                        <Card className="bg-background">
                            <CardContent className="p-4 flex flex-col items-center relative">
                                <div className="absolute left-[-1rem] top-1/2 -translate-y-1/2 text-muted-foreground/30 hidden md:block">→</div>
                                <div className="text-xs text-muted-foreground mb-1 uppercase tracking-wider">Scope Filter</div>
                                <div className="text-2xl font-bold">{analytics.stepCounts.afterScopeFilter}</div>
                                <div className="text-[10px] text-destructive mt-1">
                                    Dropped: {(analytics.stepCounts.vectorSearchRaw + analytics.stepCounts.keywordSearchRaw) - analytics.stepCounts.afterScopeFilter}
                                </div>
                            </CardContent>
                        </Card>
                        <Card className="bg-background">
                            <CardContent className="p-4 flex flex-col items-center relative">
                                <div className="absolute left-[-1rem] top-1/2 -translate-y-1/2 text-muted-foreground/30 hidden md:block">→</div>
                                <div className="text-xs text-muted-foreground mb-1 uppercase tracking-wider">Threshold</div>
                                <div className="text-2xl font-bold">{analytics.stepCounts.afterThresholdFilter}</div>
                                <div className="text-[10px] text-destructive mt-1">
                                    Dropped: {analytics.stepCounts.afterScopeFilter - analytics.stepCounts.afterThresholdFilter}
                                </div>
                            </CardContent>
                        </Card>
                        <Card className="bg-primary/5 border-primary/20">
                            <CardContent className="p-4 flex flex-col items-center relative">
                                <div className="absolute left-[-1rem] top-1/2 -translate-y-1/2 text-muted-foreground/30 hidden md:block">→</div>
                                <div className="text-xs text-primary font-bold mb-1 uppercase tracking-wider">Results</div>
                                <div className="text-2xl font-bold text-primary">{analytics.results?.length || 0}</div>
                                <div className="text-[10px] text-primary mt-1">
                                    Final Set
                                </div>
                            </CardContent>
                        </Card>
                    </div>
                </div>

                <div className="grid md:grid-cols-2 gap-8">
                    {/* 2. Accepted Results */}
                    <div className="space-y-4">
                        <h3 className="font-bold text-md flex items-center gap-2 text-green-600 dark:text-green-500">
                            <CheckCircle2 className="w-5 h-5" />
                            Accepted Documents
                        </h3>
                        {analytics.results?.length === 0 ? (
                             <div className="p-8 text-center border-2 border-dashed rounded-lg text-muted-foreground bg-muted/20">
                                 No documents survived the filter pipeline.
                             </div>
                        ) : (
                            analytics.results?.map((doc: any, i: number) => (
                                <Card key={i} className="text-sm border-green-200 dark:border-green-900 border-l-4">
                                    <CardHeader className="p-3 pb-2">
                                        <div className="flex justify-between items-start">
                                            <div className="overflow-hidden">
                                                <div className="font-mono font-bold truncate pr-2" title={doc.metadata?.fileName}>{doc.metadata?.fileName || 'Unknown'}</div>
                                                <div className="flex gap-1 mt-1 flex-wrap">
                                                    {doc.vectorRank ? (
                                                        <Tooltip>
                                                          <TooltipTrigger asChild>
                                                            <Badge variant="secondary" className="text-[10px] h-4 px-1 cursor-help font-normal text-muted-foreground bg-muted hover:bg-muted/80">
                                                              Vec #{doc.vectorRank}
                                                              {doc.vectorSimilarity !== undefined && <span className="opacity-70 ml-1 font-mono">Sim:{doc.vectorSimilarity.toFixed(2)}</span>}
                                                            </Badge>
                                                          </TooltipTrigger>
                                                          <TooltipContent>
                                                            <p className="font-bold">Vector Search</p>
                                                            <p className="text-[10px] opacity-80">Ranked by semantic similarity.</p>
                                                          </TooltipContent>
                                                        </Tooltip>
                                                    ) : (
                                                        <Badge variant="outline" className="text-[10px] h-4 px-1 text-muted-foreground border-dashed">No Vec</Badge>
                                                    )}
                                                    {doc.keywordRank ? (
                                                        <Tooltip>
                                                          <TooltipTrigger asChild>
                                                            <Badge variant="secondary" className="text-[10px] h-4 px-1 cursor-help font-normal text-muted-foreground bg-muted hover:bg-muted/80">
                                                              Key #{doc.keywordRank}
                                                              {doc.termFrequency !== undefined && <span className="opacity-70 ml-1 font-mono">TF:{doc.termFrequency}</span>}
                                                            </Badge>
                                                          </TooltipTrigger>
                                                          <TooltipContent>
                                                            <p className="font-bold">Keyword Search</p>
                                                            <p className="text-[10px] opacity-80">Ranked by term frequency.</p>
                                                          </TooltipContent>
                                                        </Tooltip>
                                                    ) : (
                                                        <Badge variant="outline" className="text-[10px] h-4 px-1 text-muted-foreground border-dashed">No Key</Badge>
                                                    )}
                                                </div>
                                            </div>
                                            <Tooltip>
                                              <TooltipTrigger asChild>
                                                <Badge variant="outline" className="font-mono bg-green-50 text-green-700 border-green-200 cursor-help">
                                                    RRF: {doc.score ? doc.score.toFixed(4) : 'N/A'}
                                                </Badge>
                                              </TooltipTrigger>
                                              <TooltipContent className="max-w-[300px]">
                                                <p className="font-bold mb-1">Reciprocal Rank Fusion (RRF)</p>
                                                <p className="text-xs mb-2">Combines Vector and Keyword ranks into a single score.</p>
                                                <div className="bg-muted p-2 rounded font-mono text-[10px]">
                                                  Score = (1 / (60 + {doc.vectorRank || '∞'})) + (1 / (60 + {doc.keywordRank || '∞'}))
                                                </div>
                                              </TooltipContent>
                                            </Tooltip>
                                        </div>
                                    </CardHeader>
                                    <CardContent className="p-3 pt-0 space-y-2">
                                        <div className="flex justify-between items-center text-[10px] text-muted-foreground bg-muted/30 px-2 py-1 rounded">
                                            <span className="font-mono">ID: {doc.id?.substring(0, 8)}...</span>
                                            <span>
                                                Indexed: {doc.metadata?.created_at ? new Date(doc.metadata.created_at).toLocaleString() : 'N/A'}
                                            </span>
                                        </div>
                                        <div className="bg-muted p-2 rounded text-xs font-mono line-clamp-3 text-muted-foreground">
                                            {doc.content}
                                        </div>
                                        <div className="flex justify-end pt-1 gap-1">
                                            <Button variant="ghost" size="sm" className="h-6 text-xs gap-1" onClick={() => setSelectedMetadata(doc.metadata)}>
                                                <FileJson className="w-3 h-3" />
                                                Metadata
                                            </Button>
                                            <Button variant="ghost" size="sm" className="h-6 text-xs gap-1" onClick={() => openDocument(doc.metadata?.fileId)}>
                                                <ExternalLink className="w-3 h-3" />
                                                Open
                                            </Button>
                                        </div>
                                    </CardContent>
                                </Card>
                            ))
                        )}
                    </div>

                    {/* 3. Rejected Analysis */}
                    <div className="space-y-4">
                        <h3 className="font-bold text-md flex items-center gap-2 text-destructive">
                            <Ban className="w-5 h-5" />
                            Rejected Candidates (Top 10)
                        </h3>
                        {analytics.rejectedDocuments?.length === 0 ? (
                             <div className="p-8 text-center border-2 border-dashed rounded-lg text-muted-foreground bg-muted/20">
                                 No rejected documents found in the captured top-k trace.
                             </div>
                        ) : (
                             analytics.rejectedDocuments?.map((doc: any, i: number) => (
                                <Card key={i} className="text-sm opacity-80 border-destructive/20 border-l-4">
                                    <CardHeader className="p-3 pb-2">
                                        <div className="flex justify-between items-start gap-2">
                                            <div className="overflow-hidden flex-1">
                                                <div className="font-mono font-bold truncate text-muted-foreground" title={doc.fileName}>{doc.fileName || 'Unknown'}</div>
                                                <div className="flex gap-1 mt-1 flex-wrap">
                                                    {doc.vectorRank ? (
                                                        <Badge variant="secondary" className="text-[10px] h-4 px-1 font-normal text-muted-foreground bg-muted">
                                                          Vec #{doc.vectorRank}
                                                          {doc.vectorSimilarity !== undefined && <span className="opacity-70 ml-1 font-mono">Sim:{doc.vectorSimilarity.toFixed(2)}</span>}
                                                        </Badge>
                                                    ) : (
                                                        <Badge variant="outline" className="text-[10px] h-4 px-1 text-muted-foreground/50 border-dashed">No Vec</Badge>
                                                    )}
                                                    {doc.keywordRank ? (
                                                        <Badge variant="secondary" className="text-[10px] h-4 px-1 font-normal text-muted-foreground bg-muted">
                                                          Key #{doc.keywordRank}
                                                          {doc.termFrequency !== undefined && <span className="opacity-70 ml-1 font-mono">TF:{doc.termFrequency}</span>}
                                                        </Badge>
                                                    ) : (
                                                        <Badge variant="outline" className="text-[10px] h-4 px-1 text-muted-foreground/50 border-dashed">No Key</Badge>
                                                    )}
                                                </div>
                                            </div>
                                            <div className="flex flex-col items-end gap-1">
                                                <Badge variant="destructive" className="font-mono text-[10px] shrink-0">REJECTED</Badge>
                                                <div className="text-[10px] font-mono text-muted-foreground">RRF: {doc.score?.toFixed(4)}</div>
                                            </div>
                                        </div>
                                    </CardHeader>
                                    <CardContent className="p-3 pt-0 space-y-2">
                                        <div className="text-xs text-destructive font-semibold bg-destructive/5 p-2 rounded flex items-start gap-2">
                                            <AlertCircle className="w-3 h-3 mt-0.5 shrink-0" />
                                            {doc.reason}
                                        </div>
                                        <div className="flex justify-between items-center text-[10px] text-muted-foreground px-1 py-1">
                                            <span className="font-mono truncate max-w-[150px]">ID: {doc.id?.substring(0, 8)}...</span>
                                            <span>
                                                Indexed: {doc.metadata?.created_at ? new Date(doc.metadata.created_at).toLocaleString() : 'N/A'}
                                            </span>
                                        </div>
                                        <div className="flex justify-end pt-1 gap-1">
                                            <Button variant="ghost" size="sm" className="h-6 text-xs gap-1" onClick={() => setSelectedMetadata(doc.metadata)}>
                                                <FileJson className="w-3 h-3" />
                                                Metadata
                                            </Button>
                                            <Button variant="ghost" size="sm" className="h-6 text-xs gap-1" onClick={() => openDocument(doc.metadata?.fileId)}>
                                                <ExternalLink className="w-3 h-3" />
                                                Investigate
                                            </Button>
                                        </div>
                                    </CardContent>
                                </Card>
                            ))
                        )}
                    </div>
                </div>
            </div>
        )}

        <Dialog open={!!selectedMetadata} onOpenChange={(open) => !open && setSelectedMetadata(null)}>
          <DialogContent className="max-w-2xl max-h-[80vh] flex flex-col">
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                <FileJson className="w-5 h-5"/>
                Document Metadata
              </DialogTitle>
              <DialogDescription>
                Raw metadata stored in the vector index.
              </DialogDescription>
            </DialogHeader>
            <div className="flex-1 overflow-auto bg-slate-950 text-slate-50 p-4 rounded-md font-mono text-xs">
              <pre>{JSON.stringify(selectedMetadata, null, 2)}</pre>
            </div>
          </DialogContent>
        </Dialog>

        <Dialog open={confirmDialog.isOpen} onOpenChange={(open) => setConfirmDialog(prev => ({ ...prev, isOpen: open }))}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>{confirmDialog.title}</DialogTitle>
                    <DialogDescription>
                        {confirmDialog.description}
                    </DialogDescription>
                </DialogHeader>
                <div className="flex justify-end gap-2 mt-4">
                    <Button variant="outline" onClick={() => setConfirmDialog(prev => ({ ...prev, isOpen: false }))}>Cancel</Button>
                    <Button onClick={() => { confirmDialog.onConfirm(); setConfirmDialog(prev => ({ ...prev, isOpen: false })); }}>Confirm</Button>
                </div>
            </DialogContent>
        </Dialog>

      </div>
      </TabsContent>

      {/* Tab: Document Manager */}
      <TabsContent value="documents" className="flex-1 flex flex-col overflow-hidden bg-background">
        <div className="p-4 border-b flex justify-between items-center">
            <div className="flex items-center gap-2">
                 <Badge variant="secondary" className="font-mono">{params.scope}</Badge>
                 <span className="text-sm text-muted-foreground">Scope Documents</span>
            </div>
            <div className="flex items-center gap-2">
                 <div className="flex items-center gap-2 mr-4 text-xs text-muted-foreground">
                    <Label htmlFor="scope-select" className="text-xs">Scope:</Label>
                    <Select 
                        value={params.scope} 
                        onValueChange={(v) => { setParams(p => ({ ...p, scope: v })); if(activeTab === 'documents') setTimeout(fetchDocuments, 100); }}
                    >
                        <SelectTrigger id="scope-select" className="h-7 w-[120px]">
                        <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                        <SelectItem value="SYSTEM">System</SelectItem>
                        <SelectItem value="TENANT">Tenant</SelectItem>
                        <SelectItem value="USER">User</SelectItem>
                        </SelectContent>
                    </Select>
                 </div>
                 <Button variant="outline" size="sm" onClick={fetchDocuments} disabled={loading} className="h-7 text-xs">
                    <Activity className="w-3 h-3 mr-2" />
                    Refresh
                 </Button>
            </div>
        </div>
        <div className="flex-1 overflow-auto p-6">
            <Card>
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>File Name</TableHead>
                            <TableHead className="w-[100px]">ID</TableHead>
                            <TableHead className="w-[150px]">Indexed At</TableHead>
                            <TableHead className="w-[100px] text-right">Actions</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {documents.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={4} className="h-24 text-center text-muted-foreground">
                                    No documents found in {params.scope} scope.
                                </TableCell>
                            </TableRow>
                        ) : (
                            documents.map((doc: any) => (
                                <TableRow key={doc.fileId}>
                                    <TableCell className="font-medium">
                                        <div className="flex items-center gap-2">
                                            <FileText className="w-4 h-4 text-blue-500" />
                                            {doc.fileName || 'Unknown'}
                                        </div>
                                    </TableCell>
                                    <TableCell className="font-mono text-xs text-muted-foreground">
                                        {doc.fileId?.substring(0, 8)}...
                                    </TableCell>
                                    <TableCell className="text-xs text-muted-foreground">
                                        {doc.createdAt ? new Date(doc.createdAt).toLocaleString() : '-'}
                                    </TableCell>
                                    <TableCell className="text-right">
                                        <Button 
                                            variant="ghost" 
                                            size="sm" 
                                            className="h-8 w-8 p-0 text-destructive hover:text-destructive hover:bg-destructive/10"
                                            onClick={() => handleDeleteDocument(doc.fileId)}
                                        >
                                            <Trash2 className="w-4 h-4" />
                                        </Button>
                                    </TableCell>
                                </TableRow>
                            ))
                        )}
                    </TableBody>
                </Table>
            </Card>
        </div>
      </TabsContent>
    </Tabs>
    </TooltipProvider>
  );
}
