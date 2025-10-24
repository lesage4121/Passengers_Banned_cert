# Étape 1 : build du projet avec Maven
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Définit le répertoire de travail dans le conteneur
WORKDIR /app

# Copie le pom.xml et télécharge les dépendances
COPY pom.xml .
RUN mvn dependency:go-offline

# Copie tout le code source
COPY src ./src

# Compile et package le projet (génère le .jar)
RUN mvn clean package -DskipTests

# Étape 2 : image d'exécution
FROM eclipse-temurin:21-jdk

# Répertoire de travail pour l'application
WORKDIR /app

# Copie le jar depuis le builder
COPY --from=builder /app/target/*.jar app.jar

# Expose le port 8080 (ou celui défini dans application.properties)
EXPOSE 8080

# Commande pour lancer le backend
ENTRYPOINT ["java", "-jar", "app.jar"]
