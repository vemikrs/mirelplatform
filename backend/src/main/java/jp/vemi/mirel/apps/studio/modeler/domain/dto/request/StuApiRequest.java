package jp.vemi.mirel.apps.studio.modeler.domain.dto.request;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StuApiRequest {
    private Map<String, Object> content;
}
