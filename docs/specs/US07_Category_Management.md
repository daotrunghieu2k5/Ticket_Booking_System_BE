 US-07 — Category Management
 
## 1. Objective

Implement category management for the Ticket Booking System.

Categories are used to classify events such as concerts, movies, conferences, and sports.

This user story belongs to the Event module and will be used by:

- admins to manage categories
- public users to browse categories when searching or filtering events

---

## 2. Scope

### Included

- Get all categories
- Get category by id
- Create category
- Update category
- Delete category
- Validate category name uniqueness
- Prevent deleting categories that are already used by events

### Not Included

- Event creation
- Venue management
- Seat management
- Event session management
- Pagination for category list unless already used by the project
- Soft delete unless already required by the existing codebase
- Category images
- Category hierarchy / parent-child categories

---

## 3. Business Flow

### 3.1 Public List Categories

```text
Client
  ↓
GET /api/v1/categories
  ↓
Load all categories
  ↓
Return category list
3.2 Get Category Detail
Client
  ↓
GET /api/v1/categories/{id}
  ↓
Find category by id
  ↓
Category exists?
  ├─ NO → throw BusinessException
  └─ YES
       ↓
Return category detail
3.3 Create Category
Admin
  ↓
POST /api/v1/admin/categories
  ↓
Validate request
  ↓
Check duplicated category name
  ↓
Create category
  ↓
Save category
  ↓
Return created response
3.4 Update Category
Admin
  ↓
PUT /api/v1/admin/categories/{id}
  ↓
Validate request
  ↓
Find category by id
  ↓
Category exists?
  ├─ NO → throw BusinessException
  └─ YES
       ↓
Check duplicated category name if name changed
       ↓
Update category fields
       ↓
Save category
       ↓
Return success response
3.5 Delete Category
Admin
  ↓
DELETE /api/v1/admin/categories/{id}
  ↓
Find category by id
  ↓
Category exists?
  ├─ NO → throw BusinessException
  └─ YES
       ↓
Check whether category is used by any event
       ↓
Category in use?
  ├─ YES → throw BusinessException
  └─ NO
       ↓
Delete category
       ↓
Return success response
4. Functional Requirements
FR-01

Allow public users to retrieve all categories.

FR-02

Allow public users to retrieve category details by id.

FR-03

Allow admins to create a new category.

FR-04

Allow admins to update an existing category.

FR-05

Allow admins to delete a category if it is not used by any event.

FR-06

Prevent duplicate category names.

FR-07

Prevent deleting a category that is already used by events.

FR-08

Return responses using the standard ApiResponse<T> structure.

5. Business Rules
BR-01

Category name must be unique ignoring case and surrounding spaces.

BR-02

Category name must not be blank.

BR-03

Category description is optional.

BR-04

If a category is used by at least one event, it must not be deleted.

BR-05

Updating a category must not create a duplicate name conflict with another category.

BR-06

Create, update, and delete operations are restricted to ADMIN users.

BR-07

Read operations may be public if the existing security configuration allows it.

BR-08

The system must not silently ignore duplicate category names. It must reject the request with a clear business error.

6. API Contract
6.1 Get All Categories
Endpoint
GET /api/v1/categories
Response
{
  "success": true,
  "message": "Categories retrieved successfully.",
  "data": [
    {
      "id": 1,
      "name": "Concert",
      "description": "Music events"
    }
  ],
  "timestamp": "2026-07-25T10:00:00"
}
6.2 Get Category By Id
Endpoint
GET /api/v1/categories/{id}
Response
{
  "success": true,
  "message": "Category retrieved successfully.",
  "data": {
    "id": 1,
    "name": "Concert",
    "description": "Music events"
  },
  "timestamp": "2026-07-25T10:00:00"
}
6.3 Create Category
Endpoint
POST /api/v1/admin/categories
Request
{
  "name": "Concert",
  "description": "Music events"
}
Response
{
  "success": true,
  "message": "Category created successfully.",
  "data": {
    "id": 1,
    "name": "Concert",
    "description": "Music events"
  },
  "timestamp": "2026-07-25T10:00:00"
}
6.4 Update Category
Endpoint
PUT /api/v1/admin/categories/{id}
Request
{
  "name": "Live Concert",
  "description": "Updated description"
}
Response
{
  "success": true,
  "message": "Category updated successfully.",
  "data": {
    "id": 1,
    "name": "Live Concert",
    "description": "Updated description"
  },
  "timestamp": "2026-07-25T10:00:00"
}
6.5 Delete Category
Endpoint
DELETE /api/v1/admin/categories/{id}
Response
{
  "success": true,
  "message": "Category deleted successfully.",
  "data": null,
  "timestamp": "2026-07-25T10:00:00"
}
7. Validation Rules
Create Category Request
name is required
name must not be blank
name must not exceed the project-defined maximum length
description is optional
description must not exceed the project-defined maximum length if provided
Update Category Request
name is required
name must not be blank
description is optional

If the project already defines a shared text-length convention, reuse it.
If not, keep validation practical and consistent with the database schema.

8. Database Impact
Read
category
event (only when checking whether category is in use before deletion)
Insert
category
Update
category
Delete
category only if not used by events
Suggested repository queries
findAll()
findById(id)
existsByNameIgnoreCase(name)
existsByNameIgnoreCaseAndIdNot(name, id) for update conflict checking
existsByCategoryId(id) on EventRepository for delete safety check
9. Expected Classes
Controller
CategoryController
Service
CategoryService
CategoryServiceImpl
DTO
CreateCategoryRequest
UpdateCategoryRequest
CategoryResponse
Repository
CategoryRepository
EventRepository (or equivalent method for dependency check)
Mapper
CategoryMapper
Entity
Category
Exception Classes

Use existing exceptions if possible. Add new ones only if needed:

CategoryNotFoundException
CategoryAlreadyExistsException
CategoryInUseException
10. Transaction
Create Category

Usually transactional.

Update Category

Usually transactional.

Delete Category

Must be transactional if the implementation checks event usage and deletes within the same flow.

Rollback on:

repository failure
constraint violation
event usage check failure
11. Exception Cases

Throw BusinessException or project-specific business exceptions when:

category name is blank
category does not exist
category name already exists
category is in use by at least one event
user tries to access admin-only endpoints without ADMIN role
12. Acceptance Criteria
Public users can list categories.
Public users can view category details.
Admin can create categories.
Admin can update categories.
Admin can delete categories only when not used by events.
Duplicate category names are rejected.
Responses follow ApiResponse<T> format.
Validation errors are handled globally.
Admin-only actions are properly protected.
13. Coding Constraints
Follow PROJECT_CONTEXT.md
Follow DATABASE.md
Follow TODO.md
Keep the existing package structure
Use constructor injection only
Do not use field injection
Use ApiResponse<T>
Use BusinessException
Do not modify unrelated modules
Do not change completed Flyway migrations
Do not introduce unnecessary abstractions
Generate production-ready code
Keep the implementation simple and maintainable
14. Review Checklist

Before considering the implementation complete, verify:

category names are validated and normalized
duplicate name checks are correct
delete safety check against events works
admin-only endpoints are protected
response format matches the API contract
controller remains thin
service contains business logic
repository queries are minimal
validation is handled by Jakarta Validation
no unnecessary code or abstraction is introduced
15. Architecture Decisions

After generating the code, always include an Architecture Review section.

For every important decision, explain:

Decision

What decision was made?

Why

Why was this approach chosen?

Alternative

What is another reasonable approach?

Trade-off

What are the pros and cons?

Future Improvement

How could this evolve in a larger production system?

16. Knowledge Check

After implementation, explain the code as if mentoring a Java Backend Intern.

Include:

Which Spring features are used?
Which design patterns are used?
Why is this implementation better than a simpler approach?
What interview questions could be asked about category management?
What common mistakes do developers make when implementing CRUD + delete-safety checks?
Which parts of the code are most important to understand?