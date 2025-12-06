package jp.vemi.mirel.foundation.menu.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jp.vemi.mirel.foundation.menu.entity.MenuEntity;

@Repository
public interface MenuRepository extends JpaRepository<MenuEntity, String> {
    List<MenuEntity> findAllByOrderBySortOrderAsc();

    List<MenuEntity> findByParentIdOrderBySortOrderAsc(String parentId);
}
