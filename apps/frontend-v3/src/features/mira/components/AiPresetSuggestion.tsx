import { useState, useEffect } from 'react';
import { Button, cn } from '@mirel/ui';
import { Sparkles, Loader2 } from 'lucide-react';
import { suggestConfig, type MessageConfig } from '@/lib/api/mira';

interface AiPresetSuggestionProps {
  messageContent: string;
  onApply: (config: MessageConfig) => void;
  className?: string;
}

export function AiPresetSuggestion({
  messageContent,
  onApply,
  className,
}: AiPresetSuggestionProps) {
  const [loading, setLoading] = useState(false);
  const [suggestion, setSuggestion] = useState<MessageConfig | null>(null);

  useEffect(() => {
    if (!messageContent || messageContent.length < 5) {
      setSuggestion(null);
      return;
    }

    const timer = setTimeout(async () => {
      setLoading(true);
      try {
        const result = await suggestConfig({ messageContent });
        // Only show if there's a meaningful suggestion (e.g., temporaryContext is not empty)
        if (result.temporaryContext || (result.historyScope && result.historyScope !== 'auto')) {
          setSuggestion(result);
        } else {
          setSuggestion(null);
        }
      } catch (error) {
        console.error('Failed to get suggestion', error);
      } finally {
        setLoading(false);
      }
    }, 1000); // Debounce 1s

    return () => clearTimeout(timer);
  }, [messageContent]);

  if (!suggestion) return null;

  return (
    <div className={cn("flex items-center gap-2 p-2 bg-purple-50 rounded-md border border-purple-100 dark:bg-purple-900/20 dark:border-purple-800", className)}>
      <Sparkles className="w-4 h-4 text-purple-600 dark:text-purple-400" />
      <span className="text-xs text-purple-700 dark:text-purple-300 flex-1">
        AI Suggestion available for this context.
      </span>
      <Button
        variant="ghost"
        size="sm"
        className="h-6 text-xs text-purple-700 hover:text-purple-800 hover:bg-purple-200/50 dark:text-purple-300 dark:hover:bg-purple-800/50"
        onClick={() => onApply(suggestion)}
        disabled={loading}
      >
        {loading ? <Loader2 className="w-3 h-3 animate-spin" /> : "Apply"}
      </Button>
    </div>
  );
}
