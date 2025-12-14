import { useCallback, useRef } from 'react';
import { useMiraStore } from '@/stores/miraStore';
import { type ChatRequest } from '@/lib/api/mira';

interface StreamEvent {
  type: 'DELTA' | 'STATUS' | 'ERROR' | 'DONE';
  content?: string;
  model?: string;
  conversationId?: string;
  error?: string;
}

// Timeout for stream inactivity (120 seconds for o1/reasoning models)
const READ_TIMEOUT_MS = 120000;

export function useMiraChatStream() {
  const {
    activeConversationId,
    startConversation,
    addUserMessage,
    startStreamingMessage,
    appendStreamingChunk,
    updateStreamingStatus,
    completeStreamingMessage,
    failStreamingMessage,
    setError,
    setLoading,
  } = useMiraStore();

  const abortControllerRef = useRef<AbortController | null>(null);

  const sendMessageStream = useCallback(async (
    content: string,
    options?: {
        mode?: any; // MiraMode
        context?: any;
        messageConfig?: any;
        forceProvider?: string;
        forceModel?: string; // Phase 4: Model selection
        webSearchEnabled?: boolean;
    }
  ) => {
    // 1. Setup Conversation
    let conversationId = activeConversationId;
    if (!conversationId) {
      conversationId = startConversation(options?.mode, options?.context);
    }
    
    // 2. Add User Message
    addUserMessage(conversationId, content);
    
    // 3. Prepare Streaming Assistant Message
    const messageId = crypto.randomUUID();
    startStreamingMessage(conversationId, messageId);
    
    // 4. Build Request
    // 4. Build Request
    // Ensure we send the correct conversationId. 
    // Previously: conversationId !== activeConversationId ? undefined : conversationId
    // exact fix: always send 'conversationId' which is the resolved target ID.
    const request: ChatRequest = {
      conversationId: conversationId,
      mode: options?.mode,
      context: {
        ...options?.context,
        messageConfig: options?.messageConfig,
      },
      message: { content },
      forceProvider: options?.forceProvider,
      forceModel: options?.forceModel,
      webSearchEnabled: options?.webSearchEnabled,
    };

    if (import.meta.env.DEV) {
        console.log('[Stream Request]', { url: '/mapi/apps/mira/api/stream/chat', request });
    }


    // 5. Start Stream
    if (abortControllerRef.current) {
        abortControllerRef.current.abort();
    }
    const abortController = new AbortController();
    abortControllerRef.current = abortController;

    try {
        // Use /mapi prefix to route through Vite proxy to backend
        const response = await fetch('/mapi/apps/mira/api/stream/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ model: request }), // Wrapper to match createApiRequest structure
            signal: abortController.signal,
        });

        if (!response.ok) {
            throw new Error(`Stream Error: ${response.status} ${response.statusText}`);
        }
        
        if (!response.body) {
            throw new Error('No response body');
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        
        // Watchdog timer for inactivity
        let watchdogTimer: ReturnType<typeof setTimeout> | null = null;
        
        const resetWatchdog = () => {
            if (watchdogTimer) clearTimeout(watchdogTimer);
            watchdogTimer = setTimeout(() => {
                console.warn(`[Stream] Timeout after ${READ_TIMEOUT_MS}ms inactivity`);
                if (abortControllerRef.current) {
                   abortControllerRef.current.abort();
                }
                // We can't easily throw into the reader loop, but aborting will cause the reader to throw or return done.
                // Actually reader.read() will likely throw AbortError if aborted.
            }, READ_TIMEOUT_MS);
        };

        resetWatchdog();

        while (true) {

                const { done, value } = await reader.read();
                
                // Clear watchdog on every read completion (chunk received)
                resetWatchdog();

                if (done) break;
            
                buffer += decoder.decode(value, { stream: true });
            
            // Process lines (SSE format: data: {...}\n\n)
            const lines = buffer.split('\n');
            // Keep the last partial line in buffer
            buffer = lines.pop() || '';

            for (const line of lines) {
                if (line.startsWith('data:')) {
                    const dataStr = line.replace('data:', '').trim();
                    if (!dataStr) continue;
                    
                    try {
                        const event: StreamEvent = JSON.parse(dataStr);
                        
                        switch (event.type) {
                            case 'DELTA':
                                if (event.content) {
                                    appendStreamingChunk(conversationId, messageId, event.content);
                                }
                                break;
                            case 'STATUS':
                                if (event.content) {
                                  updateStreamingStatus(conversationId, messageId, event.content);
                                }
                                break;
                            case 'ERROR':
                                failStreamingMessage(conversationId, messageId, event.error || 'Unknown stream error');
                                break;
                            case 'DONE':
                                completeStreamingMessage(conversationId, messageId, undefined, { 
                                    model: event.model 
                                });
                                break;
                        }
                    } catch (e) {
                        console.warn('Failed to parse SSE event:', dataStr, e);
                    }
                }
            }
        }
    } catch (error: any) {
        if (error.name === 'AbortError') {
             console.log('Stream aborted');
             // Consider if it was our watchdog that aborted it?
             // Not easy to distinguish without extra state, but generally if aborted by user we know.
             // If we want to show specific error for Timeout, we could set a flag in the timeout callback.
        } else {
            console.error('Stream error:', error);
            
            // User-friendly error message for timeouts and technical errors
            let userMessage = error.message;
            const techErrors = [
                'StatusRuntimeException',
                'UNAVAILABLE',
                'Connection reset',
                '503',
                '500',
                'io exception',
                'Failed to fetch',
                'NetworkError'
            ];

            if (error.name === 'TimeoutError' || (error.message && error.message.includes('Timeout'))) {
                userMessage = 'AIの応答がタイムアウトしました。思考時間の長いモデルを使用している可能性があります。';
            } else if (techErrors.some(te => error.message && error.message.includes(te))) {
                userMessage = 'AIサービスとの一時的な通信エラーが発生しました。しばらく待ってから再試行してください。';
            }
            
            failStreamingMessage(conversationId, messageId, userMessage);
            setError(userMessage);
        }
    } finally {
        if (abortControllerRef.current === abortController) {
             abortControllerRef.current = null;
        }
        setLoading(false);
    }

  }, [activeConversationId, startConversation, addUserMessage, startStreamingMessage, appendStreamingChunk, updateStreamingStatus, completeStreamingMessage, failStreamingMessage, setError, setLoading]);

  const abortStream = useCallback(() => {
      if (abortControllerRef.current) {
          abortControllerRef.current.abort();
          abortControllerRef.current = null;
          setLoading(false);
      }
  }, [setLoading]);

  return {
    sendMessageStream,
    abortStream,
  };
}
