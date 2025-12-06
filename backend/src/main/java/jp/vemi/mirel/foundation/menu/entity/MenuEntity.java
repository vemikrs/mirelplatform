package jp.vemi.mirel.foundation.menu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mir_menus")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuEntity {

    @Id
    @Column(name = "menu_id", nullable = false)
    private String menuId;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "path")
    private String path;

    @Column(name = "icon")
    private String icon;

    @Column(name = "parent_id")
    private String parentId;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "required_permission")
    private String requiredPermission;

    @Column(name = "description")
    private String description;
}
