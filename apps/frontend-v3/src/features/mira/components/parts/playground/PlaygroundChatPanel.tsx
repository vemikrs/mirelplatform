import { useRef, useEffect } from 'react';
import { Button, Textarea, ScrollArea } from '@mirel/ui';
import { Play, RefreshCw, Loader2 } from 'lucide-react';
import { cn } from '@/lib/utils/cn';

interface Message {
  role: 'user' | 'assistant';
  content: string;
}

interface PlaygroundChatPanelProps {
  messages: Message[];
  currentInput: string;
  isLoading: boolean;
  onInputChange: (val: string) => void;
  onRun: () => void;
  onClear: () => void;
}

export function PlaygroundChatPanel({ 
  messages, 
  currentInput, 
  isLoading, 
  onInputChange, 
  onRun, 
  onClear 
}: PlaygroundChatPanelProps) {
  
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  return (
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
             <Button variant="outline" size="icon" onClick={onClear} title="Clear Chat">
                <RefreshCw className="w-4 h-4" />
             </Button>
             <Textarea 
                value={currentInput}
                onChange={e => onInputChange(e.target.value)}
                placeholder="Enter your message... (Ctrl+Enter to run)"
                className="min-h-[50px] resize-none"
                onKeyDown={e => {
                    if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
                        onRun();
                    }
                }}
             />
             <Button onClick={onRun} disabled={isLoading} className="h-auto">
                <Play className="w-4 h-4 mr-2" />
                Run
             </Button>
          </div>
       </div>
    </div>
  );
}
