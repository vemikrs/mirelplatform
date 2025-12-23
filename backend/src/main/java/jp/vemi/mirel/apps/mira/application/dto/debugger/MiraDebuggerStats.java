/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.application.dto.debugger;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiraDebuggerStats {

    private long totalDocumentCount;
    private Map<String, Long> countByScope;
    private Map<String, Long> countByTenant; // Top 10 tenants
    private LocalDateTime lastIngestedAt;
    private boolean isIndexEmpty;
}
