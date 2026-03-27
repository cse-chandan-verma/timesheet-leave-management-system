# 🧾 Timesheet & Leave Management System (Backend)

## 📌 Overview

This project is a **microservices-based backend system** designed to manage employee timesheets, leave requests, and role-based access within an organization.

It replaces manual tracking with a scalable, secure, and distributed system built using **Spring Boot and Spring Cloud**, with support for messaging and tracing.

---

## 🚀 Tech Stack

* Java 17
* Spring Boot
* Spring Cloud (Eureka, API Gateway)
* Spring Security
* JWT (Authentication)
* Spring Data JPA (Hibernate)
* MySQL
* Swagger / OpenAPI
* RabbitMQ (Asynchronous Messaging)
* Zipkin (Distributed Tracing)
* Docker & Docker Compose
* JUnit & Mockito

---

## 🏗️ Architecture

This project follows a **microservices architecture** with centralized routing, messaging, and observability.

### 🔹 Services

| Service Name         | Description                                                           |
| -------------------- | --------------------------------------------------------------------- |
| Auth Service         | Handles user registration, login, JWT generation, and role management |
| Timesheet Service    | Manages daily entries and weekly timesheets                           |
| Leave Service        | Handles leave application, validation, and balances                   |
| Admin Service        | Manages administrative operations and approvals                       |
| Notification Service | Handles email notifications using RabbitMQ events                     |

---

### 🔹 Infrastructure

* Eureka Server → Service discovery
* API Gateway → Centralized routing & authentication
* RabbitMQ → Asynchronous communication between services
* Zipkin → Distributed tracing and monitoring
* MySQL → Database

---

## 🔐 Security Architecture

* JWT is generated in Auth Service
* JWT is validated in API Gateway
* Gateway forwards user details via headers:

  * X-User-Email
  * X-User-Role
  * X-User-Id
* Downstream services trust gateway headers
* Role-based access enforced using `@PreAuthorize`

---

## 🔄 System Flow

Client → API Gateway → Microservice → Database
↓
RabbitMQ → Notification Service → Email

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
* Leave history & balance tracking

---

### 🔹 Admin Service

* Manage system-level operations
* Role-based restrictions

---

### 🔹 Notification System

* Event-driven communication using RabbitMQ
* Email notifications on leave actions
* Decoupled notification service

---

### 🔹 Observability

* Zipkin integration for distributed tracing
* Track request flow across microservices

---

### 🔹 API Documentation

* Swagger integrated for all services
* Easy API testing

---

## 📬 API Testing

### Swagger Example

http://localhost:8080/swagger-ui.html

### Gateway Access

http://localhost:8080/<service-endpoint>

---

## ▶️ How to Run

### Using Docker (Recommended)

docker-compose up --build

### Ports

| Service       | Port  |
| ------------- | ----- |
| Eureka Server | 8761  |
| API Gateway   | 8080  |
| Auth Service  | 8081  |
| Timesheet     | 8082  |
| Leave Service | 8083  |
| Admin Service | 8084  |
| Zipkin        | 9411  |
| RabbitMQ UI   | 15672 |

---

## 📁 Project Structure

timesheet-leave-management-system/
├── eureka-server/
├── api-gateway/
├── auth-service/
├── timesheet-service/
├── leave-service/
├── admin-service/
├── notification-service/
├── docker-compose.yml
├── README.md

---

## 🧠 Key Design Decisions

* Centralized authentication at API Gateway
* No JWT validation in downstream services
* Role-based authorization using Spring Security
* Event-driven communication using RabbitMQ
* Distributed tracing using Zipkin
* Clean separation of microservices

---

## 🎯 Project Goal

* Demonstrate real-world microservices architecture
* Implement secure backend using JWT & Spring Security
* Showcase event-driven communication
* Build scalable backend system

---

## 📌 Future Enhancements

* CI/CD pipeline
* Advanced monitoring (Prometheus + Grafana)
* Rate limiting
* Frontend integration

---

## 👨‍💻 Author

Developed as part of Capgemini training and evaluation.
