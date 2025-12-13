package jp.vemi.mirel.apps.mira.infrastructure.ai.tool;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * Factory for creating Tavily search tools.
 */
@Component
public class TavilySearchToolFactory {

    /**
     * Create a new Tavily search tool instance.
     * 
     * @param apiKey
     *            The API key for Tavily.
     * @return A ToolCallback instance.
     */
    public ToolCallback createInternal(String apiKey) {
        return new WebSearchTools(new TavilySearchProvider(apiKey));
    }

    // Using 'createInternal' merely to be explicit, or just 'create'
    public ToolCallback create(String apiKey) {
        return new WebSearchTools(new TavilySearchProvider(apiKey));
    }
}
