import { useState, useEffect } from 'react';
import { 
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter,
  Button, Input, Label, Switch, Tabs, TabsContent, TabsList, TabsTrigger,
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
  Textarea
} from '@mirel/ui';
import { type MessageConfig } from '@/lib/api/mira';
import { AiPresetSuggestion } from './AiPresetSuggestion';
import { History, Sparkles, Layers, Terminal } from 'lucide-react';

interface ContextSwitcherModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  config: MessageConfig;
  onConfigChange: (config: MessageConfig) => void;
  messageContent: string;
}

export function ContextSwitcherModal({
  open,
  onOpenChange,
  config,
  onConfigChange,
  messageContent,
}: ContextSwitcherModalProps) {
  const [activeTab, setActiveTab] = useState('history');
  
  // Update local config when external config changes
  const [localConfig, setLocalConfig] = useState<MessageConfig>(config);

  useEffect(() => {
    if (open) {
      setLocalConfig(config);
    }
  }, [open, config]);

  const handleSave = () => {
    onConfigChange(localConfig);
    onOpenChange(false);
  };

  const updateConfig = (updates: Partial<MessageConfig>) => {
    setLocalConfig(prev => ({ ...prev, ...updates }));
  };

  const handleApplySuggestion = (suggestion: MessageConfig) => {
    setLocalConfig(suggestion);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Context Switcher</DialogTitle>
          <AiPresetSuggestion 
            messageContent={messageContent} 
            onApply={handleApplySuggestion} 
            className="mt-2"
          />
        </DialogHeader>

        <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
          <TabsList className="grid w-full grid-cols-4">
            <TabsTrigger value="history" className="gap-2">
              <History className="w-4 h-4" /> History
            </TabsTrigger>
            <TabsTrigger value="context" className="gap-2">
              <Layers className="w-4 h-4" /> Context
            </TabsTrigger>
            <TabsTrigger value="editor" className="gap-2">
              <Terminal className="w-4 h-4" /> Editor
            </TabsTrigger>
            {/* Presets tab for Phase 2 */}
            <TabsTrigger value="presets" className="gap-2" disabled>
              <Sparkles className="w-4 h-4" /> Presets
            </TabsTrigger>
          </TabsList>

          <TabsContent value="history" className="space-y-4 py-4">
            <div className="space-y-4">
              <div className="space-y-2">
                <Label>History Scope</Label>
                <div className="flex flex-col gap-2">
                   <Select 
                      value={localConfig.historyScope || 'auto'}
                      onValueChange={(val: any) => updateConfig({ historyScope: val })}
                   >
                    <SelectTrigger>
                      <SelectValue placeholder="Select history scope" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="auto">Auto (Default)</SelectItem>
                      <SelectItem value="recent">Recent N Messages</SelectItem>
                      <SelectItem value="none">No History (One-shot)</SelectItem>
                    </SelectContent>
                   </Select>
                   <p className="text-xs text-muted-foreground">
                     Controls how much conversation history is included in the context.
                   </p>
                </div>
              </div>

              {localConfig.historyScope === 'recent' && (
                <div className="space-y-2">
                  <Label>Message Count</Label>
                  <Input 
                    type="number" 
                    min={1} 
                    max={50}
                    value={localConfig.recentCount || 5}
                    onChange={(e) => updateConfig({ recentCount: parseInt(e.target.value) || 5 })}
                  />
                </div>
              )}
            </div>
          </TabsContent>

          <TabsContent value="context" className="space-y-4 py-4">
            <div className="space-y-2">
              <Label>Context Layers Override</Label>
              <p className="text-xs text-muted-foreground">
                Override specific context layers for this message.
              </p>
              {/* Note: This is a simplified UI for MVP. Real keys would be dynamic. */}
              <div className="border rounded-md p-4 space-y-4">
                {['system', 'terminology', 'style', 'workflow'].map(key => {
                  const override = localConfig.contextOverrides?.[key] || { enabled: true, priority: 0 };
                  return (
                    <div key={key} className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <Label htmlFor={`ctx-${key}`} className="capitalize">{key}</Label>
                      </div>
                      <div className="flex items-center gap-2">
                        <Switch 
                          id={`ctx-${key}`}
                          checked={override.enabled !== false}
                          onCheckedChange={(checked) => {
                             const newOverrides = { ...localConfig.contextOverrides || {} };
                             newOverrides[key] = { ...override, enabled: checked };
                             updateConfig({ contextOverrides: newOverrides });
                          }}
                        />
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </TabsContent>

          <TabsContent value="editor" className="space-y-4 py-4">
            <div className="space-y-2">
              <Label>Temporary Context</Label>
              <Textarea 
                placeholder="Add temporary instructions for this message..."
                className="min-h-[200px]"
                value={localConfig.temporaryContext || ''}
                onChange={(e) => updateConfig({ temporaryContext: e.target.value })}
              />
              <p className="text-xs text-muted-foreground">
                These instructions will be appended to the system prompt for this message only.
              </p>
            </div>
          </TabsContent>
        </Tabs>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button onClick={handleSave}>OK</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
