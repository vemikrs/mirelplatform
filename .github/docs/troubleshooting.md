# Troubleshooting Guide - ProMarker Platform

## Common Issues and Solutions

### Development Environment Issues

#### 1. Services Not Starting
**Problem**: `./start-services.sh` fails or services don't respond

**Solutions**:
```bash
# Check if ports are already in use
lsof -i :3000  # Backend port
lsof -i :8080  # Frontend port

# Kill existing processes
pkill -f "gradle.*bootRun"
pkill -f "npm.*dev"

# Clean restart
./stop-services.sh
./start-services.sh

# Check service logs
./watch-logs.sh
```

#### 2. Database Issues
**Problem**: `Table 'MSTE_STENCIL' doesn't exist` or database errors

**Solutions**:
```bash
# Check database status
curl -X POST http://localhost:3000/framework/db/query \
  -H "Content-Type: application/json" \
  -d '{"sql": "SHOW TABLES"}'

# Reload stencil master data
curl -X POST http://localhost:3000/mapi/apps/mste/api/reloadStencilMaster \
  -H "Content-Type: application/json" \
  -d '{"content": {}}'

# Check stencil count
curl -X POST http://localhost:3000/framework/db/query \
  -H "Content-Type: application/json" \
  -d '{"sql": "SELECT COUNT(*) FROM MSTE_STENCIL"}'
```

#### 3. Proxy Configuration Issues
**Problem**: API calls return 404 or connection refused

**Diagnostic Steps**:
```bash
# Test direct backend access
curl http://localhost:3000/apps/mste/api/suggest

# Test proxy access  
curl http://localhost:8080/mapi/apps/mste/api/suggest

# Check Nuxt.js proxy configuration
cat frontend/nuxt.config.js | grep -A 5 proxy
```

**Common Fix**:
```javascript
// frontend/nuxt.config.js
proxy: {
  '/mapi/': {
    target: 'http://localhost:3000',
    pathRewrite: { '^/mapi': '' },
    changeOrigin: true
  }
}
```

### API Response Issues

#### 4. ModelWrapper Structure Problems
**Problem**: Frontend shows empty dropdowns or undefined data

**Debug Steps**:
```javascript
// In browser console, check API response structure
console.log('Full Response:', resp);
console.log('Response Data:', resp.data);
console.log('Model Data:', resp.data.data?.model);

// Verify correct access pattern
if (resp.data.data && resp.data.data.model) {
  console.log('Stencil Categories:', resp.data.data.model.fltStrStencilCategory);
}
```

**Common Solutions**:
```javascript
// Wrong access pattern (old)
if (resp.data.model) { /* ... */ }

// Correct access pattern (new)
if (resp.data.data && resp.data.data.model) { /* ... */ }
```

#### 5. Empty Selection Issues
**Problem**: UI shows "Select first item" but dropdown remains empty

**Backend Debug**:
```java
// Check SuggestServiceImp.setFirstItemIfNoSelected method
protected static void setFirstItemIfNoSelected(ValueTextItems store) {
    // Should handle both empty strings and "*" values
    if(false == StringUtils.isEmpty(store.selected) && !"*".equals(store.selected)) {
        return;
    }
    // ... selection logic
}
```

**Frontend Debug**:
```javascript
// Check Vue.js clearAll method
clearAll() {
  this.fltStrStencilCategory.selected = '*';  // This should be handled by backend
  this.fltStrStencilCd.selected = '*';
  this.fltStrSerialNo.selected = '*';
}
```

### Template and Stencil Issues

#### 6. Stencil Not Loading
**Problem**: Stencils not appearing in categories or generation fails

**Check Stencil YAML**:
```yaml
# Verify stencil-settings.yml structure
stencil:
  config:
    categoryId: "/samples"        # Must match database
    id: "/samples/hello-world"    # Unique identifier
    name: "Hello World Generator" # Display name
    serial: "250913A"            # Version identifier
  dataElement:
    - id: "message"              # Parameter ID
  dataDomain:
    - id: "message"              # Parameter definition
      name: "メッセージ"
      value: "Hello, World!"
      type: "text"
```

**Check File Location**:
```bash
# Verify stencil files exist
ls -la backend/src/main/resources/stencil-samples/samples/hello-world/
# Should contain: stencil-settings.yml and .ftl template files
```

#### 7. Template Generation Errors
**Problem**: Generate button fails or produces errors

**Debug Steps**:
```bash
# Check template syntax
cat backend/src/main/resources/stencil-samples/samples/hello-world/template.ftl

# Test generation with curl
curl -X POST http://localhost:3000/mapi/apps/mste/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "content": {
      "stencilCategoy": "/samples",
      "stencilCanonicalName": "/samples/hello-world", 
      "serialNo": "250913A",
      "message": "Test Message"
    }
  }'
```

### Frontend UI Issues

#### 8. Dropdown Not Populating
**Problem**: Selection dropdowns show no options

**Debug in Browser Console**:
```javascript
// Check component data
console.log('Category Items:', this.fltStrStencilCategory.items);
console.log('Selected Value:', this.fltStrStencilCategory.selected);

// Verify API response mapping
this.refresh().then(() => {
  console.log('After refresh:', this.fltStrStencilCategory);
});
```

**Common Solution**:
```javascript
// Ensure proper reactive assignment
if (resp.data.data && resp.data.data.model && resp.data.data.model.fltStrStencilCategory) {
  // Use Vue.set for reactivity if needed
  this.fltStrStencilCategory = resp.data.data.model.fltStrStencilCategory;
}
```

#### 9. Form Validation Issues
**Problem**: Generate button remains disabled despite selections

**Debug State Flags**:
```javascript
// Check component state in browser console
console.log({
  cateogryNoSelected: this.cateogryNoSelected,
  stencilNoSelected: this.stencilNoSelected, 
  serialNoNoSelected: this.serialNoNoSelected,
  processing: this.processing,
  disabled: this.disabled
});

// Verify selection validation
console.log('Category Valid:', this.isFltStrSelected(this.fltStrStencilCategory));
```

### Performance and Memory Issues

#### 10. Slow API Responses
**Problem**: API calls take too long or timeout

**Backend Optimization**:
```java
// Check database query efficiency
@Query("SELECT s FROM MsteStencil s WHERE s.stencilCd = :categoryId AND s.itemKind = :itemKind")
List<MsteStencil> findByStencilCd(@Param("categoryId") String categoryId, @Param("itemKind") String itemKind);

// Add database indexes if needed
// Implement caching for frequently accessed data
```

**Frontend Optimization**:
```javascript
// Debounce frequent API calls
data() {
  return {
    refreshTimeout: null
  }
},

methods: {
  debouncedRefresh() {
    clearTimeout(this.refreshTimeout);
    this.refreshTimeout = setTimeout(() => {
      this.refresh();
    }, 300);
  }
}
```

## Debugging Tools and Techniques

### Backend Debugging

#### Database Query Tool
```bash
# Use development database access (localhost only)
curl -X POST http://localhost:3000/framework/db/query \
  -H "Content-Type: application/json" \
  -d '{"sql": "SELECT * FROM MSTE_STENCIL LIMIT 5"}'
```

#### Logging Configuration
```yaml
# application-dev.yml
logging:
  level:
    jp.vemi.mirel.apps.mste: DEBUG
    org.springframework.web: DEBUG
  pattern:
    file: "%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n"
```

#### Service Debugging
```java
// Add debug logging to services
@Service
public class SuggestServiceImp implements SuggestService {
    
    @Override
    public ApiResponse<SuggestResult> invoke(ApiRequest<SuggestParameter> parameter) {
        System.out.println("Suggest request: " + parameter.getModel());
        
        // ... service logic
        
        System.out.println("Suggest response: " + resultModel);
        return response;
    }
}
```

### Frontend Debugging

#### Vue.js DevTools
- Install Vue.js DevTools browser extension
- Inspect component state and props
- Monitor Vuex store changes (if used)
- Track component lifecycle events

#### Network Analysis
```javascript
// Axios request/response interceptors for debugging
axios.interceptors.request.use(request => {
  console.log('Starting Request:', request);
  return request;
});

axios.interceptors.response.use(response => {
  console.log('Response:', response);
  return response;
}, error => {
  console.error('Response Error:', error);
  return Promise.reject(error);
});
```

#### Component State Inspection
```javascript
// Add to Vue component for debugging
mounted() {
  // Make component accessible in console
  window.debugComponent = this;
  
  // Log initial state
  console.log('Component mounted:', {
    eparams: this.eparams,
    stencilConfig: this.stencilConfig
  });
}
```

### Integration Testing

#### API Testing Scripts
```bash
#!/bin/bash
# test-api-flow.sh - Test complete API workflow

echo "Testing suggest API..."
SUGGEST_RESPONSE=$(curl -s -X POST http://localhost:3000/mapi/apps/mste/api/suggest \
  -H "Content-Type: application/json" \
  -d '{"content":{"stencilCategoy":"*","stencilCanonicalName":"*","serialNo":"*"}}')

echo "Suggest response: $SUGGEST_RESPONSE"

echo "Testing generate API..."
GENERATE_RESPONSE=$(curl -s -X POST http://localhost:3000/mapi/apps/mste/api/generate \
  -H "Content-Type: application/json" \
  -d '{"content":{"stencilCategoy":"/samples","stencilCanonicalName":"/samples/hello-world","serialNo":"250913A","message":"Test"}}')

echo "Generate response: $GENERATE_RESPONSE"
```

#### End-to-End Testing
```javascript
// Cypress or similar E2E test
describe('ProMarker Workflow', () => {
  it('should complete full generation workflow', () => {
    cy.visit('/mste');
    
    // Wait for initial load
    cy.get('[data-cy=category-select]').should('not.be.disabled');
    
    // Select category
    cy.get('[data-cy=category-select]').select('/samples');
    
    // Verify stencil options loaded
    cy.get('[data-cy=stencil-select]').should('contain.option', 'Hello World');
    
    // Complete workflow...
  });
});
```

## Environment-Specific Issues

### DevContainer/Codespaces
```bash
# Check container resources
df -h  # Disk space
free -m  # Memory usage
nproc  # CPU cores

# Restart dev container if needed
# Command Palette: "Remote-Containers: Rebuild Container"
```

### Local Development
```bash
# Java version compatibility
java -version  # Should be Java 21
./gradlew -version  # Should use compatible Gradle

# Node.js compatibility  
node -v  # Should be 16+
npm -v   # Check npm version
```

## Prevention Strategies

### Code Quality
1. **Unit Testing**: Maintain good test coverage
2. **Integration Testing**: Test API interactions
3. **Code Reviews**: Ensure consistent patterns
4. **Documentation**: Keep documentation updated

### Monitoring
1. **Logging**: Implement comprehensive logging
2. **Error Tracking**: Monitor error rates
3. **Performance Monitoring**: Track response times
4. **Health Checks**: Implement service health endpoints

### Development Process
1. **Environment Consistency**: Use DevContainer for standardization
2. **Version Control**: Proper branching and PR process
3. **Automated Testing**: CI/CD pipeline integration
4. **Configuration Management**: Environment-specific configurations

## Getting Help

### Documentation Resources
- **Main README**: `/README.md` - Project overview and setup
- **API Reference**: `/docs/api-reference.md` - Detailed API documentation
- **Frontend Guide**: `/docs/frontend-architecture.md` - Frontend implementation details

### Log Analysis
```bash
# Monitor real-time logs
./watch-logs.sh

# Check specific log files
tail -f logs/backend.log
tail -f logs/frontend.log

# Search for specific errors
grep -i "error" logs/backend.log
grep -i "failed" logs/frontend.log
```

### Community Resources
- GitHub Issues: Report bugs and feature requests
- Pull Requests: Contribute fixes and improvements
- Documentation: Update guides based on experience