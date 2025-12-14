# Agent Guidelines

## Development Practices

### Code Standards
- Follow Kotlin/Android conventions
- Use meaningful variable/function names
- Keep functions small and focused
- Add comments for complex logic only

### Architecture
- Follow the MVVM pattern
- Use a repository pattern for the data layer
- Implement proper dependency injection
- Separate UI, business logic, and data layers

### Model Context Protocol (If available)
- Use **Supabase MCP** for backend related tasks.
- Use **Context7** for better understanding lf latest guidance.

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

### BaaS
- Supabase

### UI Development
- Use Jetpack Compose for UI
- Follow Material Design 3 guidelines
- Implement proper state management
- Handle loading and error states
- Use Material 3 Expressive
- Keep consistency in the UI/UX
- Use super smooth animations in everything (if possible)

## Prohibited Actions
- Do not modify core architecture without approval
- Do not add unnecessary dependencies
- Do not bypass existing security measures
- Do not remove existing error handling