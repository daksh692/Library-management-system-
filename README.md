# Library Management System

A comprehensive, full-stack Library Management System built with Spring Boot (Java) and React. This application provides a complete solution for managing library resources, user waitlists, borrowing processes, and penalty tracking, along with a sleek and editorial-themed user interface.

## 🚀 Features

### For Users ("Quiet Reading Room")
* **Book Discovery:** Browse and search for available books.
* **Waitlists & Queuing:** Automatically join queues for currently unavailable books (`BOOKED_IN_QUEUE`).
* **Holds & Pickup:** Automated 48-hour hold window for picking up books once they become available (`HELD_FOR_PICKUP`).
* **Active Checks & Returns:** Track currently borrowed books (`ISSUED`).
* **Notifications:** Real-time alert banners for updates and notifications.

### For Administrators ("Admin Command Center")
* **Inventory Management:** Add, update, and remove books from the library catalog.
* **Transaction Monitoring:** View all borrowing transactions and waitlists.
* **Penalty Engine:** Automated calculation and application of penalties for overdue returns.
* **Role-Based Access Control:** Secure routes and actions protected by JWT-based authentication.

### Technical Highlights
* **Security First:** Stateless JWT validation, Bucket4j Rate Limiting, and robust global exception handling to sanitize stack traces and prevent information leakage.
* **Modern UI:** Built with React 19, Vite, and Tailwind CSS (v4) to deliver a modern, visually stunning interface.
* **Robust Backend:** Powered by Java 17, Spring Boot 3.2, and MongoDB for scalable data storage and fast querying.

## 🛠️ Tech Stack

### Backend
* **Java 17**
* **Spring Boot 3.2.3**
* **MongoDB** (Spring Data MongoDB)
* **Spring Security & JWT (io.jsonwebtoken)**
* **Bucket4j** (Rate Limiting)
* **Lombok**
* **JUnit 5 & Mockito** (Testing)

### Frontend
* **React 19**
* **Vite**
* **Tailwind CSS 4.3**
* **Axios** (with custom interceptors)
* **React Router DOM**
* **Lucide React** (Icons)
* **Oxlint** (Linting)

## 📦 Project Structure

The project is structured as a monorepo containing both the backend and frontend:

- `/backend` - Contains the Java Spring Boot API, configurations, services, and tests.
- `/frontend` - Contains the React single-page application and its components.

## ⚙️ Getting Started

### Prerequisites
Make sure you have the following installed on your machine:
* [Java Development Kit (JDK) 17](https://jdk.java.net/17/)
* [Node.js (v18 or higher)](https://nodejs.org/) & npm
* [MongoDB](https://www.mongodb.com/) (running locally or a connection URI for MongoDB Atlas)
* [Maven](https://maven.apache.org/)

### Setting up the Backend

1. Navigate to the backend directory:
   ```bash
   cd backend
   ```
2. Make sure MongoDB is running and update `application.properties` or `application.yml` (if needed) with your database connection details.
3. Start the Spring Boot server:
   ```bash
   mvn spring-boot:run
   ```
   The backend API will start on `http://localhost:8080`.

### Setting up the Frontend

1. Open a new terminal window and navigate to the frontend directory:
   ```bash
   cd frontend
   ```
2. Install the frontend dependencies:
   ```bash
   npm install
   ```
3. Start the Vite development server:
   ```bash
   npm run dev
   ```
   The frontend application will typically be available at `http://localhost:5173`.

## 🧪 Testing
The backend features extensive JUnit 5 test suites utilizing Mockito to secure logic and penalty edge-cases. To run the tests:
```bash
cd backend
mvn test
```

## 🔒 Security & Architecture
- **JWT Authentication:** Tokens are generated upon login and passed via HTTP headers for all secured endpoints.
- **Custom Axios Interceptors:** Ensure tokens are automatically attached to requests in the frontend and handle unauthorized states gracefully.
- **Rate Limiting:** Protects key endpoints from abuse using Bucket4j.

## 🤝 Contributing
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request