# Frontend Architecture - mirelplatform

## Overview

The ProMarker frontend is built with Vue.js 2.x and Nuxt.js, providing a responsive and interactive user interface for template management and code generation. This document covers the architecture, components, patterns, and implementation details.

## Technology Stack

### Core Technologies
- **Vue.js**: 2.x with Options API patterns
- **Nuxt.js**: 2.14.9 for SSR and development tooling
- **Bootstrap Vue**: Component library for UI consistency
- **Axios**: HTTP client for API communication
- **Node.js**: 16+ for development environment

### Development Tools
- **ESLint**: Code quality and consistency
- **Jest**: Unit testing framework
- **VS Code**: Development environment with DevContainer support

## Project Structure

```
frontend/
├── assets/
│   └── styles/
│       └── common.css          # Global styles
├── components/
│   ├── Logo.vue               # Brand logo component
│   ├── SingleProcessButton.vue # Process control component
│   ├── dialog/
│   │   └── BvDownloadDialog.vue # File download modal
│   ├── frame/
│   │   ├── Footer.vue         # Application footer
│   │   └── Header.vue         # Application header
│   └── pane/
│       └── DemoAgGrid1.vue    # Data grid demonstration
├── layouts/
│   ├── default.vue            # Default layout wrapper
│   └── Main.vue              # Main application layout
├── pages/
│   ├── index.vue             # Landing page
│   ├── apprunner/
│   │   └── index.vue         # Test automation interface
│   ├── mste/
│   │   └── index.vue         # ProMarker main interface
│   └── sampleui/
│       └── index.vue         # UI component showcase
├── plugins/
│   └── src/
│       └── axios.js          # Axios configuration
├── static/                   # Static assets
├── store/                    # Vuex store (placeholder)
└── test/
    └── Logo.spec.js         # Component tests
```

## Core Components

### ProMarker Main Interface (`pages/mste/index.vue`)

**Purpose**: Primary user interface for stencil selection, parameter input, and code generation.

#### Component Structure
```vue
<template>
  <div class="container">
    <!-- Stencil Information Section -->
    <form @submit.stop.prevent="mainHandleSubmit">
      <!-- Category Selection -->
      <b-form-select 
        v-model="fltStrStencilCategory.selected"
        :options="fltStrStencilCategory.items"
        @change="stencilCategorySelected()"
      />
      
      <!-- Stencil Selection -->
      <b-form-select 
        v-model="fltStrStencilCd.selected"
        :options="fltStrStencilCd.items"
        @change="stencilSelected()"
      />
      
      <!-- Dynamic Parameter Fields -->
      <div v-for="eparam in eparams" :key="eparam.id">
        <b-form-input 
          v-model="eparam.value"
          :placeholder="eparam.placeholder"
        />
      </div>
    </form>
  </div>
</template>
```

#### Data Management
```javascript
data() {
  return {
    disabled: false,
    processing: false,
    
    // Selection state tracking
    serialNoNoSelected: true,
    stencilNoSelected: true,
    cateogryNoSelected: true,
    
    // Dynamic parameters from API
    eparams: [],
    
    // Dropdown data structures
    fltStrStencilCategory: {
      'selected': '',
      'items': []
    },
    fltStrStencilCd: {
      'selected': '',
      'items': []
    },
    fltStrSerialNo: {
      'selected': '',
      'items': []
    }
  }
}
```

### Reactive State Management

#### Selection Workflow
```javascript
methods: {
  async stencilCategorySelected() {
    // Reset dependent selections
    this.fltStrStencilCd.selected = '*';
    this.fltStrSerialNo.selected = '';
    
    // Update state flags
    this.cateogryNoSelected = false;
    this.stencilNoSelected = true;
    this.serialNoNoSelected = true;
    
    // Validate selection
    if (!this.isFltStrSelected(this.fltStrStencilCategory)) {
      this.categoryNoSelected = true;
      return false;
    }
    
    // Refresh data from API
    await this.refresh();
    return true;
  }
}
```

## API Integration Patterns

### HTTP Client Configuration
```javascript
// nuxt.config.js proxy configuration
proxy: {
  '/mapi/': {
    target: 'http://localhost:3000',
    pathRewrite: { '^/mapi': '' }
  }
}
```

### API Response Handling

#### ModelWrapper Response Pattern
```javascript
async refresh() {
  const ret = await axios.post(
    '/mapi/apps/mste/api/suggest',
    { content: this.createRequest(this) }
  ).then((resp) => {
    // Handle ModelWrapper structure: resp.data.data.model
    if (resp.data.data && resp.data.data.model) {
      const model = resp.data.data.model;
      
      // Update dropdown data
      if (model.fltStrStencilCategory) {
        this.fltStrStencilCategory = model.fltStrStencilCategory;
      }
      
      // Update parameters
      if (model.params && model.params.childs) {
        Object.assign(this.eparams, model.params.childs);
      }
    }
    
    // Handle errors
    if (resp.data.errors && resp.data.errors.length > 0) {
      this.bvMsgBoxErr(resp.data.errors);
      return false;
    }
    
    return true;
  }).catch((errors) => {
    this.bvMsgBoxErr(errors);
    return false;
  });
  
  return ret;
}
```

### Request Building
```javascript
createRequest(body) {
  const pitems = {
    stencilCategoy: body.fltStrStencilCategory.selected,
    stencilCanonicalName: body.fltStrStencilCd.selected,
    serialNo: body.fltStrSerialNo.selected
  };
  
  // Add dynamic parameters
  if (body.eparams && Array.isArray(body.eparams)) {
    const assigned = Object.assign([], body.eparams)
      .filter((item) => item && !item.noSend);
    
    for (const key in assigned) {
      if (assigned[key] && assigned[key].id) {
        pitems[assigned[key].id] = assigned[key].value;
      }
    }
  }
  
  return pitems;
}
```

## Component Communication

### Modal Dialog Integration
```javascript
// Trigger file upload dialog
fileUpload(uploadingItemId, fileId) {
  const files = [];
  // Prepare file data...
  
  this.$root.$emit('bv::show::modal', 'bv_dialog', { 
    files, 
    uploadMode: true, 
    uploadingItemId 
  });
}

// Handle file selection result
fixFileId(data) {
  let fileIds = '';
  if (data.files && Array.isArray(data.files)) {
    for (const file of data.files) {
      this.fileNames[file.fileId] = { fileName: file.name };
      fileIds += file.fileId + ',';
    }
  }
  
  // Update parameter value
  this.setEparamById(this.eparams, data.uploadingItemId, fileIds.slice(0, -1));
}
```

### Error Handling Component
```javascript
bvMsgBoxErr(msgs) {
  if (!msgs || msgs === undefined) {
    msgs = 'エラーが発生しました。管理者に問い合わせてください。';
  }
  
  let converted = '';
  if (Array.isArray(msgs)) {
    converted = msgs.join(' ');
  } else {
    converted = msgs.toString();
  }
  
  this.$bvModal.msgBoxOk(converted, {
    title: 'Error',
    size: 'lg',
    okTitle: 'Close',
    headerBgVariant: 'danger',
    headerTextVariant: 'light',
    footerBgVariant: 'light',
    scrollable: true,
    centered: true
  });
}
```

## Layout System

### Main Layout (`layouts/Main.vue`)
```vue
<template>
  <div>
    <Header />
    <main class="main-content">
      <Nuxt />
    </main>
    <Footer />
  </div>
</template>
```

### Layout Configuration
```javascript
// pages/mste/index.vue
export default {
  layout: 'Main',
  // ... component definition
}
```

## State Management Patterns

### Local Component State
- **Reactive Data**: Vue's reactivity system for UI updates
- **Computed Properties**: Derived state calculations
- **Watchers**: Side effects from state changes

### Session Storage Integration
```javascript
async checkAndExecuteInitialReload() {
  const STORAGE_KEY = 'mste_initial_reload_completed';
  
  // Check session storage for initial load flag
  if (!sessionStorage.getItem(STORAGE_KEY)) {
    console.log('初回訪問: ステンシルマスタを自動ロード中...');
    
    try {
      await this.reloadStencilMaster();
      sessionStorage.setItem(STORAGE_KEY, 'true');
      console.log('初期ステンシルマスタロード完了');
    } catch (error) {
      console.error('初期ステンシルマスタロードに失敗:', error);
      sessionStorage.setItem(STORAGE_KEY, 'error');
    }
  }
}
```

## Form Handling

### Dynamic Form Generation
```javascript
// Parameter validation
isFltStrSelected(fltStr) {
  if (!fltStr) return false;
  if (!Array.isArray(fltStr.items)) return false;
  if (!fltStr.selected) return false;
  if (fltStr.selected.length === 0) return false;
  return true;
}

// Form submission
mainHandleSubmit() {
  // Validation logic
  if (this.serialNoNoSelected) {
    this.bvMsgBoxErr('必要な選択を完了してください。');
    return;
  }
  
  // Submit form
  this.generate();
}
```

### JSON Import/Export
```javascript
// Export current state to JSON
paramToJsonValue(eparams) {
  if (!this.fltStrStencilCategory.selected) return {};
  
  const dataElements = [];
  if (eparams && Array.isArray(eparams)) {
    for (const eparam of eparams) {
      if (eparam && eparam.id) {
        dataElements.push({
          id: eparam.id,
          value: eparam.value
        });
      }
    }
  }
  
  return {
    stencilCategory: this.fltStrStencilCategory.selected,
    stencilCd: this.fltStrStencilCd.selected,
    serialNo: this.fltStrSerialNo.selected,
    dataElements
  };
}
```

## Performance Optimization

### Async Operations
```javascript
// Non-blocking API calls
async stencilSelected() {
  this.processing = true;
  
  try {
    // Update UI state immediately
    this.serialNoNoSelected = true;
    
    // Fetch data asynchronously
    await this.refresh();
    
    // Update dependent state
    this.serialNoNoSelected = false;
  } finally {
    this.processing = false;
  }
}
```

### Loading States
```javascript
data() {
  return {
    processing: false,  // Global processing state
    disabled: false     // Form disable state
  }
}

// Usage in template
<b-button 
  :disabled="disabled || processing || serialNoNoSelected"
  @click="generate()"
  variant="primary"
>
  Generate
</b-button>
```

## Testing Strategy

### Component Testing
```javascript
// test/Logo.spec.js example
import { mount } from '@vue/test-utils'
import Logo from '@/components/Logo.vue'

describe('Logo', () => {
  test('is a Vue instance', () => {
    const wrapper = mount(Logo)
    expect(wrapper.vm).toBeTruthy()
  })
})
```

### Integration Testing
- **API Mocking**: Mock axios responses for predictable tests
- **User Interaction**: Test complete user workflows
- **Error Scenarios**: Validate error handling and display

## Security Considerations

### Input Sanitization
- All user inputs validated before API transmission
- XSS prevention through Vue's built-in escaping
- File upload restrictions and validation

### Authentication Integration
- JWT token management (when implemented)
- Session timeout handling
- Secure cookie configuration

## Deployment Configuration

### Development
```javascript
// nuxt.config.js development settings
module.exports = {
  mode: 'spa',
  dev: process.env.NODE_ENV !== 'production',
  server: {
    host: '0.0.0.0',
    port: process.env.PORT || 8080
  }
}
```

### Production
- Static generation with `nuxt generate`
- CDN optimization for assets
- Gzip compression enabled
- Security headers configuration

## Best Practices

### Component Design
1. **Single Responsibility**: Each component has one clear purpose
2. **Props Validation**: Define and validate all component props
3. **Event Handling**: Use proper event naming conventions
4. **Lifecycle Management**: Clean up resources in `beforeDestroy`

### Code Organization
1. **Consistent Naming**: Use descriptive, consistent naming conventions
2. **Method Organization**: Group related methods logically
3. **Comment Documentation**: Document complex logic and API interactions
4. **Error Boundaries**: Implement proper error handling at component level

### API Integration
1. **Response Validation**: Always validate API response structure
2. **Loading States**: Provide visual feedback during async operations
3. **Error Handling**: Graceful degradation and user-friendly error messages
4. **Retry Logic**: Implement retry mechanisms for transient failures