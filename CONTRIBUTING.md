# Contributing to Pocket Nexus

Thank you for your interest in contributing! This document provides guidelines for contributing to the project.

## Code of Conduct

- Be respectful and inclusive
- Focus on constructive feedback
- Help maintain a welcoming environment

## How to Contribute

### Reporting Bugs

1. Check if the bug has already been reported in [Issues](../../issues)
2. If not, create a new issue with:
   - Clear title and description
   - Steps to reproduce
   - Expected vs actual behavior
   - Screenshots if applicable
   - Device and Android version

### Suggesting Features

1. Check [Issues](../../issues) for existing feature requests
2. Create a new issue with:
   - Clear description of the feature
   - Use cases and benefits
   - Possible implementation approach

### Pull Requests

1. **Fork** the repository
2. **Create a branch** for your feature:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Make your changes** following the code style
4. **Test thoroughly** on multiple devices
5. **Commit** with clear messages:
   ```bash
   git commit -m "Add: feature description"
   ```
6. **Push** to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```
7. **Create a Pull Request** with:
   - Description of changes
   - Related issue number (if applicable)
   - Screenshots/videos of UI changes

## Code Style

### Kotlin
- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Add comments for complex logic
- Keep functions small and focused

### Compose
- Use `remember` and `LaunchedEffect` appropriately
- Avoid side effects in composables
- Extract reusable components
- Follow Material 3 guidelines

### Commits
- Use present tense ("Add feature" not "Added feature")
- Keep commits focused and atomic
- Reference issues when applicable

## Development Setup

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle
4. Run on emulator or device

## Testing

- Test on multiple Android versions (API 24+)
- Test on different screen sizes
- Verify dark mode compatibility
- Check for memory leaks

## Questions?

Feel free to open an issue for any questions or clarifications.

---

**Thank you for contributing!** 🎉
