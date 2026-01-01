import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, useToast } from '@mirel/ui';
import { ArrowLeft, Loader2 } from 'lucide-react';
import { apiClient } from '@/lib/api/client';
import { PlaygroundConfigPanel } from '../parts/playground/PlaygroundConfigPanel';
import { PlaygroundChatPanel } from '../parts/playground/PlaygroundChatPanel';
import { PlaygroundInspectorPanel } from '../parts/playground/PlaygroundInspectorPanel';
import { PlaygroundIndexDebuggerPanel } from '../parts/playground/PlaygroundIndexDebuggerPanel';
import { Tabs, TabsList, TabsTrigger } from '@mirel/ui';

interface Config {
  models: { id: string; name: string; provider: string }[];
  defaultParams: {
    temperature: number;
    topP: number;
    topK: number;
    maxTokens: number;
    systemPrompt: string;
  };
}

export function MiraPlaygroundPage() {
  const navigate = useNavigate();
  const { toast } = useToast();

  const [config, setConfig] = useState<Config | null>(null);
  const [configLoading, setConfigLoading] = useState(true);

  // Settings State
  const [settings, setSettings] = useState({
    model: '',
    temperature: 0.7,
    maxTokens: 2048,
    topP: 0.95,
    topK: 40,
    systemPrompt: '',
    ragEnabled: false,
    ragScope: 'SYSTEM',
    ragTopK: 3,
    // リランカー設定
    rerankerEnabled: false,
    rerankerTopN: 5
  });

  const [mode, setMode] = useState<'chat' | 'debug'>('chat');

  const [messages, setMessages] = useState<{role: 'user'|'assistant', content: string}[]>([]);
  const [currentInput, setCurrentInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  
  const [lastResponse, setLastResponse] = useState<any>(null);

  // Fetch Configuration
  useEffect(() => {
    const fetchConfig = async () => {
      try {
        const res = await apiClient.get('/api/mira/admin/playground/config');
        const data = res.data;
        setConfig(data);
        
        // Initial setup with defaults
        setSettings(prev => ({
          ...prev,
          model: data.models[0]?.id || '',
          temperature: data.defaultParams.temperature,
          topP: data.defaultParams.topP,
          topK: data.defaultParams.topK,
          maxTokens: data.defaultParams.maxTokens,
          systemPrompt: data.defaultParams.systemPrompt
        }));
      } catch (error) {
        console.error('Failed to load config', error);
        toast({
          title: 'Configuration Error',
          description: 'Failed to load playground configuration.',
          variant: 'destructive'
        });
      } finally {
        setConfigLoading(false);
      }
    };
    fetchConfig();
  }, [toast]);

  const handleSettingsChange = (key: keyof typeof settings, value: any) => {
    setSettings(prev => ({ ...prev, [key]: value }));
  };

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
        model: settings.model,
        temperature: settings.temperature,
        topP: settings.topP,
        topK: settings.topK,
        maxTokens: settings.maxTokens,
        systemInstruction: settings.systemPrompt,
        ragSettings: {
          enabled: settings.ragEnabled,
          scope: settings.ragScope,
          topK: settings.ragTopK
        },
        rerankerSettings: {
          enabled: settings.rerankerEnabled ? true : null,
          topN: settings.rerankerEnabled ? settings.rerankerTopN : null
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

  if (configLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <Loader2 className="w-8 h-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col bg-background overflow-hidden">
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
      </header>

      <div className="border-b px-4 py-2 bg-muted/20 flex justify-center">
          <Tabs value={mode} onValueChange={(v) => setMode(v as any)} className="w-[400px]">
              <TabsList className="grid w-full grid-cols-2">
                  <TabsTrigger value="chat">Chat Playground</TabsTrigger>
                  <TabsTrigger value="debug">Index Debugger</TabsTrigger>
              </TabsList>
          </Tabs>
      </div>

      {/* Main Content - 3 Panes */}
      <div className="flex-1 flex overflow-hidden">
        
        {mode === 'chat' ? (
            <>
                <PlaygroundConfigPanel 
                  config={config} 
                  settings={settings} 
                  onSettingsChange={handleSettingsChange} 
                />

                <PlaygroundChatPanel 
                  messages={messages}
                  currentInput={currentInput}
                  isLoading={isLoading}
                  onInputChange={setCurrentInput}
                  onRun={handleRun}
                  onClear={handleClear}
                />

                <PlaygroundInspectorPanel 
                  lastResponse={lastResponse} 
                />
            </>
        ) : (
            <PlaygroundIndexDebuggerPanel />
        )}

      </div>
    </div>
  );
}
