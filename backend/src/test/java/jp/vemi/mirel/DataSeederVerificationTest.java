package jp.vemi.mirel;

import jp.vemi.framework.util.DataLoader;
import jp.vemi.framework.util.DataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.mockito.Mockito.*;

public class DataSeederVerificationTest {

    private ApplicationContext applicationContext;
    private DataLoader dataLoader;

    @BeforeEach
    public void setUp() {
        applicationContext = mock(ApplicationContext.class);
        dataLoader = mock(DataLoader.class);

        when(applicationContext.getBean(DataLoader.class)).thenReturn(dataLoader);

        new DataSeeder().setApplicationContext(applicationContext);
    }

    @Test
    public void testInitializeSaasTestData_CallsLoadSystemData() {
        // Run initialization
        DataSeeder.initializeSaasTestData();

        // Verify dataLoader.loadSystemData() was called
        verify(dataLoader, times(1)).loadSystemData();
    }

    @Test
    public void testInitializeSchemaSampleData_CallsLoadSampleData() {
        // Run initialization
        DataSeeder.initializeSchemaSampleData();

        // Verify dataLoader.loadSampleData() was called
        verify(dataLoader, times(1)).loadSampleData();
    }
}
