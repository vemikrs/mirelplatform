# API Reference - ProMarker Platform

## Overview

ProMarker Platform provides RESTful APIs for template management, code generation, and file operations. This document covers all available endpoints, request/response structures, and implementation details.

## Base Configuration

### URLs
- Backend base (dev): `http://localhost:3000/mipla2`
- Swagger UI: `http://localhost:3000/mipla2/swagger-ui.html`
- OpenAPI JSON: `http://localhost:3000/mipla2/api-docs`
- Frontend Proxy: Requests to `/mapi/*` are proxied to backend
  - example: proxy path `/mapi/apps/mste/api/*` → backend `/apps/mste/api/*` (under base `/mipla2`)
- Direct Backend: `http://localhost:3000/mipla2/apps/mste/api/*`

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
**Endpoint**: `POST /mapi/apps/mste/api/generate` (proxy)
  - Direct: `POST /mipla2/apps/mste/api/generate`

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

Example:
```json
{
  "content": {
    "stencilCategoy": "/samples",
    "stencilCanonicalName": "/samples/hello-world",
    "serialNo": "250913A",
    "message": "Hello"
  }
}
```

**Response Structure**:
```json
{
  "data": {
    "files": [
      {
        "abc123-def456-...": "hello-world-250913A.zip"
      }
    ]
  },
  "messages": [],
  "errors": []
}
```

**重要**: 
- `files` は Pair<String, String> のリストで、JSONではオブジェクト配列として表現されます
- **キー**: 生成されたZIPファイルのファイル管理ID (UUID形式)
- **値**: ZIPファイルの論理名（例: `hello-world-250913A.zip`）
- **ZIPファイルの内容**: ステンシルから生成された複数ファイル（.java, .xml等）がアーカイブされている
- **ダウンロード**: fileId（オブジェクトのキー）を `/commons/dlsite/{fileId}` または `/commons/download` で使用してZIPファイルをダウンロード
- **関連API**: `/commons/upload` (ファイル型パラメータのアップロード)

**Note**: Apache Commons の Pair クラスが JSON シリアライズされる際、配列ではなくオブジェクトとして出力されます。

**Important Notes**:
- **Standard ApiResponse**: Uses direct `ApiResponse<GenerateResult>` structure
- **Frontend Access**: Use `resp.data.data` in Vue.js components
- **File Download**: File IDs can be used with download endpoints (`/commons/dlsite/{fileIds}` GET or `/commons/download` POST)

#### 3. Reload Stencil Master
**Endpoint**: `POST /mapi/apps/mste/api/reloadStencilMaster` (proxy)
  - Direct: `POST /mipla2/apps/mste/api/reloadStencilMaster`

**Purpose**: Reload stencil templates from classpath and database

**Request Structure**:
```json
{
  "content": {}
}
```

Example:
```json
{ "content": {} }
```

**Response**: Standard success/error response

### File Management APIs

#### Upload Endpoint
**Endpoint**: `POST /mapi/commons/upload` (proxy) / `POST /mipla2/commons/upload` (direct)
- Handles file uploads with temporary storage
- Returns file ID for template parameter usage

#### Download Endpoints
**Endpoints**:
- `GET /mapi/commons/dlsite/{fileIds}` (proxy) / `GET /mipla2/commons/dlsite/{fileIds}` (direct)
  - Downloads files by IDs (comma-separated). Multiple IDs → ZIP archive
- `POST /mapi/commons/download` (proxy) / `POST /mipla2/commons/download` (direct)
  - Downloads by request body `{"content":[{"fileId":"id1"},{"fileId":"id2"}]}`
  - Multiple IDs → ZIP archive
  
Notes:
- The fileIds are the first elements of the `files` tuples returned by Generate API.

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

## Quick Testing with cURL

### Suggest API - Get All Available Stencils
```bash
curl -X POST http://localhost:3000/mipla2/apps/mste/api/suggest \
  -H "Content-Type: application/json" \
  -d '{
    "content": {
      "stencilCategoy": "*",
      "stencilCanonicalName": "*",
      "serialNo": "*"
    }
  }'
```

**Expected Response**: List of available categories, stencils, and serial numbers with first item auto-selected.

### Reload Stencil Master - Refresh Template Cache
```bash
curl -X POST http://localhost:3000/mipla2/apps/mste/api/reloadStencilMaster \
  -H "Content-Type: application/json" \
  -d '{
    "content": {}
  }'
```

**Expected Response**: `{"data": null, "messages": ["Reloaded successfully"], "errors": []}`

### Generate API - Create Code from Template
```bash
curl -X POST http://localhost:3000/mipla2/apps/mste/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "content": {
      "stencilCategoy": "/samples",
      "stencilCanonicalName": "/samples/hello-world",
      "serialNo": "250913A",
      "message": "Hello, ProMarker!",
      "userName": "Developer",
      "language": "ja"
    }
  }'
```

**Expected Response**: 
```json
{
  "data": {
    "files": [
      {
        "ac50ee03-66a4-4d20-9d47-6bfa801b2e33": "251003212109145.zip"
      }
    ]
  },
  "messages": [],
  "errors": []
}
```

**Next Step**: Extract the fileId (object key, e.g., `ac50ee03-66a4-4d20-9d47-6bfa801b2e33`) and use it to download the generated ZIP file.

**Extracting fileId with jq**:
```bash
FILE_ID=$(curl -s ... | jq -r '.data.files[0] | to_entries[0].key')
echo "FileID: $FILE_ID"
```

### Download API - Get Generated Files

#### Method 1: GET with fileId in path
```bash
# Replace {fileId} with actual fileId from generate response
curl -X GET "http://localhost:3000/mipla2/commons/dlsite/abc123def456" \
  --output hello-world-250913A.zip
```

#### Method 2: POST with request body
```bash
curl -X POST http://localhost:3000/mipla2/commons/download \
  -H "Content-Type: application/json" \
  -d '{
    "content": [
      {"fileId": "abc123def456"}
    ]
  }' \
  --output hello-world-250913A.zip
```

#### Method 3: Multiple files (auto-zipped)
```bash
curl -X GET "http://localhost:3000/mipla2/commons/dlsite/fileId1,fileId2,fileId3" \
  --output multiple-files.zip
```

**Note**: Multiple fileIds are automatically bundled into a single ZIP archive.

### Upload API - Upload File Parameters
```bash
curl -X POST http://localhost:3000/mipla2/commons/upload \
  -F "file=@/path/to/your/file.txt"
```

**Expected Response**:
```json
{
  "data": [
    {
      "fileId": "xyz789",
      "name": "file.txt"
    }
  ],
  "messages": [],
  "errors": []
}
```

**Usage**: Use the returned `fileId` as a parameter value for file-type stencil parameters.

## Frontend Usage Guide

### ProMarker UI Workflow

**Access URL**: http://localhost:5173/promarker

#### Typical Code Generation Workflow

1. **Select Category** (カテゴリ選択)
   - Open ProMarker UI
   - From "Stencil Category" dropdown, select desired category (e.g., `/samples`)
   - UI automatically loads available stencils for selected category

2. **Select Stencil** (ステンシル選択)
   - From "Stencil" dropdown, select template (e.g., `Hello World Generator`)
   - UI automatically loads available versions (serial numbers)

3. **Select Version** (バージョン選択)
   - From "Serial No" dropdown, select version (e.g., `250913A`)
   - UI displays dynamic parameter input fields based on stencil configuration

4. **Input Parameters** (パラメータ入力)
   - Fill in required parameters (e.g., `message`: "Hello, World!")
   - For file-type parameters:
     - Click "Upload" button
     - Select file from dialog
     - File is automatically uploaded and fileId is set

5. **Generate Code** (コード生成)
   - Click "Generate" button
   - System generates code based on template and parameters
   - Generated files are packaged into ZIP archive

6. **Download Result** (結果ダウンロード)
   - Download dialog appears automatically
   - Click "Download" to save ZIP file
   - Alternatively, use the fileId with download API endpoints

#### Advanced Features

- **Parameter Import/Export**: Save parameter sets as JSON for reuse
  - Export: "Export Parameters" button → JSON file download
  - Import: "Import Parameters" button → Select JSON file

- **Batch Generation**: Generate multiple variations by modifying parameters
  - Change parameters between generations
  - Each generation creates new ZIP with unique fileId

- **Template Preview**: Some stencils support preview mode
  - Shows generated code structure before full generation

### Frontend API Integration Patterns

#### Using Axios in Vue.js Components
```javascript
// Example: Generate code from frontend
async generateCode() {
  try {
    const response = await axios.post('/mapi/apps/mste/api/generate', {
      content: {
        stencilCategoy: this.selectedCategory,
        stencilCanonicalName: this.selectedStencil,
        serialNo: this.selectedSerial,
        message: this.userMessage
      }
    });
    
    if (response.data.data && response.data.data.files) {
      const [fileId, fileName] = response.data.data.files[0];
      console.log(`Generated: ${fileName} (ID: ${fileId})`);
      
      // Trigger download
      window.location.href = `/mapi/commons/dlsite/${fileId}`;
    }
    
    if (response.data.errors && response.data.errors.length > 0) {
      this.showErrors(response.data.errors);
    }
  } catch (error) {
    console.error('Generation failed:', error);
  }
}
```

#### Error Handling Pattern
```javascript
methods: {
  async callAPI(endpoint, params) {
    try {
      const response = await axios.post(endpoint, { content: params });
      
      // Check for API-level errors
      if (response.data.errors && response.data.errors.length > 0) {
        this.bvMsgBoxErr(response.data.errors);
        return null;
      }
      
      return response.data.data;
    } catch (error) {
      // HTTP-level errors
      this.bvMsgBoxErr(error.message || 'Network error occurred');
      return null;
    }
  },
  
  bvMsgBoxErr(msgs) {
    const message = Array.isArray(msgs) ? msgs.join('\n') : msgs;
    this.$bvModal.msgBoxOk(message, {
      title: 'Error',
      size: 'lg',
      okTitle: 'Close',
      headerBgVariant: 'danger'
    });
  }
}
```

## Testing Checklist

### API Endpoints Testing

- [ ] **Suggest API**: Returns categories, stencils, and serial numbers
  ```bash
  curl -X POST http://localhost:3000/mipla2/apps/mste/api/suggest \
    -H "Content-Type: application/json" \
    -d '{"content":{"stencilCategoy":"*","stencilCanonicalName":"*","serialNo":"*"}}'
  ```

- [ ] **Reload API**: Successfully reloads stencil master
  ```bash
  curl -X POST http://localhost:3000/mipla2/apps/mste/api/reloadStencilMaster \
    -H "Content-Type: application/json" \
    -d '{"content":{}}'
  ```

- [ ] **Generate API**: Creates ZIP file with generated code
  ```bash
  curl -X POST http://localhost:3000/mipla2/apps/mste/api/generate \
    -H "Content-Type: application/json" \
    -d '{"content":{"stencilCategoy":"/samples","stencilCanonicalName":"/samples/hello-world","serialNo":"250913A","message":"Test","userName":"Developer","language":"ja"}}'
  ```

- [ ] **Download API (GET)**: Downloads ZIP by fileId
  ```bash
  curl -X GET "http://localhost:3000/mipla2/commons/dlsite/{fileId}" --output test.zip
  ```

- [ ] **Download API (POST)**: Downloads ZIP with request body
  ```bash
  curl -X POST http://localhost:3000/mipla2/commons/download \
    -H "Content-Type: application/json" \
    -d '{"content":[{"fileId":"abc123"}]}' --output test.zip
  ```

### Frontend Testing

- [ ] **ProMarker UI Access**: http://localhost:5173/promarker loads successfully
- [ ] **Category Selection**: Dropdown populates with available categories
- [ ] **Stencil Selection**: Stencil dropdown updates based on selected category
- [ ] **Parameter Display**: Input fields appear based on stencil configuration
- [ ] **File Upload**: File parameter upload works correctly
- [ ] **Code Generation**: Generate button creates ZIP file
- [ ] **Download Dialog**: Download dialog appears with correct file name
- [ ] **Error Display**: Errors show in modal dialog with clear messages

### Integration Testing

- [ ] **Full Workflow**: Category → Stencil → Parameters → Generate → Download
- [ ] **File Parameter**: Upload file → Use in generation → Verify in output
- [ ] **Multiple Generations**: Sequential generations with different parameters
- [ ] **Error Scenarios**: Invalid parameters show appropriate error messages
- [ ] **ZIP Content**: Downloaded ZIP contains expected generated files

## Troubleshooting

### Common Issues

1. **Empty Dropdowns**: Run reload API to refresh stencil cache
2. **Generation Fails**: Check parameter values match stencil requirements
3. **Download Error**: Verify fileId is correct and file hasn't expired (3-day retention)
4. **CORS Issues**: Ensure using proxy path `/mapi/*` instead of direct backend URLs
5. **404 Errors**: Verify backend is running and context path is `/mipla2`

### Debug Commands

```bash
# Check stencil count
curl -X POST http://localhost:3000/framework/db/query \
  -H "Content-Type: application/json" \
  -d '{"sql":"SELECT COUNT(*) FROM MSTE_STENCIL"}'

# List available stencils
curl -X POST http://localhost:3000/framework/db/query \
  -H "Content-Type: application/json" \
  -d '{"sql":"SELECT stencil_cd, stencil_name, item_kind FROM MSTE_STENCIL LIMIT 10"}'

# Check file management records
curl -X POST http://localhost:3000/framework/db/query \
  -H "Content-Type: application/json" \
  -d '{"sql":"SELECT file_id, file_name, expire_date FROM FILE_MANAGEMENT ORDER BY expire_date DESC LIMIT 5"}'
```

**Note**: Debug endpoints (`/framework/*`) are only accessible from localhost.