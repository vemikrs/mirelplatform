package jp.vemi.framework.util;

import java.util.Optional;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jp.vemi.mirel.foundation.abst.dao.entity.TenantSystemMaster;
import jp.vemi.mirel.foundation.abst.dao.repository.TenantSystemMasterRepository;

@Component("databaseUtil")
@Order(10)
public class DatabaseUtil implements ApplicationContextAware {

  private static ApplicationContext context;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    context = applicationContext;
  }

  private static TenantSystemMasterRepository getTenantSystemMasterRepository() {
    return context.getBean(TenantSystemMasterRepository.class);
  }

  public static void initializeDefaultTenant() {
    String defaultTenantId = "default";
    String jwkSetUriKey = "jwkSetUri";
    String defaultJwkSetUri = "https://auth.vemi.jp/jwk"; // デフォルトのJWK Set URI

    TenantSystemMasterRepository tenantSystemMasterRepository = getTenantSystemMasterRepository();
    Optional<TenantSystemMaster> tenant = tenantSystemMasterRepository.findByTenantIdAndKey(defaultTenantId,
        jwkSetUriKey);
    if (!tenant.isPresent()) {
      TenantSystemMaster newTenant = new TenantSystemMaster();
      newTenant.setTenantId(defaultTenantId);
      newTenant.setKey(jwkSetUriKey);
      newTenant.setValue(defaultJwkSetUri);
      tenantSystemMasterRepository.save(newTenant);
    }
  }

}
