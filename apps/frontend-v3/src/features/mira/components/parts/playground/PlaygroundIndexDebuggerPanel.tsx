
import { useState } from 'react';
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
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
import { Search, Loader2 } from 'lucide-react';
import { apiClient } from '@/lib/api/client';

export function PlaygroundIndexDebuggerPanel() {
  const { toast } = useToast();
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState<any[]>([]);

  const [params, setParams] = useState({
    query: '',
    scope: 'USER',
    targetTenantId: '',
    targetUserId: '',
    topK: 5,
    threshold: 0.0
  });

  const handleSearch = async () => {
    if (!params.query.trim()) {
      toast({
        title: 'Validation Error',
        description: 'Please enter a search query',
        variant: 'destructive'
      });
      return;
    }

    setLoading(true);
    setResults([]);

    try {
      // API call to debug search
      const res = await apiClient.post('/api/mira/knowledge/search', params);
      setResults(res.data.results || []);
      
      toast({
        title: 'Search Complete',
        description: `Found ${res.data.results?.length || 0} documents.`
      });

    } catch (error: any) {
      console.error(error);
      toast({
        title: 'Search Failed',
        description: error.response?.data?.message || 'Failed to execute search',
        variant: 'destructive'
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex h-full w-full">
      {/* Left Pane: Config */}
      <div className="w-80 border-r bg-muted/10 p-4 flex flex-col gap-6 overflow-y-auto">
        <div className="space-y-4">
          <h3 className="font-semibold flex items-center gap-2">
            <Search className="w-4 h-4" />
            Debug Parameters
          </h3>

          <div className="space-y-2">
            <Label>Search Query</Label>
            <Textarea 
              placeholder="Enter search phrase..." 
              value={params.query}
              onChange={(e) => setParams(p => ({ ...p, query: e.target.value }))}
              rows={4}
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
                <SelectItem value="SYSTEM">SYSTEM (Global)</SelectItem>
                <SelectItem value="TENANT">TENANT (Shared)</SelectItem>
                <SelectItem value="USER">USER (Personal)</SelectItem>
              </SelectContent>
            </Select>
          </div>
          
          <Separator />
          
          <div className="space-y-2">
             <Label>Target (Impersonation)</Label>
             <Input 
               placeholder="Target Tenant ID" 
               className="text-xs font-mono"
               value={params.targetTenantId}
               onChange={(e) => setParams(p => ({ ...p, targetTenantId: e.target.value }))}
             />
             <Input 
               placeholder="Target User ID" 
               className="text-xs font-mono"
               value={params.targetUserId}
               onChange={(e) => setParams(p => ({ ...p, targetUserId: e.target.value }))}
             />
             <div className="text-[10px] text-muted-foreground">
               * Requires ADMIN role. Leave empty to use your own context.
             </div>
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
                 onValueChange={([v]) => setParams(p => ({ ...p, topK: v }))}
               />
            </div>

            <div className="space-y-2">
               <div className="flex justify-between">
                 <Label>Threshold (Min Score)</Label>
                 <span className="text-xs text-muted-foreground">{params.threshold.toFixed(2)}</span>
               </div>
               <Slider
                 min={0}
                 max={1}
                 step={0.05}
                 value={[params.threshold]}
                 onValueChange={([v]) => setParams(p => ({ ...p, threshold: v }))}
               />
            </div>
          </div>

          <Button onClick={handleSearch} disabled={loading} className="w-full mt-4">
            {loading ? <Loader2 className="w-4 h-4 animate-spin mr-2" /> : <Search className="w-4 h-4 mr-2" />}
            Execute Search
          </Button>

        </div>
      </div>

      {/* Right Pane: Results */}
      <div className="flex-1 p-4 bg-muted/5 overflow-y-auto">
        <h3 className="font-semibold mb-4">Results ({results.length})</h3>
        
        {results.length === 0 && !loading && (
            <div className="flex flex-col items-center justify-center h-64 text-muted-foreground">
                <Search className="w-8 h-8 mb-2 opacity-50" />
                <p>No results found or search not executed</p>
            </div>
        )}

        <div className="space-y-4 max-w-4xl mx-auto">
          {results.map((doc: any, i: number) => (
            <Card key={i} className="text-sm">
               <CardHeader className="p-4 pb-2">
                 <div className="flex justify-between items-start">
                    <div className="space-y-1">
                        <CardTitle className="text-base text-primary font-mono">{doc.fileName || 'Unknown File'}</CardTitle>
                        <div className="flex gap-2 text-xs text-muted-foreground font-mono">
                            <span>ID: {doc.id?.substring(0, 8)}...</span>
                            <span>•</span>
                            <span>{doc.metadata?.scope || 'UNKNOWN'}</span>
                        </div>
                    </div>
                    <Badge variant={doc.score >= 0.7 ? 'default' : 'secondary'} className="font-mono text-xs">
                        {typeof doc.score === 'number' ? doc.score.toFixed(4) : 'N/A'}
                    </Badge>
                 </div>
               </CardHeader>
               <CardContent className="p-4 pt-2">
                  <div className="bg-muted p-3 rounded font-mono text-xs whitespace-pre-wrap max-h-60 overflow-y-auto border">
                      {doc.content}
                  </div>
                  <div className="mt-2 pt-2 border-t">
                      <details className="text-xs">
                          <summary className="cursor-pointer text-muted-foreground hover:text-foreground">View Metadata</summary>
                          <pre className="mt-2 p-2 bg-black/5 dark:bg-white/5 rounded overflow-x-auto">
                              {JSON.stringify(doc.metadata, null, 2)}
                          </pre>
                      </details>
                  </div>
               </CardContent>
            </Card>
          ))}
        </div>
      </div>
    </div>
  );
}
