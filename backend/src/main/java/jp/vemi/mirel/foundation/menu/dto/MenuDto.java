package jp.vemi.mirel.foundation.menu.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuDto {
    private String id;
    private String label;
    private String path;
    private String icon;
    private String parentId;
    private Integer sortOrder;
    private String requiredPermission;
    private String description;
    private List<MenuDto> children;
}
