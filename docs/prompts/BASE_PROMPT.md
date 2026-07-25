# BASE_PROMPT.md

Read the following documents before generating any code:

- PROJECT_CONTEXT.md
- DATABASE.md
- TODO.md

Follow all rules defined in those documents.

## Coding Rules

- Use Java 21
- Use Spring Boot 4.1
- Use constructor injection
- Never use @Autowired
- Use @RequiredArgsConstructor
- Use Spring Data JPA
- Use ApiResponse<T>
- Use BusinessException
- Use GlobalExceptionHandler
- Use Jakarta Validation
- Keep methods small and readable
- Generate production-ready code
- Do not modify unrelated modules
- Do not change package structure
- Do not modify Flyway migrations

## Required Output

Always provide:

1. Files created or modified
2. Complete source code
3. Explanation of important decisions

---

## Architecture Decisions

For every important decision explain:

### Decision

### Why

### Alternative

### Trade-off

### Future Improvement

---

## Knowledge Check

Explain:

1. Which Spring features are used?
2. Which design patterns are used?
3. Why this implementation?
4. Possible interview questions
5. Common mistakes
6. Which code is most important to understand