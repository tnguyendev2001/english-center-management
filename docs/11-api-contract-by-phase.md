# Phase 1 API Contract

## Student

GET /api/students

Query:
- keyword
- status
- page
- size

Response:
- id
- studentCode
- fullName
- phone
- parentPhone
- status

POST /api/students

Request:
- fullName
- dateOfBirth
- phone
- parentName
- parentPhone
- address
- note