
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
  Slider
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
  CheckCircle2
} from 'lucide-react';
import { apiClient } from '@/lib/api/client';

interface DebuggerStats {
  totalDocumentCount: number;
  countByScope: Record<string, number>;
  isIndexEmpty: boolean;
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
    keywordRank?: number;
    metadata: any;
  }[];
}

export function PlaygroundIndexDebuggerPanel() {
  const { toast } = useToast();
  const [loading, setLoading] = useState(false);
  const [stats, setStats] = useState<DebuggerStats | null>(null);
  const [analytics, setAnalytics] = useState<DebuggerAnalytics | null>(null);

  const [params, setParams] = useState({
    query: '',
    scope: 'USER',
    targetTenantId: '',
    targetUserId: '',
    topK: 5,
    threshold: 0.0
  });

  useEffect(() => {
    fetchStats();
  }, []);

  const fetchStats = async () => {
    try {
      const res = await apiClient.get('/api/mira/debugger/stats');
      setStats(res.data);
    } catch (error) {
      console.error('Failed to fetch stats', error);
      // Silent fail or toast? Just log for stats.
    }
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

  const openDocument = (fileId: string) => {
      if (!fileId) return;
      // Open in new tab or navigate
      window.open(`/admin/mira/knowledge/view/${fileId}`, '_blank');
  };

  return (
    <div className="flex h-full w-full bg-background">
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
           <Button variant="ghost" size="sm" className="w-full mt-2 h-6 text-xs" onClick={fetchStats}>Refresh Stats</Button>
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
              <Label>Scope</Label>
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
                   <Label>Top K</Label>
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
                   <Label>Threshold</Label>
                   <span className="text-xs text-muted-foreground">{params.threshold.toFixed(2)}</span>
                 </div>
                 <Slider
                   min={0}
                   max={1}
                   step={0.05}
                   value={[params.threshold]}
                   onValueChange={([v]) => setParams(p => ({ ...p, threshold: v ?? 0.0 }))}
                 />
              </div>
            </div>

            <Button onClick={handleAnalyze} disabled={loading} className="w-full mt-4">
              {loading ? <Loader2 className="w-4 h-4 animate-spin mr-2" /> : <Activity className="w-4 h-4 mr-2" />}
              Analyze Trace
            </Button>

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
                                                        <Badge variant="outline" className="text-[10px] h-4 px-1 bg-blue-50/50 text-blue-700 border-blue-200">Vec #{doc.vectorRank}</Badge>
                                                    ) : (
                                                        <Badge variant="outline" className="text-[10px] h-4 px-1 text-muted-foreground border-dashed">Vec -</Badge>
                                                    )}
                                                    {doc.keywordRank ? (
                                                        <Badge variant="outline" className="text-[10px] h-4 px-1 bg-amber-50/50 text-amber-700 border-amber-200">Key #{doc.keywordRank}</Badge>
                                                    ) : (
                                                        <Badge variant="outline" className="text-[10px] h-4 px-1 text-muted-foreground border-dashed">Key -</Badge>
                                                    )}
                                                </div>
                                            </div>
                                            <Badge variant="outline" className="font-mono bg-green-50 text-green-700 border-green-200">
                                                RRF: {doc.score ? doc.score.toFixed(4) : 'N/A'}
                                            </Badge>
                                        </div>
                                    </CardHeader>
                                    <CardContent className="p-3 pt-0 space-y-2">
                                        <div className="bg-muted p-2 rounded text-xs font-mono line-clamp-3 text-muted-foreground">
                                            {doc.content}
                                        </div>
                                        <div className="flex justify-end pt-1">
                                            <Button variant="ghost" size="sm" className="h-6 text-xs gap-1" onClick={() => openDocument(doc.metadata?.fileId)}>
                                                <ExternalLink className="w-3 h-3" />
                                                Open Document
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
                                                        <Badge variant="outline" className="text-[10px] h-4 px-1 bg-muted text-muted-foreground">Vec #{doc.vectorRank}</Badge>
                                                    ) : (
                                                        <Badge variant="outline" className="text-[10px] h-4 px-1 text-muted-foreground/50 border-dashed">Vec -</Badge>
                                                    )}
                                                    {doc.keywordRank ? (
                                                        <Badge variant="outline" className="text-[10px] h-4 px-1 bg-muted text-muted-foreground">Key #{doc.keywordRank}</Badge>
                                                    ) : (
                                                        <Badge variant="outline" className="text-[10px] h-4 px-1 text-muted-foreground/50 border-dashed">Key -</Badge>
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
                                        <div className="flex justify-end pt-1">
                                            <Button variant="ghost" size="sm" className="h-6 text-xs gap-1" onClick={() => openDocument(doc.metadata?.fileId)}>
                                                <ExternalLink className="w-3 h-3" />
                                                Investigate Source
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
      </div>
    </div>
  );
}
