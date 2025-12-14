# AI Coding Agent Guidelines

## Development Practices

### Code Standards
- Follow Kotlin/Android conventions
- Use meaningful variable/function names
- Keep functions small and focused
- Add comments for complex logic only

### Architecture
- Follow MVVM pattern
- Use Repository pattern for data layer
- Implement proper dependency injection
- Separate UI, business logic, and data layers

### Testing
- Write unit tests for business logic
- Use MockK for mocking in tests
- Test edge cases and error scenarios
- Maintain >80% code coverage

## AI Agent Instructions

### When Contributing Code
1. **Analyze existing patterns** before implementing new features
2. **Follow established naming conventions** in the codebase
3. **Use existing dependencies** rather than adding new ones
4. **Implement proper error handling** with try-catch blocks
5. **Add logging** for debugging purposes using Timber

### Code Generation Rules
- Generate minimal, focused implementations
- Avoid over-engineering solutions
- Use existing utility classes and extensions
- Follow the project's existing file structure

### Firebase Integration
- Use Firebase Genkit for AI features
- Implement proper offline handling
- Follow Firebase security rules
- Use Firestore for real-time data

### UI Development
- Use Jetpack Compose for UI
- Follow Material Design 3 guidelines
- Implement proper state management
- Handle loading and error states

## Prohibited Actions
- Do not modify core architecture without approval
- Do not add unnecessary dependencies
- Do not bypass existing security measures
- Do not remove existing error handling
