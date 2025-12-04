package jp.vemi.mirel.apps.studio.modeler.domain.dto.response;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StuApiResponse {
    private Map<String, Object> data;
    private List<String> messages;
    private List<String> errors;
    private String errorCode;
}
