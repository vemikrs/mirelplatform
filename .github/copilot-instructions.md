# ProMarker Platform - GitHub Copilot Instructions

## Project Overview

ProMarker is a comprehensive code generation and template management platform built on the mirelplatform framework. This application provides automated development scaffolding, template processing, and code generation capabilities for rapid application development.

### Architecture
- **Backend**: Spring Boot 3.3 with Java 21, mirelplatform framework
- **Frontend**: Vue.js 2.x with Nuxt.js, Bootstrap Vue components
- **Database**: H2 (development), MySQL (production)
- **Template Engine**: FreeMarker with custom function resolvers
- **Container**: DevContainer support for Codespaces and local development

## Development Environment

### Quick Start
```bash
# Start both backend and frontend services
./start-services.sh

# Stop services
./stop-services.sh

# Monitor logs
./watch-logs.sh [backend|frontend]
```

### Service URLs
- Frontend: http://localhost:8080/mirel/
- Backend API: http://localhost:3000
- ProMarker UI: http://localhost:8080/mirel/mste/

### Key Configuration Files
- `backend/src/main/resources/config/application.yml` - Main configuration
- `frontend/nuxt.config.js` - Frontend proxy and build settings
- `settings.gradle` - Multi-project Gradle configuration
- `.devcontainer/devcontainer.json` - Development container setup

## Core Components

### Backend Structure
```
backend/
├── src/main/java/jp/vemi/
│   ├── mirel/apps/mste/        # ProMarker core functionality
│   │   ├── domain/service/     # Business logic
│   │   ├── application/controller/ # REST controllers
│   │   └── domain/dto/         # Data transfer objects
│   ├── framework/              # Base framework utilities
│   └── ste/                   # Stencil Template Engine
├── src/main/resources/
│   ├── config/                # Configuration files
│   ├── stencil-samples/       # Built-in template samples
│   └── templates/             # Web templates
```

### Frontend Structure
```
frontend/
├── pages/
│   └── mste/
│       └── index.vue          # Main ProMarker UI component
├── components/
│   ├── frame/                 # Layout components
│   └── dialog/                # Modal dialogs
├── layouts/
│   └── Main.vue              # Main application layout
```

## Development Guidelines

### Code Style
- Java: Follow Spring Boot conventions, use Lombok for boilerplate reduction
- Vue.js: Use Composition API patterns where possible, maintain reactive data patterns
- Ensure proper null safety checks, especially for API response handling

### API Response Patterns
- Most APIs return `ApiResponse<T>` structure with `data`, `messages`, `errors` fields
- Special case: SuggestService uses ModelWrapper for frontend compatibility
- Always check for both success data and error conditions

### Database Access
- Primary: JPA repositories with Spring Data patterns
- Development: Framework debug endpoint `/framework/db/query` (localhost only)
- Use proper transaction boundaries for multi-step operations

### Template System
- Stencil templates use YAML configuration + FreeMarker templates
- Templates stored in `backend/src/main/resources/stencil-samples/`
- Support for hierarchical category organization

## Key Features Implementation

### ProMarker (MSTE) - Main Template Engine
- **Purpose**: Dynamic code generation from templates
- **Main UI**: `/frontend/pages/mste/index.vue`
- **Core Service**: `SuggestServiceImp`, `GenerateServiceImp`
- **Workflow**: Category selection → Stencil selection → Parameter input → Generation

### Stencil Management
- **Configuration**: YAML-based stencil settings
- **Templates**: FreeMarker (.ftl) files for code generation
- **Storage**: Classpath-bundled samples + database-managed custom stencils
- **Categories**: Hierarchical organization with user/standard/sample levels

### File Management
- **Upload/Download**: Secure temporary file handling
- **Batch Operations**: ZIP compression for multi-file downloads
- **Integration**: Template parameter file references

## Related Documentation

For detailed information on specific aspects, refer to:

- **[API Reference](./docs/api-reference.md)** - Complete API endpoint documentation
- **[Frontend Architecture](./docs/frontend-architecture.md)** - Vue.js implementation details
- **[Development Guide](./docs/development-guide.md)** - Advanced development patterns
- **[Troubleshooting](./docs/troubleshooting.md)** - Common issues and solutions

## Common Development Tasks

### Adding New API Endpoints
1. Create DTO classes in `domain/dto/`
2. Implement service in `domain/service/`
3. Create API wrapper in `domain/api/`
4. Register with Spring's component scanning

### Frontend Component Development
1. Follow Vue.js 2.x patterns with Options API
2. Use Bootstrap Vue components for UI consistency
3. Implement proper error handling with `bvMsgBoxErr`
4. Maintain reactive state management patterns

### Template Development
1. Create YAML configuration in `stencil-samples/`
2. Implement FreeMarker templates (.ftl files)
3. Test with ProMarker UI workflow
4. Ensure proper parameter validation

## Security Considerations

- **Authentication**: JWT-based with session management
- **API Access**: Most endpoints require authentication
- **File Security**: Temporary file cleanup and access control
- **Database**: Development debug access restricted to localhost only
- **Template Security**: Proper input validation for template parameters

## Performance Guidelines

- **Frontend**: Use proper Vue.js reactivity patterns, avoid unnecessary API calls
- **Backend**: Implement proper caching for template metadata, use efficient database queries
- **File Operations**: Stream large files, implement proper cleanup for temporary files

---

This document provides the foundation for working with the ProMarker platform. Refer to the detailed documentation in the `docs/` directory for specific implementation guidance.