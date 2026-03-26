# 🧾 Timesheet & Leave Management System (Backend)

## 📌 Overview

This project is a **microservices-based backend system** designed to manage employee timesheets, leave requests, and role-based access within an organization.

It replaces manual tracking with a scalable, secure, and structured system built using **Spring Boot and Spring Cloud**.

---

## 🚀 Tech Stack

* **Java 17**
* **Spring Boot**
* **Spring Cloud (Eureka, API Gateway)**
* **Spring Security**
* **JWT (Authentication)**
* **Spring Data JPA (Hibernate)**
* **MySQL**
* **Swagger / OpenAPI**
* **RabbitMQ (planned)**
* **JUnit & Mockito (planned)**

---

## 🏗️ Architecture

This project follows a **microservices architecture** with centralized routing and security.

### 🔹 Services

| Service Name          | Description                                                           |
| --------------------- | --------------------------------------------------------------------- |
| **Auth Service**      | Handles user registration, login, JWT generation, and role management |
| **Timesheet Service** | Manages daily entries and weekly timesheets                           |
| **Leave Service**     | Handles leave application and validation                              |
| **Admin Service**     | Manages administrative operations and approvals                       |

---

### 🔹 Infrastructure

* **Eureka Server** → Service discovery
* **API Gateway** → Centralized routing & authentication

---

## 🔐 Security Architecture

* JWT is **generated in Auth Service**

* JWT is **validated in API Gateway**

* Gateway forwards user details via headers:

  * `X-User-Email`
  * `X-User-Role`
  * `X-User-Id`

* Downstream services **do not validate JWT again**

* Role-based access is enforced using `@PreAuthorize`

---

## 🔄 System Flow

```text
Client → API Gateway → Microservice → Database
```

---

## ✅ Features Implemented

### 🔹 Authentication

* User registration (default role: EMPLOYEE)
* Login with JWT token
* Role-based access control
* Admin-only role promotion

---

### 🔹 Timesheet Service

* Add daily entries
* Weekly timesheet submission
* View timesheet history
* Validation (hours, duplicate entries)

---

### 🔹 Leave Service

* Apply leave
* Leave validation (date range, overlap)
* Leave history & balance

---

### 🔹 Admin Service

* Manage system-level operations
* Role-based restrictions

---

### 🔹 API Documentation

* Swagger integrated for all services
* Easy API testing and exploration

---

## 📬 API Testing

### 🔹 Swagger

Example:

```
http://localhost:8082/swagger-ui.html
```

### 🔹 Postman (via Gateway)

```
http://localhost:8080/<service-endpoint>
```

---

## ▶️ How to Run

### 1. Start services in order:

1. Eureka Server (8761)
2. API Gateway (8080)
3. Auth Service (8081)
4. Timesheet Service (8082)
5. Leave Service (8083)
6. Admin Service (8084)

---

### 2. Database

* MySQL running on port `3306`
* Databases auto-created

---

### 3. Verify Eureka

```
http://localhost:8761
```

---

## 📁 Project Structure

```
timesheet-leave-management-system/
│
├── eureka-server/
├── api-gateway/
├── auth-service/
├── timesheet-service/
├── leave-service/
├── admin-service/
│
├── docs/
├── README.md
```

---

## 🧠 Key Design Decisions

* Centralized authentication at API Gateway
* No JWT validation in downstream services
* Role-based authorization using Spring Security
* Clean separation of microservices

---

## 🎯 Project Goal

* Demonstrate real-world **microservices architecture**
* Implement secure backend using **JWT & Spring Security**
* Build scalable and maintainable backend system

---

## 📌 Future Enhancements

* RabbitMQ for async communication
* Docker containerization
* CI/CD pipeline
* Email notifications
* Frontend integration

---

## 👨‍💻 Author

Developed as part of Capgemini training and evaluation.

---
