# 🧾 Timesheet & Leave Management System (Backend)

## 📌 Overview

This project is a **Timesheet & Leave Management System** designed to help organizations manage employee work hours, leave requests, and approval workflows in a structured and efficient way.

It replaces manual processes like spreadsheets and email-based approvals with a centralized backend system built using **Spring Boot Microservices architecture**.

---

## 🚀 Tech Stack

* **Java 17**
* **Spring Boot**
* **Spring Cloud (Eureka, API Gateway)**
* **Spring Security + JWT**
* **Spring Data JPA (Hibernate)**
* **MySQL**
* **RabbitMQ** (planned)
* **JUnit & Mockito** (planned)
* **Swagger/OpenAPI** (planned migration from Postman)

---

## 🏗️ Architecture

This project follows a **microservices-based architecture** with service discovery and centralized routing.

### 🔹 Services

| Service Name                  | Description                                                   |
| ----------------------------- | ------------------------------------------------------------- |
| **Auth Service**              | Handles authentication, JWT generation, and role-based access |
| **Timesheet Service**         | Manages employee work entries and timesheet submission        |
| **Leave Service** *(Planned)* | Handles leave requests and balance tracking                   |
| **Admin Service** *(Planned)* | Manages approvals, reports, and master data                   |

---

### 🔹 Infrastructure Components

* **Eureka Server** → Service discovery
* **API Gateway** → Single entry point for all requests

---

## 🔄 System Flow

```text
Client → API Gateway → Microservice → Database
```

* All services register with **Eureka**
* Requests pass through **API Gateway**
* Gateway routes requests to appropriate service
* JWT is used for authentication and authorization

---

## 🔐 Security

* Implemented using **Spring Security + JWT**
* Role-based access:

  * **Employee**
  * **Manager**
  * **Admin**
* Token-based authentication for secure API access

---

## ✅ Features Implemented

### 🔹 Auth Service

* User registration
* Login with JWT token generation
* Role-based authentication setup

### 🔹 Timesheet Service

* Add daily work entries
* Store timesheet data in MySQL
* JPA/Hibernate integration
* Basic validation support

### 🔹 Infrastructure

* Eureka Server configured
* API Gateway routing setup
* Service registration and communication working

---

## 🚧 Features In Progress

* Leave Service (apply leave, balance tracking)
* Admin Service (approvals & reporting)
* RabbitMQ integration (async communication)
* Unit testing using JUnit & Mockito
* Advanced validation (duplicate entries, weekly checks)

---

## 🗄️ Database

* MySQL is used as the primary database
* Each microservice manages its own data
* JPA/Hibernate is used for ORM
* Auto schema update enabled (`ddl-auto=update`)

---

## 📬 API Testing

* APIs are currently tested using **Postman**
* Swagger UI (Springdoc OpenAPI) will be used for interactive API documentation

Example:

```text
http://localhost:8082/swagger-ui.html
```

---

## ▶️ How to Run the Project

### 1. Start Services in Order:

1. Eureka Server
2. API Gateway
3. Auth Service
4. Timesheet Service

---

### 2. Database Setup

* Ensure MySQL is running on port `3306`
* Database will be auto-created if not present

---

### 3. Run Application

* Run each service as a Spring Boot application
* Verify services are registered in Eureka Dashboard:

```text
http://localhost:8761
```

---

## 📁 Project Structure (Per Service)

```text
controller → handles API requests
service → business logic
repository → database interaction
entity → database models
dto → request/response models
config → security/configuration classes
```

---

## 🧪 Testing (Planned)

* Unit testing using **JUnit 5**
* Mocking using **Mockito**
* Focus on:

  * Service layer logic
  * Validation rules

---

## 🎯 Project Goal

The goal of this project is to demonstrate:

* Real-world **microservices architecture**
* Secure backend using **JWT authentication**
* Proper **service separation and communication**
* Scalable and maintainable backend design

---

## 📌 Future Enhancements

* RabbitMQ for event-driven communication
* Docker containerization
* Advanced reporting APIs
* Notification system
* Integration with frontend

---

## 👨‍💻 Author

Developed as part of training and evaluation project.

---
