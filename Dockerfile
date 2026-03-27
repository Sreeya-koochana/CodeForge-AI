FROM node:22-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-17 AS backend-build
WORKDIR /app
COPY backend/pom.xml backend/
COPY backend/.mvn/ backend/.mvn/
COPY backend/mvnw backend/mvnw
COPY backend/mvnw.cmd backend/mvnw.cmd
RUN mvn -f backend/pom.xml -q -DskipTests dependency:go-offline
COPY backend/src/ backend/src/
COPY --from=frontend-build /app/frontend/dist/ backend/src/main/resources/static/
RUN mvn -f backend/pom.xml -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend-build /app/backend/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
