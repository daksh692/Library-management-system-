To support your React, Spring Boot, and MongoDB library management system, here is a clean, production-grade system architecture. It outlines the data flow, security boundary, and the physical directory structures for both backend and frontend.

1. High-Level System Architecture
The application uses a Layered Architecture with a decoupling boundary between the React Client and the Spring Boot API.

       +-----------------------------------------------------------+
       |                     React UI Client                       |
       |     [User Views]      [Admin Views]      [Shared Auth]     |
       +------------------------------+----------------------------+
                                      |
                                      | Secure HTTPS (JWT in Header)
                                      v
       +-----------------------------------------------------------+
       |                   Spring Boot API Gateway                 |
       |      [CORS Filter]  -->  [Spring Security / JWT Filter]   |
       +------------------------------+----------------------------+
                                      | Internal Routing
                                      v
       +-----------------------------------------------------------+
       |                       Controller Layer                    |
       |      [UserController]  [AdminController]  [AuthController]|
       +------------------------------+----------------------------+
                                      | DTOs
                                      v
       +-----------------------------------------------------------+
       |                        Service Layer                      |
       |   [BookService]   [TransactionService]  [QueueMgmtService]|
       +------------------------------+----------------------------+
                                      | JPA / Repository Interfaces
                                      v
       +-----------------------------------------------------------+
       |                      DataAccess Layer                     |
       |      [BookRepository] [UserRepository] [TxnRepository]    |
       +------------------------------+----------------------------+
                                      | MongoDB Protocol
                                      v
       +-----------------------------------------------------------+
       |                     MongoDB Database                      |
       |      [books]          [users]          [transactions]     |
       +-----------------------------------------------------------+
2. Core Architecture Specifications
A. Authentication & Authorization Flow
Protocol: Stateless JWT (JSON Web Tokens).

Workflow:

The client posts credentials to /api/auth/login.

Spring Boot validates credentials, signs a JWT containing the user's ID, email, and role (ROLE_USER or ROLE_ADMIN), and returns it to the client.

The React client stores this token safely in memory or in a secure session context, attaching it as a Bearer <token> header to all outgoing Axios requests.

Spring Security intercepts incoming requests, extracts the JWT, authenticates the context, and grants or denies access to protected endpoints based on roles.

B. Transaction & Reservation State Machine
Managing books when they are in high demand requires a reliable transaction system. The diagram below illustrates how a book moves through different reservation and rental states:

                      +-------------------+
                      |     AVAILABLE     |
                      +---------+---------+
                                |
                                | Issue Book (Copies > 0)
                                v
                      +-------------------+
                      |      ISSUED       | <------------+
                      +---------+---------+              |
                                |                        |
      Copies = 0 & New Request  |                        | Admin Handover
      (User added to Queue)     v                        | (Within 48h Window)
                      +-------------------+              |
                      |  BOOKED_IN_QUEUE  |              |
                      +---------+---------+              |
                                |                        |
                                | Old Copy Returned      |
                                v                        |
                      +-------------------+              |
                      |  HELD_FOR_PICKUP  |--------------+
                      +-------------------+
3. Directory Layouts (Codebase Structure)
This structure follows standard Java package layout and React feature-based structuring.

A. Backend: Spring Boot Directory Structure
Plaintext
src/main/java/com/library/lms
├── config/                  # Security, Web MVC (CORS), & Mongo Configurations
│   ├── SecurityConfig.java
│   ├── JwtAuthenticationFilter.java
│   └── MongoConfig.java
├── controller/              # REST Endpoints
│   ├── AuthController.java
│   ├── UserController.java   # /api/user/* endpoints
│   └── AdminController.java  # /api/admin/* endpoints
├── model/                   # MongoDB Documents
│   ├── Book.java
│   ├── User.java
│   └── Transaction.java
├── repository/              # Spring Data MongoDB Repositories
│   ├── BookRepository.java
│   ├── UserRepository.java
│   └── TransactionRepository.java
├── service/                 # Business Logic
│   ├── BookService.java
│   ├── QueueMgmtService.java # Handles reservations & 48hr release rules
│   └── TransactionService.java
└── dto/                     # Data Transfer Objects
    ├── LoginRequest.java
    ├── BookResponse.java
    └── UserDto.java
B. Frontend: React Directory Structure
Plaintext
src/
├── assets/                  # CSS Stylesheets and static system icons
├── components/              # Shared UI Components
│   ├── ProtectedRoute.jsx   # Role Guard wrapper
│   ├── SearchBar.jsx
│   └── Card.jsx             # The grid layout card cell (Photo | Title | Genre)
├── context/                 # State management (Global Auth State)
│   └── AuthContext.jsx
├── services/                # Axios instance and endpoints connection mapping
│   └── api.js
└── views/                   # Dynamic Page-Level Components
    ├── auth/
    │   └── Login.jsx
    ├── user/
    │   ├── UserDashboard.jsx
    │   └── BookDetails.jsx
    └── admin/
        ├── AdminDashboard.jsx
        └── UserManagement.jsx