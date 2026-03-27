$ErrorActionPreference = "Stop"

Write-Host "========================================="
Write-Host "Building All Spring Boot Microservices..."
Write-Host "========================================="

Write-Host "`n1. Building Eureka Server..."
Push-Location eureka-server
.\mvnw.cmd clean install -DskipTests
Pop-Location

Write-Host "`n2. Building API Gateway..."
Push-Location api-gateway
.\mvnw.cmd clean install -DskipTests
Pop-Location

Write-Host "`n3. Building Auth Service..."
Push-Location auth-service
.\mvnw.cmd clean install -DskipTests
Pop-Location

Write-Host "`n4. Building Admin Service..."
Push-Location admin-service
.\mvnw.cmd clean install -DskipTests
Pop-Location

Write-Host "`n5. Building Leave Service..."
Push-Location leave-service
.\mvnw.cmd clean install -DskipTests
Pop-Location

Write-Host "`n6. Building Timesheet Service..."
Push-Location timesheet-service
.\mvnw.cmd clean install -DskipTests
Pop-Location

Write-Host "`n7. Building Notification Service..."
Push-Location notification-service
.\mvnw.cmd clean install -DskipTests
Pop-Location

Write-Host "`n========================================="
Write-Host "All builds complete successfully!"
Write-Host "You can now package and run via Docker Compose using:"
Write-Host "docker-compose up --build -d"
Write-Host "========================================="
