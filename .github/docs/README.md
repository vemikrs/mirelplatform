# ProMarker Platform Documentation Index

This directory contains comprehensive documentation for the ProMarker Platform, structured according to GitHub Copilot best practices for optimal AI assistance and developer experience.

## Document Structure

### Core Documentation
- **[copilot-instructions.md](../.github/copilot-instructions.md)** - Main GitHub Copilot instructions file
  - Project overview and architecture
  - Development environment setup
  - Key implementation patterns
  - Security and performance guidelines

### Detailed Guides

#### [api-reference.md](./api-reference.md)
**Complete API endpoint documentation**
- Endpoint specifications and request/response formats
- ModelWrapper pattern explanation for suggest API
- Authentication and security requirements
- Integration examples and error handling

#### [frontend-architecture.md](./frontend-architecture.md)
**Vue.js implementation and component design**
- Component structure and communication patterns
- State management and reactive programming
- API integration patterns and error handling
- Performance optimization and testing strategies

#### [development-guide.md](./development-guide.md)
**Advanced development patterns and extension guides**
- Custom API endpoint creation
- Template engine integration and function resolvers
- Database extensions and entity management
- Testing strategies and deployment considerations

#### [troubleshooting.md](./troubleshooting.md)
**Common issues resolution and debugging techniques**
- Environment setup problems and solutions
- API response handling issues
- Template and stencil configuration problems
- Performance optimization and monitoring

## Quick Reference

### For New Developers
1. Start with [copilot-instructions.md](../.github/copilot-instructions.md) for project overview
2. Review [api-reference.md](./api-reference.md) for API understanding
3. Check [troubleshooting.md](./troubleshooting.md) for common setup issues

### For Feature Development
1. Reference [development-guide.md](./development-guide.md) for extension patterns
2. Use [frontend-architecture.md](./frontend-architecture.md) for UI component development
3. Consult [api-reference.md](./api-reference.md) for integration specifications

### For Debugging
1. Check [troubleshooting.md](./troubleshooting.md) for known issues
2. Reference [api-reference.md](./api-reference.md) for response structure validation
3. Use [development-guide.md](./development-guide.md) for testing strategies

## GitHub Copilot Integration

These documents are optimized for GitHub Copilot to provide:
- **Context-aware suggestions** based on project architecture
- **Pattern recognition** for consistent code generation
- **Problem-solving assistance** with comprehensive troubleshooting guides
- **Best practice enforcement** through documented patterns and examples

## Document Maintenance

### Update Guidelines
- Keep API documentation synchronized with backend changes
- Update troubleshooting guides based on common developer issues
- Maintain version compatibility information
- Include new development patterns as they emerge

### Contributing
When adding new features or fixing issues:
1. Update relevant documentation sections
2. Add troubleshooting entries for common problems
3. Include examples in development guides
4. Update API reference for new endpoints

## Key Architectural Decisions Documented

### ModelWrapper Pattern
The suggest API uses a ModelWrapper structure causing `resp.data.data.model` access pattern, documented as a temporary measure pending API structure standardization.

### Template Engine Integration
FreeMarker-based template system with custom function resolvers, allowing extensible code generation capabilities.

### DevContainer/Codespaces Support
Complete development environment containerization enabling one-click setup and consistent developer experience.

### Security Patterns
JWT authentication, localhost-restricted debug endpoints, and input validation patterns throughout the platform.

---

**Note**: This documentation structure follows GitHub's recommendations for Copilot Instructions organization, ensuring optimal AI assistance while maintaining human readability and maintenance efficiency.