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

        while (true) {
            const { done, value } = await reader.read();
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
        } else {
            console.error('Stream error:', error);
            failStreamingMessage(conversationId, messageId, error.message);
            setError(error.message);
        }
    } finally {
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
