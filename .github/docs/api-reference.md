# API Reference - ProMarker Platform

## Overview

ProMarker Platform provides RESTful APIs for template management, code generation, and file operations. This document covers all available endpoints, request/response structures, and implementation details.

## Base Configuration

### URLs
- **Development**: `http://localhost:3000`
- **Frontend Proxy**: Requests to `/mapi/*` are proxied to backend
- **Direct Backend**: `http://localhost:3000/apps/mste/api/*`

### Authentication
- JWT-based authentication required for most endpoints
- Development endpoints (`/framework/*`) restricted to localhost only

## Core API Endpoints

### ProMarker (MSTE) APIs

#### 1. Suggest API
**Endpoint**: `POST /mapi/apps/mste/api/suggest`

**Purpose**: Retrieve available stencils, categories, and populate UI dropdowns

**Request Structure**:
```json
{
  "content": {
    "stencilCategoy": "string",     // Category filter ("*" for all)
    "stencilCanonicalName": "string", // Stencil filter ("*" for all)
    "serialNo": "string"            // Serial number filter ("*" for all)
  }
}
```

**Response Structure**:
```json
{
  "data": {
    "model": {
      "stencil": {
        "config": {
          "id": "string",
          "name": "string",
          "categoryId": "string",
          "categoryName": "string",
          "serial": "string",
          "lastUpdate": "string",
          "lastUpdateUser": "string",
          "description": "string"
        }
      },
      "params": {
        "childs": [
          {
            "id": "string",
            "name": "string",
            "valueType": "string",
            "value": "string",
            "placeholder": "string",
            "note": "string",
            "nodeType": "ELEMENT"
          }
        ],
        "nodeType": "ROOT"
      },
      "fltStrStencilCategory": {
        "items": [
          {
            "value": "string",
            "text": "string"
          }
        ],
        "selected": "string"
      },
      "fltStrStencilCd": {
        "items": [
          {
            "value": "string", 
            "text": "string"
          }
        ],
        "selected": "string"
      },
      "fltStrSerialNo": {
        "items": [
          {
            "value": "string",
            "text": "string"
          }
        ],
        "selected": "string"
      }
    }
  },
  "messages": [],
  "errors": []
}
```

**Important Notes**:
- **ModelWrapper Structure**: Response has `data.data.model` structure due to ModelWrapper implementation
- **Frontend Access**: Use `resp.data.data.model` in Vue.js components
- **Selection Logic**: Backend automatically selects first available item when input is "*" or empty

#### 2. Generate API
**Endpoint**: `POST /mapi/apps/mste/api/generate`

**Purpose**: Generate code from selected stencil template with provided parameters

**Request Structure**:
```json
{
  "content": {
    "stencilCategoy": "string",
    "stencilCanonicalName": "string", 
    "serialNo": "string",
    "paramId1": "value1",
    "paramId2": "value2"
    // ... additional parameters based on stencil configuration
  }
}
```

**Response Structure**:
```json
{
  "data": {
    "files": [
      ["fileId1", "fileName1.java"],
      ["fileId2", "fileName2.xml"]
    ]
  },
  "messages": [],
  "errors": []
}
```

**Important Notes**:
- **Standard ApiResponse**: Uses direct `ApiResponse<GenerateResult>` structure
- **Frontend Access**: Use `resp.data.data` in Vue.js components
- **File Download**: File IDs can be used with download endpoints

#### 3. Reload Stencil Master
**Endpoint**: `POST /mapi/apps/mste/api/reloadStencilMaster`

**Purpose**: Reload stencil templates from classpath and database

**Request Structure**:
```json
{
  "content": {}
}
```

**Response**: Standard success/error response

### File Management APIs

#### Upload Endpoint
**Endpoint**: `POST /commons/upload`
- Handles file uploads with temporary storage
- Returns file ID for template parameter usage

#### Download Endpoints
**Endpoint**: `GET /commons/download/{fileId}`
- Downloads files by ID
- Supports batch ZIP downloads

### Development/Debug APIs

#### Database Query (Development Only)
**Endpoint**: `POST /framework/db/query`

**Purpose**: Execute SELECT queries for debugging (localhost only)

**Request Structure**:
```json
{
  "sql": "SELECT COUNT(*) FROM MSTE_STENCIL"
}
```

**Response Structure**:
```json
{
  "sql": "SELECT COUNT(*) FROM MSTE_STENCIL",
  "count": 1,
  "data": [[2]]
}
```

## API Response Patterns

### Standard ApiResponse Structure
```java
public class ApiResponse<T> {
    private T data;                    // Response data
    private List<String> messages;     // Success messages  
    private List<String> errors;       // Error messages
}
```

### ModelWrapper Pattern (SuggestService Only)
```java
// FIXME: Applied as temporary measure
class ModelWrapper {
    public SuggestResult model;
}
// Results in: ApiResponse.builder().data(wrapper).build()
```

### Frontend Response Handling

#### For Suggest API (ModelWrapper):
```javascript
// Correct access pattern
if (resp.data.data && resp.data.data.model) {
    const model = resp.data.data.model;
    // Process model data
}

// Error handling  
if (resp.data.errors && resp.data.errors.length > 0) {
    this.bvMsgBoxErr(resp.data.errors);
}
```

#### For Generate API (Standard):
```javascript
// Correct access pattern
if (resp.data.data && resp.data.data.files) {
    const files = resp.data.data.files;
    // Process file data
}

// Error handling
if (resp.data.errors && resp.data.errors.length > 0) {
    this.bvMsgBoxErr(resp.data.errors);
}
```

## Stencil Configuration

### YAML Structure
```yaml
stencil:
  config:
    categoryId: "/samples"
    categoryName: "Sample Stencils"
    id: "/samples/hello-world"
    name: "Hello World Generator"
    serial: "250913A"
    lastUpdate: "2025/09/13"
    lastUpdateUser: "mirelplatform"
    description: |
      Template description
  dataElement:
    - id: "paramId"
  dataDomain:
    - id: "paramId"
      name: "Parameter Name"
      value: "Default Value"
      type: "text"
      placeholder: "Placeholder text"
      note: "Parameter description"
  codeInfo:
    copyright: "Copyright notice"
    versionNo: "1.0"
    author: "Author name"
    vendor: "Vendor name"
```

### Template Storage
- **Classpath**: `backend/src/main/resources/stencil-samples/`
- **Database**: Custom stencils in `MSTE_STENCIL` table
- **Categories**: Hierarchical organization with `/` delimiters

## Error Handling

### Common Error Codes
- **404**: API endpoint not found
- **400**: Invalid request parameters
- **500**: Server-side processing errors

### Error Response Format
```json
{
  "data": null,
  "messages": [],
  "errors": ["Error message 1", "Error message 2"]
}
```

### Frontend Error Display
```javascript
bvMsgBoxErr(msgs) {
  // Handles both string and array error messages
  // Displays modal dialog with error details
}
```

## Performance Considerations

### Caching
- Template metadata cached for performance
- Database queries optimized with proper indexing
- File operations use streaming for large files

### Request Optimization
- Batch operations supported for multiple files
- Lazy loading of template parameters
- Efficient dropdown population with selected item logic

## Security Features

### Access Control
- JWT authentication for production endpoints
- Localhost restriction for debug endpoints
- Temporary file cleanup and access control

### Input Validation
- SQL injection prevention in debug queries
- Template parameter sanitization
- File upload security checks

## Integration Examples

### Vue.js Component Integration
```javascript
// Component method example
async stencilCategorySelected() {
  this.fltStrStencilCd.selected = '*';
  
  if (!this.isFltStrSelected(this.fltStrStencilCategory)) {
    return false;
  }
  
  await this.refresh(); // Calls suggest API
  return true;
}
```

### Backend Service Integration
```java
@Service
public class CustomService {
    @Autowired
    private SuggestService suggestService;
    
    public ApiResponse<SuggestResult> getStencilData(String category) {
        SuggestParameter param = new SuggestParameter();
        param.stencilCategory = category;
        // ... setup request
        return suggestService.invoke(apiRequest);
    }
}
```