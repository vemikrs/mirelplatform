package jp.vemi.mirel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import jp.vemi.framework.util.DatabaseUtil;
import jp.vemi.mirel.foundation.abst.dao.entity.User;
import jp.vemi.mirel.foundation.abst.dao.repository.ApplicationLicenseRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.SystemUserRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.TenantRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.TenantSystemMasterRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.UserRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.UserTenantRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.FeatureFlagRepository;

public class DatabaseUtilVerificationTest {

    private ApplicationContext applicationContext;
    private SystemUserRepository systemUserRepository;
    private UserRepository userRepository;
    private TenantRepository tenantRepository;
    private UserTenantRepository userTenantRepository;
    private ApplicationLicenseRepository applicationLicenseRepository;
    private TenantSystemMasterRepository tenantSystemMasterRepository;
    private PasswordEncoder passwordEncoder;

    private FeatureFlagRepository featureFlagRepository;

    @BeforeEach
    public void setUp() {
        applicationContext = mock(ApplicationContext.class);
        systemUserRepository = mock(SystemUserRepository.class);
        userRepository = mock(UserRepository.class);
        tenantRepository = mock(TenantRepository.class);
        userTenantRepository = mock(UserTenantRepository.class);
        applicationLicenseRepository = mock(ApplicationLicenseRepository.class);
        tenantSystemMasterRepository = mock(TenantSystemMasterRepository.class);
        featureFlagRepository = mock(FeatureFlagRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);

        when(applicationContext.getBean(SystemUserRepository.class)).thenReturn(systemUserRepository);
        when(applicationContext.getBean(UserRepository.class)).thenReturn(userRepository);
        when(applicationContext.getBean(TenantRepository.class)).thenReturn(tenantRepository);
        when(applicationContext.getBean(UserTenantRepository.class)).thenReturn(userTenantRepository);
        when(applicationContext.getBean(ApplicationLicenseRepository.class)).thenReturn(applicationLicenseRepository);
        when(applicationContext.getBean(TenantSystemMasterRepository.class)).thenReturn(tenantSystemMasterRepository);
        when(applicationContext.getBean(FeatureFlagRepository.class)).thenReturn(featureFlagRepository);
        when(applicationContext.getBean(PasswordEncoder.class)).thenReturn(passwordEncoder);

        new DatabaseUtil().setApplicationContext(applicationContext);
    }

    @Test
    public void testInitializeSaasTestData_UpdatesExistingUserWithNullUsername() {
        // Simulate SystemUser exists
        when(systemUserRepository.existsById(any())).thenReturn(true);

        // Simulate Tenant exists
        when(tenantRepository.existsById(anyString())).thenReturn(true);

        // Simulate User exists but has null username
        User existingAdminUser = new User();
        existingAdminUser.setUserId("user-admin-001");
        existingAdminUser.setUsername(null); // The issue we are testing
        existingAdminUser.setEmail(null);

        when(userRepository.findById("user-admin-001")).thenReturn(Optional.of(existingAdminUser));

        // Simulate Regular User exists and is fine (or also broken, let's just focus on
        // admin for now or both)
        User existingRegularUser = new User();
        existingRegularUser.setUserId("user-regular-001");
        existingRegularUser.setUsername("user");
        existingRegularUser.setEmail("user@example.com");
        when(userRepository.findById("user-regular-001")).thenReturn(Optional.of(existingRegularUser));

        // Simulate other entities exist
        when(userTenantRepository.existsById(anyString())).thenReturn(true);
        when(applicationLicenseRepository.existsById(anyString())).thenReturn(true);

        // Run initialization
        DatabaseUtil.initializeSaasTestData();

        // Verify userRepo.save was called for admin user
        // Verify userRepo.save was called at least once
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, org.mockito.Mockito.atLeastOnce()).save(userCaptor.capture());

        // Find the saved user we are interested in
        User savedUser = userCaptor.getAllValues().stream()
                .filter(u -> "user-admin-001".equals(u.getUserId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("User user-admin-001 was not saved"));

        // Assert that username was fixed
        if (!"admin".equals(savedUser.getUsername())) {
            throw new AssertionError("Expected username to be 'admin', but was: " + savedUser.getUsername());
        }
        if (!"admin@example.com".equals(savedUser.getEmail())) {
            throw new AssertionError("Expected email to be 'admin@example.com', but was: " + savedUser.getEmail());
        }
    }
}
