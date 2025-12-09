import { useState, useEffect } from 'react';
import { 
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter,
  Button, Input, Label, Switch,
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
  Textarea
} from '@mirel/ui';

import { type MessageConfig } from '@/lib/api/mira';
import { AiPresetSuggestion } from './AiPresetSuggestion';
import { History, Sparkles, Layers, Terminal } from 'lucide-react';

import { useMiraStore } from '@/stores/miraStore';

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
  // Global Streaming Setting
  const useStream = useMiraStore((state) => state.useStream);
  const setUseStream = useMiraStore((state) => state.setUseStream);
  
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
          <DialogTitle>Context & Settings</DialogTitle>
          <AiPresetSuggestion 
            messageContent={messageContent} 
            onApply={handleApplySuggestion} 
            className="mt-2"
          />
        </DialogHeader>

        <div className="flex-1 overflow-y-auto max-h-[60vh] p-1 space-y-6">
          {/* Section: Feature Flags / Preferences */}
          <div className="space-y-3 p-4 border rounded-lg bg-muted/20">
             <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                   <Sparkles className="w-4 h-4 text-primary" />
                   <Label className="font-semibold">Realtime Streaming</Label>
                </div>
                <Switch 
                   checked={useStream}
                   onCheckedChange={setUseStream}
                />
             </div>
             <p className="text-xs text-muted-foreground ml-6">
                Enable AI response streaming (Server-Sent Events). Toggle off if you experience network issues.
             </p>
          </div>

          <div className="grid md:grid-cols-2 gap-6">
            {/* Left Column: Context Controls */}
            <div className="space-y-6">
               {/* Section: History Scope */}
               <div className="space-y-3">
                  <div className="flex items-center gap-2 pb-1 border-b">
                     <History className="w-4 h-4" />
                     <h3 className="text-sm font-semibold">History Scope</h3>
                  </div>
                  
                   <div className="space-y-3">
                     <div className="flex flex-col gap-2">
                        <Label>Conversation Scope</Label>
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
               </div>

               {/* Section: Temporary Context */}
                <div className="space-y-3">
                  <div className="flex items-center gap-2 pb-1 border-b">
                     <Terminal className="w-4 h-4" />
                     <h3 className="text-sm font-semibold">Temporary Context</h3>
                  </div>
                  <div className="space-y-2">
                    <Textarea 
                      placeholder="Add temporary instructions for this message..."
                      className="min-h-[120px]"
                      value={localConfig.temporaryContext || ''}
                      onChange={(e) => updateConfig({ temporaryContext: e.target.value })}
                    />
                    <p className="text-xs text-muted-foreground">
                      Attached to system prompt for this turn only.
                    </p>
                  </div>
                </div>
            </div>

            {/* Right Column: Context Layers */}
            <div className="space-y-6">
               <div className="space-y-3">
                  <div className="flex items-center gap-2 pb-1 border-b">
                     <Layers className="w-4 h-4" />
                     <h3 className="text-sm font-semibold">Context Layers</h3>
                  </div>
                  
                  <div className="space-y-3">
                    <p className="text-xs text-muted-foreground">
                      Enable/Disable specific context injections.
                    </p>
                    <div className="space-y-2">
                      {['system', 'terminology', 'style', 'workflow'].map(key => {
                        const override = localConfig.contextOverrides?.[key] || { enabled: true, priority: 0 };
                        return (
                          <div key={key} className="flex items-center justify-between p-2 rounded border bg-card">
                            <Label htmlFor={`ctx-${key}`} className="capitalize cursor-pointer flex-1">{key}</Label>
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
                        );
                      })}
                    </div>
                  </div>
               </div>
            </div>
          </div>


        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button onClick={handleSave}>OK</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
