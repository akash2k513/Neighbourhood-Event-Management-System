# Neighborhood Event Management System

A full-stack web application for managing neighborhood events, built with Spring Boot 3.x and React.js.

## Tech Stack

- **Backend**: Spring Boot 3.5.3, Spring Security, Spring Data JPA, JWT
- **Frontend**: React 19, Vite, Axios
- **Database**: MySQL 8.0
- **Containerization**: Docker, Docker Compose

## Prerequisites

- Java 17+
- Node.js 22+
- Docker & Docker Compose
- Maven 3.9+

## Local Setup

### Option 1 — Docker Compose (recommended)

1. Clone the repository and switch to the `dev` branch:
   ```bash
   git clone <repo-url>
   cd Neighborhood-Event-Management-System
   git checkout dev
   ```

2. Create a `.env` file in the project root:
   ```env
   MAIL_USERNAME=your-email@gmail.com
   MAIL_PASSWORD=your-app-password
   ```

3. Build the backend JAR:
   ```bash
   cd backend
   ./mvnw clean package -DskipTests
   cd ..
   ```

4. Start all services:
   ```bash
   docker-compose up --build
   ```

5. Access the services:
   - Backend API: http://localhost:8080
   - Frontend:    http://localhost:3000
   - Swagger UI:  http://localhost:8080/swagger-ui.html

### Option 2 — Run locally without Docker

**Database**

Create the MySQL database and run the schema:
```bash
mysql -u root -p < database/schema.sql
```

**Backend**
```bash
cd backend
./mvnw spring-boot:run
```

**Frontend**
```bash
cd frontend
npm install
npm run dev
```

## Environment Variables

| Variable            | Description                        | Required |
|---------------------|------------------------------------|----------|
| `DATABASE_URL`      | JDBC URL for MySQL                 | Yes      |
| `DATABASE_USERNAME` | MySQL username                     | Yes      |
| `DATABASE_PASSWORD` | MySQL password                     | Yes      |
| `JWT_SECRET`        | Secret key for JWT signing         | Yes      |
| `JWT_EXPIRATION`    | JWT expiry in ms (default 86400000)| No       |
| `MAIL_USERNAME`     | SMTP email address                 | Yes      |
| `MAIL_PASSWORD`     | SMTP app password                  | Yes      |

## Database Schema

The `database/schema.sql` file contains all 10 core tables:
`zones`, `users`, `venues`, `events`, `event_registrations`, `event_approvals`, `resources`, `resource_bookings`, `notifications`, `audit_logs`

## Project Structure

```
├── backend/          # Spring Boot application
│   └── src/main/java/com/neighborhood/eventmanagement/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── entity/
│       ├── dto/
│       ├── security/
│       ├── exception/
│       └── util/
├── frontend/         # React application
│   └── src/
│       ├── components/
│       ├── context/
│       ├── hooks/
│       ├── services/
│       ├── utils/
│       └── styles/
├── database/
│   └── schema.sql    # MySQL schema
└── docker-compose.yml
```
