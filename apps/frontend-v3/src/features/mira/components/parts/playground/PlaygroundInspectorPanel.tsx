import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Badge,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from '@mirel/ui';
import { Database, Terminal } from 'lucide-react';

interface PlaygroundInspectorPanelProps {
  lastResponse: any;
}

export function PlaygroundInspectorPanel({ lastResponse }: PlaygroundInspectorPanelProps) {
  return (
    <div className="w-96 border-l bg-muted/10 flex flex-col min-w-[300px] overflow-hidden">
        <Tabs defaultValue="rag" className="flex-1 flex flex-col">
          <TabsList className="w-full justify-start rounded-none border-b bg-transparent p-0">
            <TabsTrigger value="rag" className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:bg-transparent">
              <Database className="w-4 h-4 mr-2" />
              Context
            </TabsTrigger>
            <TabsTrigger value="raw" className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:bg-transparent">
              <Terminal className="w-4 h-4 mr-2" />
              Raw
            </TabsTrigger>
          </TabsList>

          <TabsContent value="rag" className="flex-1 overflow-y-auto p-4 m-0">
              {!lastResponse && <div className="text-muted-foreground text-sm text-center py-10">Run search to see context</div>}
              {lastResponse?.ragDocuments?.length > 0 ? (
                  <div className="space-y-4">
                      {lastResponse.ragDocuments.map((doc: any, i: number) => (
                          <Card key={i} className="text-xs">
                              <CardHeader className="p-3 pb-1">
                                  <div className="flex items-center justify-between">
                                      <CardTitle className="leading-tight truncate pr-2 text-primary">{doc.fileName}</CardTitle>
                                      {typeof doc.score === 'number' && <Badge variant="outline">{doc.score.toFixed(3)}</Badge>}
                                  </div>
                              </CardHeader>
                              <CardContent className="p-3">
                                  <div className="bg-muted p-2 rounded font-mono text-[10px] overflow-x-auto max-h-[150px] whitespace-pre-wrap">
                                      {doc.content}
                                  </div>
                              </CardContent>
                          </Card>
                      ))}
                  </div>
              ) : lastResponse && (
                   <div className="text-muted-foreground text-sm text-center py-10">No documents retrieved</div>
              )}
          </TabsContent>

          <TabsContent value="raw" className="flex-1 overflow-y-auto p-4 m-0">
               <pre className="text-[10px] font-mono whitespace-pre-wrap break-all bg-muted p-2 rounded">
                   {lastResponse ? JSON.stringify(lastResponse, null, 2) : '// No response yet'}
               </pre>
          </TabsContent>
        </Tabs>
    </div>
  );
}
