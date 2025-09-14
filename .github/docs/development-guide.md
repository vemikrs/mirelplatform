# Development Guide - ProMarker Platform

## Advanced Development Patterns

This guide covers advanced development patterns, best practices, and architectural considerations for extending and maintaining the ProMarker platform.

## Architecture Deep Dive

### mirelplatform Framework Integration
```java
// Core framework structure
jp.vemi.framework/
├── web/api/           # API base classes and utilities
├── util/              # Common utilities
├── exeption/          # Custom exception handling
└── security/          # Authentication and authorization

// Application-specific modules
jp.vemi.mirel.apps/
├── mste/              # ProMarker (Master Template Engine)
├── selenade/          # Web automation (development)
└── [future modules]   # Extensible architecture
```

### Layered Architecture Pattern
```java
// Controller Layer - HTTP endpoints
@RestController
@RequestMapping("apps/mste/api")
public class ApiController {
    @RequestMapping("/{path}")
    public ResponseEntity<ApiResponse<?>> index(@RequestBody Map<String, Object> request, @PathVariable String path) {
        // Delegate to appropriate API service
        String apiName = path + "Api";
        MsteApi api = apis.get(apiName);
        return new ResponseEntity<>(api.service(request), HttpStatus.OK);
    }
}

// API Layer - Request/response handling
@Service
public class SuggestApi implements MsteApi {
    @Autowired
    protected SuggestService service;
    
    @Override
    public ApiResponse<SuggestResult> service(Map<String, Object> request) {
        // Transform request to domain objects
        // Delegate to service layer
        // Transform response for frontend
    }
}

// Service Layer - Business logic
@Service
@Transactional
public class SuggestServiceImp implements SuggestService {
    // Core business logic implementation
    // Database interactions through repositories
    // Template engine integration
}

// Repository Layer - Data access
@Repository
public interface MsteStencilRepository extends JpaRepository<MsteStencil, String> {
    @Query("SELECT s FROM MsteStencil s WHERE s.stencilCd = :categoryId AND s.itemKind = :itemKind")
    List<MsteStencil> findByStencilCd(@Param("categoryId") String categoryId, @Param("itemKind") String itemKind);
}
```

## Custom API Development

### Creating New API Endpoints

#### Step 1: Define DTOs
```java
// Request DTO
@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class CustomParameter {
    private String parameterName;
    private String parameterValue;
    private List<String> options;
}

// Response DTO
@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class CustomResult {
    private String resultData;
    private List<String> generatedFiles;
    private Map<String, Object> metadata;
}
```

#### Step 2: Implement Service
```java
@Service
@Transactional
public class CustomServiceImp implements CustomService {
    
    @Autowired
    private SomeRepository repository;
    
    @Override
    public ApiResponse<CustomResult> invoke(ApiRequest<CustomParameter> parameter) {
        ApiResponse<CustomResult> response = ApiResponse.<CustomResult>builder().build();
        
        try {
            // Business logic implementation
            CustomResult result = processCustomLogic(parameter.getModel());
            response.setData(result);
            
        } catch (Exception e) {
            response.addError("Processing failed: " + e.getMessage());
            logger.error("Custom service error", e);
        }
        
        return response;
    }
    
    private CustomResult processCustomLogic(CustomParameter param) {
        // Implement custom business logic
        return CustomResult.builder()
            .resultData("processed")
            .build();
    }
}
```

#### Step 3: Create API Wrapper
```java
@Service
public class CustomApi implements MsteApi {
    
    @Autowired
    protected CustomService service;
    
    @Override
    public ApiResponse<?> service(Map<String, Object> request) {
        // Extract and validate request parameters
        Map<String, Object> content = InstanceUtil.forceCast(request.get("content"));
        
        CustomParameter parameter = CustomParameter.builder()
            .parameterName((String) content.get("parameterName"))
            .parameterValue((String) content.get("parameterValue"))
            .build();
            
        // Build API request
        ApiRequest<CustomParameter> apiRequest = ApiRequest.<CustomParameter>builder()
            .model(parameter)
            .build();
            
        // Delegate to service
        return service.invoke(apiRequest);
    }
}
```

#### Step 4: Register with Spring
```java
// Automatic registration through component scanning
// Ensure @Service annotation is present
// Interface naming convention: {FunctionName}Api implements MsteApi
// Spring will automatically register as "{functionName}Api" bean
```

### Template Engine Integration

#### Custom Function Resolvers
```java
@Component
public class CustomFunctionResolver extends FunctionResolverAbstract {
    
    @Override
    public boolean is(FunctionResolverCondition condition) {
        return "customFunction".equals(condition.getFunctionName());
    }
    
    @Override
    public String resolve(FunctionResolverCondition condition) {
        List<Object> args = condition.getArgs();
        
        // Implement custom template function logic
        if (args.size() < 2) {
            throw new IllegalArgumentException("customFunction requires 2 arguments");
        }
        
        String input = args.get(0).toString();
        String format = args.get(1).toString();
        
        // Process and return result
        return processCustomFunction(input, format);
    }
    
    private String processCustomFunction(String input, String format) {
        // Custom processing logic
        return input.toUpperCase() + "_" + format;
    }
}
```

#### Template Integration
```freemarker
<#-- FreeMarker template using custom function -->
package ${packageName};

/**
 * ${customFunction(className, "PROCESSED")} 
 * Generated by ProMarker Platform
 */
public class ${className} {
    
    <#list parameters as param>
    private ${param.type} ${param.name};
    </#list>
    
    // Generated methods...
}
```

### Database Extensions

#### Custom Entity Definition
```java
@Entity
@Table(name = "CUSTOM_TEMPLATE")
@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
public class CustomTemplate {
    
    @Id
    @Column(name = "template_id", length = 100)
    private String templateId;
    
    @Column(name = "template_name", length = 200)
    private String templateName;
    
    @Column(name = "template_category", length = 100)
    private String templateCategory;
    
    @Lob
    @Column(name = "template_content")
    private String templateContent;
    
    @Column(name = "created_date")
    private LocalDateTime createdDate;
    
    @Column(name = "updated_date") 
    private LocalDateTime updatedDate;
}
```

#### Repository Implementation
```java
@Repository
public interface CustomTemplateRepository extends JpaRepository<CustomTemplate, String> {
    
    List<CustomTemplate> findByTemplateCategory(String category);
    
    @Query("SELECT c FROM CustomTemplate c WHERE c.templateName LIKE %:pattern%")
    List<CustomTemplate> findByNamePattern(@Param("pattern") String pattern);
    
    @Modifying
    @Query("UPDATE CustomTemplate c SET c.updatedDate = :date WHERE c.templateId = :id")
    void updateTimestamp(@Param("id") String id, @Param("date") LocalDateTime date);
}
```

## Frontend Extension Patterns

### Custom Vue.js Components

#### Reusable Form Component
```vue
<template>
  <div class="dynamic-form">
    <div v-for="field in fields" :key="field.id" class="form-group">
      <label :for="field.id">{{ field.label }}</label>
      
      <b-form-input 
        v-if="field.type === 'text'"
        :id="field.id"
        v-model="field.value"
        :placeholder="field.placeholder"
        @input="onFieldChange(field)"
      />
      
      <b-form-select 
        v-else-if="field.type === 'select'"
        :id="field.id" 
        v-model="field.value"
        :options="field.options"
        @change="onFieldChange(field)"
      />
      
      <b-form-file 
        v-else-if="field.type === 'file'"
        :id="field.id"
        v-model="field.value"
        @input="onFileChange(field)"
      />
    </div>
  </div>
</template>

<script>
export default {
  name: 'DynamicForm',
  
  props: {
    fields: {
      type: Array,
      required: true,
      validator: (fields) => {
        return fields.every(field => 
          field.id && field.type && field.label
        );
      }
    }
  },
  
  methods: {
    onFieldChange(field) {
      this.$emit('field-changed', {
        fieldId: field.id,
        value: field.value,
        field: field
      });
    },
    
    onFileChange(field) {
      // Handle file upload logic
      this.$emit('file-selected', {
        fieldId: field.id,
        file: field.value,
        field: field
      });
    }
  }
}
</script>
```

#### API Service Abstraction
```javascript
// services/api.js
class ApiService {
  constructor() {
    this.baseURL = '/mapi/apps/mste/api';
  }
  
  async suggest(parameters) {
    const response = await axios.post(`${this.baseURL}/suggest`, {
      content: parameters
    });
    
    // Handle ModelWrapper structure
    if (response.data.data && response.data.data.model) {
      return {
        success: true,
        data: response.data.data.model,
        errors: response.data.errors || []
      };
    }
    
    return {
      success: false,
      data: null,
      errors: response.data.errors || ['Unknown error occurred']
    };
  }
  
  async generate(parameters) {
    const response = await axios.post(`${this.baseURL}/generate`, {
      content: parameters
    });
    
    return {
      success: !response.data.errors?.length,
      data: response.data.data,
      errors: response.data.errors || []
    };
  }
  
  async customEndpoint(parameters) {
    // Template for new endpoints
    const response = await axios.post(`${this.baseURL}/custom`, {
      content: parameters
    });
    
    return this.handleResponse(response);
  }
  
  handleResponse(response) {
    return {
      success: !response.data.errors?.length,
      data: response.data.data || response.data.data?.model,
      errors: response.data.errors || []
    };
  }
}

export default new ApiService();
```

### State Management Patterns

#### Composition API Pattern (Vue 3 Ready)
```javascript
// composables/useStencilManagement.js
import { ref, reactive, computed } from 'vue';
import ApiService from '@/services/api';

export function useStencilManagement() {
  // Reactive state
  const processing = ref(false);
  const stencilData = reactive({
    categories: { items: [], selected: '' },
    stencils: { items: [], selected: '' },
    serials: { items: [], selected: '' }
  });
  const parameters = ref([]);
  
  // Computed properties
  const isValid = computed(() => {
    return stencilData.categories.selected && 
           stencilData.stencils.selected && 
           stencilData.serials.selected;
  });
  
  // Methods
  const loadStencils = async () => {
    processing.value = true;
    
    try {
      const result = await ApiService.suggest({
        stencilCategoy: stencilData.categories.selected || '*',
        stencilCanonicalName: stencilData.stencils.selected || '*',
        serialNo: stencilData.serials.selected || '*'
      });
      
      if (result.success) {
        updateStencilData(result.data);
      }
      
      return result;
    } finally {
      processing.value = false;
    }
  };
  
  const updateStencilData = (data) => {
    if (data.fltStrStencilCategory) {
      Object.assign(stencilData.categories, data.fltStrStencilCategory);
    }
    if (data.fltStrStencilCd) {
      Object.assign(stencilData.stencils, data.fltStrStencilCd);
    }
    if (data.fltStrSerialNo) {
      Object.assign(stencilData.serials, data.fltStrSerialNo);
    }
    if (data.params && data.params.childs) {
      parameters.value = data.params.childs;
    }
  };
  
  const generateCode = async () => {
    if (!isValid.value) {
      throw new Error('Invalid selection state');
    }
    
    const params = {
      stencilCategoy: stencilData.categories.selected,
      stencilCanonicalName: stencilData.stencils.selected,
      serialNo: stencilData.serials.selected
    };
    
    // Add dynamic parameters
    parameters.value.forEach(param => {
      params[param.id] = param.value;
    });
    
    return await ApiService.generate(params);
  };
  
  return {
    // State
    processing: readonly(processing),
    stencilData: readonly(stencilData),
    parameters: readonly(parameters),
    
    // Computed
    isValid,
    
    // Methods
    loadStencils,
    generateCode
  };
}
```

## Testing Strategies

### Backend Unit Testing
```java
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
class SuggestServiceTest {
    
    @Autowired
    private SuggestService suggestService;
    
    @MockBean
    private MsteStencilRepository stencilRepository;
    
    @Test
    void shouldReturnStencilCategories() {
        // Given
        List<MsteStencil> mockStencils = Arrays.asList(
            createMockStencil("/samples", "Sample Stencils"),
            createMockStencil("/custom", "Custom Templates")
        );
        
        when(stencilRepository.findByStencilCd("", "CATEGORY"))
            .thenReturn(mockStencils);
        
        // When
        ApiRequest<SuggestParameter> request = createSuggestRequest("*", "*", "*");
        ApiResponse<SuggestResult> response = suggestService.invoke(request);
        
        // Then
        assertThat(response.getData()).isNotNull();
        assertThat(response.getErrors()).isEmpty();
        
        // Verify ModelWrapper structure
        if (response.getData() instanceof ModelWrapper) {
            SuggestResult model = ((ModelWrapper) response.getData()).model;
            assertThat(model.fltStrStencilCategory.items).hasSize(2);
        }
    }
    
    private MsteStencil createMockStencil(String id, String name) {
        MsteStencil stencil = new MsteStencil();
        stencil.setStencilCd(id);
        stencil.setStencilName(name);
        stencil.setItemKind("CATEGORY");
        return stencil;
    }
    
    private ApiRequest<SuggestParameter> createSuggestRequest(String category, String stencil, String serial) {
        SuggestParameter param = new SuggestParameter();
        param.stencilCategory = category;
        param.stencilCd = stencil;
        param.serialNo = serial;
        
        return ApiRequest.<SuggestParameter>builder()
            .model(param)
            .build();
    }
}
```

### Frontend Component Testing
```javascript
// test/components/ProMarkerForm.spec.js
import { shallowMount } from '@vue/test-utils';
import ProMarkerForm from '@/pages/mste/index.vue';
import axios from 'axios';

// Mock axios
jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

describe('ProMarkerForm', () => {
  let wrapper;
  
  beforeEach(() => {
    wrapper = shallowMount(ProMarkerForm, {
      stubs: ['b-form-select', 'b-form-input', 'b-button']
    });
  });
  
  afterEach(() => {
    wrapper.destroy();
  });
  
  it('should initialize with correct default state', () => {
    expect(wrapper.vm.processing).toBe(false);
    expect(wrapper.vm.cateogryNoSelected).toBe(true);
    expect(wrapper.vm.stencilNoSelected).toBe(true);
    expect(wrapper.vm.serialNoNoSelected).toBe(true);
  });
  
  it('should handle API response correctly', async () => {
    // Mock API response with ModelWrapper structure
    const mockResponse = {
      data: {
        data: {
          model: {
            fltStrStencilCategory: {
              items: [{ value: '/samples', text: 'Sample Stencils' }],
              selected: '/samples'
            },
            fltStrStencilCd: {
              items: [{ value: '/samples/hello-world', text: 'Hello World' }],
              selected: '/samples/hello-world'
            },
            params: {
              childs: [
                { id: 'message', value: 'Hello', name: 'Message' }
              ]
            }
          }
        },
        errors: []
      }
    };
    
    mockedAxios.post.mockResolvedValue(mockResponse);
    
    // Execute refresh
    await wrapper.vm.refresh();
    
    // Verify state updates
    expect(wrapper.vm.fltStrStencilCategory.items).toHaveLength(1);
    expect(wrapper.vm.fltStrStencilCategory.selected).toBe('/samples');
    expect(wrapper.vm.eparams).toHaveLength(1);
    expect(wrapper.vm.eparams[0].id).toBe('message');
  });
  
  it('should handle category selection workflow', async () => {
    // Setup mock
    const mockResponse = { /* ... mock response ... */ };
    mockedAxios.post.mockResolvedValue(mockResponse);
    
    // Trigger category selection
    await wrapper.vm.stencilCategorySelected();
    
    // Verify state changes
    expect(wrapper.vm.fltStrStencilCd.selected).toBe('*');
    expect(wrapper.vm.cateogryNoSelected).toBe(false);
    expect(wrapper.vm.stencilNoSelected).toBe(true);
    
    // Verify API call
    expect(mockedAxios.post).toHaveBeenCalledWith(
      '/mapi/apps/mste/api/suggest',
      expect.objectContaining({
        content: expect.any(Object)
      })
    );
  });
});
```

### Integration Testing
```javascript
// test/integration/api.spec.js
import axios from 'axios';

const API_BASE = process.env.API_BASE || 'http://localhost:3000/mapi/apps/mste/api';

describe('ProMarker API Integration', () => {
  it('should complete full workflow', async () => {
    // Test suggest API
    const suggestResponse = await axios.post(`${API_BASE}/suggest`, {
      content: {
        stencilCategoy: '*',
        stencilCanonicalName: '*',
        serialNo: '*'
      }
    });
    
    expect(suggestResponse.status).toBe(200);
    expect(suggestResponse.data.data.model).toBeDefined();
    
    const model = suggestResponse.data.data.model;
    expect(model.fltStrStencilCategory.items.length).toBeGreaterThan(0);
    
    // Test generate API with selected values
    if (model.fltStrStencilCd.items.length > 0) {
      const generateResponse = await axios.post(`${API_BASE}/generate`, {
        content: {
          stencilCategoy: model.fltStrStencilCategory.selected,
          stencilCanonicalName: model.fltStrStencilCd.selected,
          serialNo: model.fltStrSerialNo.selected,
          message: 'Integration Test Message'
        }
      });
      
      expect(generateResponse.status).toBe(200);
      expect(generateResponse.data.data.files).toBeDefined();
    }
  });
});
```

## Performance Optimization

### Backend Optimization

#### Database Query Optimization
```java
// Use proper indexing
@Entity
@Table(name = "MSTE_STENCIL", 
       indexes = {
           @Index(name = "idx_stencil_category", columnList = "stencil_cd, item_kind"),
           @Index(name = "idx_stencil_update", columnList = "last_update")
       })
public class MsteStencil {
    // Entity definition...
}

// Efficient query patterns
@Repository
public interface MsteStencilRepository extends JpaRepository<MsteStencil, String> {
    
    // Use specific queries instead of findAll()
    @Query("SELECT s FROM MsteStencil s WHERE s.stencilCd = :categoryId AND s.itemKind = :itemKind ORDER BY s.stencilName")
    List<MsteStencil> findByCategoryAndKind(@Param("categoryId") String categoryId, @Param("itemKind") String itemKind);
    
    // Use projection for limited data needs
    @Query("SELECT s.stencilCd, s.stencilName FROM MsteStencil s WHERE s.itemKind = 'CATEGORY'")
    List<Object[]> findCategorySummary();
}
```

#### Caching Implementation
```java
@Service
@Transactional
public class SuggestServiceImp implements SuggestService {
    
    @Cacheable(value = "stencilCategories", unless = "#result.isEmpty()")
    public ValueTextItems getStencilCategories() {
        List<MsteStencil> stencils = stencilRepository.findByCategoryAndKind("", Const.STENCIL_ITEM_KIND_CATEGORY);
        return new ValueTextItems(convertStencilToValueTexts(stencils), "");
    }
    
    @CacheEvict(value = "stencilCategories", allEntries = true)
    public void clearStencilCache() {
        // Called when stencils are updated
    }
}
```

### Frontend Optimization

#### Lazy Loading Components
```javascript
// Lazy load heavy components
export default {
  components: {
    // Lazy loaded dialog
    BvDownloadDialog: () => import('~/components/dialog/BvDownloadDialog.vue'),
    
    // Conditional loading
    AgGrid: () => {
      if (process.client) {
        return import('ag-grid-vue');
      }
      return Promise.resolve(null);
    }
  }
}
```

#### API Request Optimization
```javascript
// Debounce API calls
data() {
  return {
    searchTimeout: null
  };
},

methods: {
  onSearchInput(query) {
    clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => {
      this.performSearch(query);
    }, 300);
  },
  
  // Cache API responses
  async refresh() {
    const cacheKey = this.createCacheKey();
    
    if (this.responseCache.has(cacheKey)) {
      this.updateUIFromCache(this.responseCache.get(cacheKey));
      return;
    }
    
    const response = await this.callAPI();
    this.responseCache.set(cacheKey, response);
    this.updateUI(response);
  }
}
```

## Security Best Practices

### Backend Security
```java
// Input validation
@Service
public class ValidationService {
    
    public void validateStencilParameter(String parameter) {
        if (parameter == null || parameter.trim().isEmpty()) {
            throw new ValidationException("Parameter cannot be empty");
        }
        
        // Prevent path traversal
        if (parameter.contains("..") || parameter.contains("/")) {
            throw new ValidationException("Invalid parameter format");
        }
        
        // Limit length
        if (parameter.length() > 100) {
            throw new ValidationException("Parameter too long");
        }
    }
}

// SQL injection prevention (already handled by JPA, but for custom queries)
@Repository
public class CustomRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    public List<Object[]> customQuery(String userInput) {
        // Use parameterized queries
        String sql = "SELECT * FROM custom_table WHERE name = :name";
        return entityManager.createNativeQuery(sql)
            .setParameter("name", userInput)
            .getResultList();
    }
}
```

### Frontend Security
```javascript
// XSS prevention (Vue.js handles this automatically, but for dynamic content)
methods: {
  sanitizeInput(input) {
    return input.replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '');
  },
  
  // Validate file uploads
  validateFileUpload(file) {
    const allowedTypes = ['application/json', 'text/yaml', 'text/plain'];
    const maxSize = 10 * 1024 * 1024; // 10MB
    
    if (!allowedTypes.includes(file.type)) {
      throw new Error('File type not allowed');
    }
    
    if (file.size > maxSize) {
      throw new Error('File too large');
    }
    
    return true;
  }
}
```

## Deployment Considerations

### Production Configuration
```yaml
# application-prod.yml
spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:mysql://prod-db:3306/promarker
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    
server:
  port: 8080
  tomcat:
    max-threads: 200
    accept-count: 100
    
logging:
  level:
    jp.vemi.mirel: INFO
    org.springframework.web: WARN
  file:
    name: /var/log/promarker/application.log
    max-size: 100MB
    max-history: 30
```

### Monitoring and Observability
```java
// Custom health checks
@Component
public class StencilHealthIndicator implements HealthIndicator {
    
    @Autowired
    private MsteStencilRepository stencilRepository;
    
    @Override
    public Health health() {
        try {
            long count = stencilRepository.count();
            
            if (count > 0) {
                return Health.up()
                    .withDetail("stencilCount", count)
                    .build();
            } else {
                return Health.down()
                    .withDetail("error", "No stencils found")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

This comprehensive guide provides the foundation for advanced ProMarker development. Refer to the specific documentation sections for detailed implementation guidance.