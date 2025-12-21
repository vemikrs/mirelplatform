import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Button,
  Input,
  Textarea,
  Label,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Slider,
  Switch,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  ScrollArea,
  Separator,
  Badge,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
  useToast
} from '@mirel/ui';
import { ArrowLeft, Play, Settings2, Database, Terminal, RefreshCw, Loader2 } from 'lucide-react';
import { cn } from '@/lib/utils/cn';
import { apiClient } from '@/lib/api/client';

export function MiraPlaygroundPage() {
  const navigate = useNavigate();
  const { toast } = useToast();

  // State Management (could use context or URL params for persistence)
  const [model, setModel] = useState('gemini-1.5-flash');
  const [temperature, setTemperature] = useState(0.7);
  const [maxTokens, setMaxTokens] = useState(2048);
  const [topP, setTopP] = useState(0.95);
  const [topK, setTopK] = useState(40);
  const [systemPrompt, setSystemPrompt] = useState('You are a helpful AI assistant.');
  
  const [ragEnabled, setRagEnabled] = useState(false);
  const [ragScope, setRagScope] = useState('SYSTEM');
  const [ragTopK, setRagTopK] = useState(3);

  const [messages, setMessages] = useState<{role: 'user'|'assistant', content: string}[]>([]);
  const [currentInput, setCurrentInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  
  const [lastResponse, setLastResponse] = useState<any>(null);

  // Scroll to bottom
  const messagesEndRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleRun = async () => {
    if (!currentInput.trim() && messages.length === 0) return;
    
    // If input exists, add it to messages first
    let msgsToSend = [...messages];
    if (currentInput.trim()) {
      const newMsg = { role: 'user' as const, content: currentInput };
      msgsToSend = [...msgsToSend, newMsg];
      setMessages(msgsToSend);
      setCurrentInput('');
    }

    setIsLoading(true);
    setLastResponse(null);

    try {
      const payload = {
        messages: msgsToSend,
        model,
        temperature,
        topP,
        topK,
        maxTokens,
        systemInstruction: systemPrompt,
        ragSettings: {
          enabled: ragEnabled,
          scope: ragScope,
          topK: ragTopK
        }
      };

      const res = await apiClient.post('/api/mira/admin/playground/chat', payload);
      const data = res.data;

      setLastResponse(data);
      setMessages(prev => [...prev, { role: 'assistant', content: data.content }]);

    } catch (error: any) {
      console.error(error);
      toast({
        title: 'Error',
        description: error.response?.data?.message || 'Failed to execute chat',
        variant: 'destructive'
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleClear = () => {
    setMessages([]);
    setLastResponse(null);
  };

  return (
    <div className="flex flex-col h-screen bg-background overflow-hidden">
      {/* Header */}
      <header className="flex-shrink-0 h-14 border-b flex items-center px-4 justify-between bg-card z-10">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" onClick={() => navigate('/admin/mira')}>
            <ArrowLeft className="w-4 h-4" />
          </Button>
          <div className="flex flex-col">
            <span className="text-xs text-muted-foreground font-medium">Mira Admin</span>
            <span className="font-semibold">AI Playground</span>
          </div>
        </div>
        <div className="flex items-center gap-2">
            {/* Future: Save presets */}
        </div>
      </header>

      {/* Main Content - 3 Panes */}
      <div className="flex-1 flex overflow-hidden">
        
        {/* Left Pane: Configuration */}
        <div className="w-80 border-r bg-muted/10 flex flex-col min-w-[320px] overflow-y-auto p-4 space-y-6">
          
          <div className="space-y-4">
            <div className="flex items-center gap-2 font-semibold text-sm">
              <Settings2 className="w-4 h-4" />
              Model Parameters
            </div>
            
            <div className="space-y-2">
              <Label>Model</Label>
              <Select value={model} onValueChange={setModel}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="gemini-1.5-flash">Gemini 1.5 Flash</SelectItem>
                  <SelectItem value="gemini-1.5-pro">Gemini 1.5 Pro</SelectItem>
                  <SelectItem value="gpt-4o">GPT-4o</SelectItem>
                  {/* Add more dynamically later */}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-4 pt-2">
              <div className="space-y-2">
                <div className="flex justify-between">
                  <Label>Temperature</Label>
                  <span className="text-xs text-muted-foreground">{temperature}</span>
                </div>
                <Slider 
                  value={[temperature]} 
                  onValueChange={([v]) => { if (v !== undefined) setTemperature(v); }} 
                  max={2} step={0.1} 
                />
              </div>

              <div className="space-y-2">
                <div className="flex justify-between">
                  <Label>Top P</Label>
                  <span className="text-xs text-muted-foreground">{topP}</span>
                </div>
                <Slider 
                  value={[topP]} 
                  onValueChange={([v]) => { if (v !== undefined) setTopP(v); }} 
                  max={1} step={0.05} 
                />
              </div>

              <div className="space-y-2">
                <div className="flex justify-between">
                  <Label>Top K</Label>
                  <span className="text-xs text-muted-foreground">{topK}</span>
                </div>
                 <Input 
                    type="number" 
                    value={topK} 
                    onChange={e => setTopK(Number(e.target.value))} 
                    className="h-8"
                 />
              </div>
              
               <div className="space-y-2">
                <div className="flex justify-between">
                  <Label>Max Tokens</Label>
                  <span className="text-xs text-muted-foreground">{maxTokens}</span>
                </div>
                 <Input 
                    type="number" 
                    value={maxTokens} 
                    onChange={e => setMaxTokens(Number(e.target.value))} 
                    className="h-8"
                 />
              </div>
            </div>
          </div>

          <Separator />

          <div className="space-y-4">
             <div className="flex items-center gap-2 font-semibold text-sm">
              <Database className="w-4 h-4" />
              RAG Settings
            </div>
             <div className="flex items-center justify-between">
                <Label htmlFor="rag-mode" className="cursor-pointer">Enable RAG</Label>
                <Switch id="rag-mode" checked={ragEnabled} onCheckedChange={setRagEnabled} />
             </div>

             {ragEnabled && (
                <div className="space-y-3 pl-2 border-l-2 border-muted animate-in fade-in slide-in-from-top-2">
                   <div className="space-y-2">
                      <Label>Scope</Label>
                      <Select value={ragScope} onValueChange={setRagScope}>
                        <SelectTrigger className="h-8">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="SYSTEM">SYSTEM (Global)</SelectItem>
                          <SelectItem value="TENANT">TENANT</SelectItem>
                          <SelectItem value="USER">USER</SelectItem>
                        </SelectContent>
                      </Select>
                   </div>
                   <div className="space-y-2">
                      <Label>Top K</Label>
                      <Input type="number" value={ragTopK} onChange={e => setRagTopK(Number(e.target.value))} className="h-8" />
                   </div>
                </div>
             )}
          </div>

          <Separator />
          
           <div className="space-y-2 flex-1 flex flex-col">
              <Label>System Instruction</Label>
              <Textarea 
                value={systemPrompt}
                onChange={e => setSystemPrompt(e.target.value)}
                className="flex-1 min-h-[100px] text-xs resize-none font-mono"
              />
           </div>

        </div>

        {/* Center Pane: Chat */}
        <div className="flex-1 flex flex-col min-w-[400px]">
           <ScrollArea className="flex-1 p-4">
              <div className="space-y-6 max-w-3xl mx-auto">
                 {messages.length === 0 && (
                    <div className="text-center text-muted-foreground py-20">
                       <Play className="w-12 h-12 mx-auto mb-4 opacity-20" />
                       <p>Start a conversation to test the model.</p>
                       <p className="text-xs mt-2">Parameters on the left, Results on the right.</p>
                    </div>
                 )}
                 {messages.map((msg, i) => (
                    <div key={i} className={cn(
                        "flex flex-col gap-1",
                        msg.role === 'user' ? "items-end" : "items-start"
                    )}>
                        <div className="text-xs text-muted-foreground uppercase px-1">{msg.role}</div>
                        <div className={cn(
                           "p-3 rounded-lg max-w-[85%] text-sm whitespace-pre-wrap",
                           msg.role === 'user' 
                             ? "bg-primary text-primary-foreground" 
                             : "bg-muted border"
                        )}>
                           {msg.content}
                        </div>
                    </div>
                 ))}
                 {isLoading && (
                    <div className="flex items-center gap-2 text-muted-foreground text-sm p-2">
                       <Loader2 className="w-4 h-4 animate-spin" />
                       Generating...
                    </div>
                 )}
                 <div ref={messagesEndRef} />
              </div>
           </ScrollArea>
           
           <div className="p-4 border-t bg-background/50 backdrop-blur-sm">
              <div className="relative max-w-3xl mx-auto flex gap-2">
                 <Button variant="outline" size="icon" onClick={handleClear} title="Clear Chat">
                    <RefreshCw className="w-4 h-4" />
                 </Button>
                 <Textarea 
                    value={currentInput}
                    onChange={e => setCurrentInput(e.target.value)}
                    placeholder="Enter your message... (Ctrl+Enter to run)"
                    className="min-h-[50px] resize-none"
                    onKeyDown={e => {
                        if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
                            handleRun();
                        }
                    }}
                 />
                 <Button onClick={handleRun} disabled={isLoading} className="h-auto">
                    <Play className="w-4 h-4 mr-2" />
                    Run
                 </Button>
              </div>
           </div>
        </div>

        {/* Right Pane: Inspection */}
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
                                      <div className="flex justify-between items-start">
                                          <CardTitle className="leading-tight truncate pr-2 text-primary">{doc.fileName}</CardTitle>
                                          {doc.score !== null && <Badge variant="outline">{doc.score.toFixed(3)}</Badge>}
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

      </div>
    </div>
  );
}
