import {
  Label,
  Slider,
  Switch,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Input,
  Separator,
  Textarea
} from '@mirel/ui';
import { Settings2, Database, Sparkles } from 'lucide-react';

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

interface Settings {
  model: string;
  temperature: number;
  maxTokens: number;
  topP: number;
  topK: number;
  systemPrompt: string;
  ragEnabled: boolean;
  ragScope: string;
  ragTopK: number;
  // リランカー設定
  rerankerEnabled: boolean;
  rerankerTopN: number;
}

interface PlaygroundConfigPanelProps {
  config: Config | null;
  settings: Settings;
  onSettingsChange: (key: keyof Settings, value: any) => void;
}

export function PlaygroundConfigPanel({ config, settings, onSettingsChange }: PlaygroundConfigPanelProps) {
  return (
    <div className="w-80 border-r bg-muted/10 flex flex-col min-w-[320px] overflow-y-auto p-4 space-y-6">
      
      <div className="space-y-4">
        <div className="flex items-center gap-2 font-semibold text-sm">
          <Settings2 className="w-4 h-4" />
          Model Parameters
        </div>
        
        <div className="space-y-2">
          <Label>Model</Label>
          <Select 
            value={settings.model} 
            onValueChange={(val) => onSettingsChange('model', val)}
            disabled={!config}
          >
            <SelectTrigger>
              <SelectValue placeholder="Select model" />
            </SelectTrigger>
            <SelectContent>
              {config?.models.map(model => (
                 <SelectItem key={model.id} value={model.id}>{model.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-4 pt-2">
          <div className="space-y-2">
            <div className="flex justify-between">
              <Label>Temperature</Label>
              <span className="text-xs text-muted-foreground">{settings.temperature}</span>
            </div>
            <Slider 
              value={[settings.temperature]} 
              onValueChange={([v]) => { if (v !== undefined) onSettingsChange('temperature', v); }} 
              max={2} step={0.1} 
            />
          </div>

          <div className="space-y-2">
            <div className="flex justify-between">
              <Label>Top P</Label>
              <span className="text-xs text-muted-foreground">{settings.topP}</span>
            </div>
            <Slider 
              value={[settings.topP]} 
              onValueChange={([v]) => { if (v !== undefined) onSettingsChange('topP', v); }} 
              max={1} step={0.05} 
            />
          </div>

          <div className="space-y-2">
            <div className="flex justify-between">
              <Label>Top K</Label>
              <span className="text-xs text-muted-foreground">{settings.topK}</span>
            </div>
             <Input 
                type="number" 
                value={settings.topK} 
                onChange={e => onSettingsChange('topK', Number(e.target.value))} 
                className="h-8"
             />
          </div>
          
           <div className="space-y-2">
            <div className="flex justify-between">
              <Label>Max Tokens</Label>
              <span className="text-xs text-muted-foreground">{settings.maxTokens}</span>
            </div>
             <Input 
                type="number" 
                value={settings.maxTokens} 
                onChange={e => onSettingsChange('maxTokens', Number(e.target.value))} 
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
            <Switch 
              id="rag-mode" 
              checked={settings.ragEnabled} 
              onCheckedChange={(val) => onSettingsChange('ragEnabled', val)} 
            />
         </div>

         {settings.ragEnabled && (
            <div className="space-y-3 pl-2 border-l-2 border-muted animate-in fade-in slide-in-from-top-2">
               <div className="space-y-2">
                  <Label>Scope</Label>
                  <Select 
                    value={settings.ragScope} 
                    onValueChange={(val) => onSettingsChange('ragScope', val)}
                  >
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
                  <Input 
                    type="number" 
                    value={settings.ragTopK} 
                    onChange={e => onSettingsChange('ragTopK', Number(e.target.value))} 
                    className="h-8" 
                  />
               </div>

               {/* リランカー設定（RAG有効時のみ表示） */}
               <div className="pt-2 space-y-3">
                  <div className="flex items-center gap-2 text-xs font-medium text-muted-foreground">
                     <Sparkles className="w-3 h-3" />
                     Reranker
                  </div>
                  <div className="flex items-center justify-between">
                     <Label htmlFor="reranker-mode" className="cursor-pointer text-sm">Enable Reranking</Label>
                     <Switch 
                       id="reranker-mode" 
                       checked={settings.rerankerEnabled} 
                       onCheckedChange={(val) => onSettingsChange('rerankerEnabled', val)} 
                     />
                  </div>
                  {settings.rerankerEnabled && (
                     <div className="space-y-2">
                        <div className="flex justify-between">
                           <Label>Top N</Label>
                           <span className="text-xs text-muted-foreground">{settings.rerankerTopN}</span>
                        </div>
                        <Slider 
                          value={[settings.rerankerTopN]} 
                          onValueChange={([v]) => { if (v !== undefined) onSettingsChange('rerankerTopN', v); }} 
                          min={1} max={20} step={1} 
                        />
                     </div>
                  )}
               </div>
            </div>
         )}
      </div>

      <Separator />
      
       <div className="space-y-2 flex-1 flex flex-col">
          <Label>System Instruction</Label>
          <Textarea 
            value={settings.systemPrompt}
            onChange={e => onSettingsChange('systemPrompt', e.target.value)}
            className="flex-1 min-h-[100px] text-xs resize-none font-mono"
          />
       </div>

    </div>
  );
}
