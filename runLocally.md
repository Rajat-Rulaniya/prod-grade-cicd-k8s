# Inventory Management System - Setup Guide

This guide will help you set up and run the Inventory Management System locally on your machine.

## 📋 Prerequisites

Before you begin, make sure you have the following installed on your system:

### Required Software
1. **Java 17 or higher**
   - Download from: https://adoptium.net/
   - Verify installation: `java -version`

2. **Maven 3.6 or higher**
   - Download from: https://maven.apache.org/download.cgi
   - Verify installation: `mvn -version`

3. **PostgreSQL 12 or higher**
   - Download from: https://www.postgresql.org/download/
   - Verify installation: `psql --version`

4. **Node.js 16 or higher**
   - Download from: https://nodejs.org/
   - Verify installation: `node --version`

5. **npm (comes with Node.js)**
   - Verify installation: `npm --version`

## 🗄️ Database Setup

### 1. Start PostgreSQL
Make sure PostgreSQL is running on your system:
```bash
# On Windows (if installed as service)
# PostgreSQL should start automatically

# On macOS (if installed via Homebrew)
brew services start postgresql

# On Linux
sudo systemctl start postgresql
```

### 2. Create Database
Connect to PostgreSQL and create the database:
```bash
# Connect to PostgreSQL as postgres user
psql -U postgres

# In the PostgreSQL prompt, create the database
CREATE DATABASE inventory_db;

# Verify the database was created
\l

# Exit PostgreSQL
\q
```

## 🚀 Backend Setup

### 1. Navigate to Project Directory
```bash
cd inventory-management
```

### 2. Build the Backend
```bash
# Clean and compile the project
mvn clean compile

# Run tests (optional)
mvn test

# Build the JAR file
mvn package
```

### 3. Run the Backend
```bash
# Option 1: Run using Maven
mvn spring-boot:run

# Option 2: Run the JAR file
java -jar target/inventory-management-1.0.0.jar
```

The backend will start on `http://localhost:8080`

## 🎨 Frontend Setup

### 1. Navigate to Frontend Directory
```bash
cd frontend
```

### 2. Install Dependencies
```bash
npm install
```

### 3. Start the Frontend
```bash
npm start
```

The frontend will start on `http://localhost:3000`

## 🔧 Configuration

### Database Configuration
The application is configured to use:
- **Host**: localhost
- **Port**: 5432
- **Database**: inventory_db
- **Username**: postgres
- **Password**: admin

If you need to change these settings, edit `src/main/resources/application.properties`

### Frontend Configuration
The frontend is configured to proxy requests to `http://localhost:8080`

## 👤 Default Login

After starting both applications, you can log in with:
- **Username**: admin
- **Password**: admin123

## 📱 Using the Application

### 1. Access the Application
Open your browser and go to: `http://localhost:3000`

### 2. Features Available
- **Dashboard**: View system statistics and recent orders
- **Products**: Manage inventory items (CRUD operations)
- **Orders**: Create and view orders
- **History**: Track inventory changes and activities

## 🧪 Testing

### Backend Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ProductControllerTest
```

### Frontend Tests
```bash
cd frontend
npm test
```

## 🔍 Troubleshooting

### Common Issues

#### 1. Database Connection Error
**Error**: `Connection refused` or `Authentication failed`
**Solution**: 
- Ensure PostgreSQL is running
- Verify database credentials in `application.properties`
- Check if database `inventory_db` exists

#### 2. Port Already in Use
**Error**: `Port 8080 is already in use`
**Solution**:
- Change port in `application.properties`: `server.port=8081`
- Or kill the process using the port

#### 3. Frontend Build Error
**Error**: `npm install` fails
**Solution**:
- Clear npm cache: `npm cache clean --force`
- Delete `node_modules` and `package-lock.json`
- Run `npm install` again

#### 4. CORS Error
**Error**: CORS policy blocking requests
**Solution**:
- Ensure backend is running on port 8080
- Check CORS configuration in `SecurityConfig.java`

### Logs and Debugging

#### Backend Logs
The application logs are displayed in the console where you started the backend.

#### Frontend Logs
Check the browser's Developer Tools (F12) for any JavaScript errors.

## 📁 Project Structure

```
inventory-management/
├── src/
│   ├── main/
│   │   ├── java/com/inventory/management/
│   │   │   ├── config/          # Security and JWT configuration
│   │   │   ├── controller/       # REST API endpoints
│   │   │   ├── model/           # Entity classes
│   │   │   ├── repository/      # Data access layer
│   │   │   └── service/         # Business logic
│   │   └── resources/
│   │       ├── application.properties
│   │       └── data.sql         # Seed data
│   └── test/                    # Unit tests
├── frontend/
│   ├── src/
│   │   ├── components/          # React components
│   │   ├── App.js              # Main app component
│   │   └── index.js            # Entry point
│   └── package.json
├── pom.xml                     # Maven configuration
└── howTo.md                    # This file
```

## 🔒 Security Features

- JWT-based authentication
- Password encryption using BCrypt
- CORS configuration for frontend-backend communication

## 📊 Database Schema

The application uses the following main tables:
- `users`: User authentication and profiles
- `products`: Inventory items
- `orders`: Customer orders
- `order_items`: Order details
- `inventory_history`: Audit trail for inventory changes

## 🚀 Deployment

### Backend Deployment
1. Build the JAR: `mvn clean package`
2. Deploy the JAR file to your server
3. Configure environment variables for database connection

### Frontend Deployment
1. Build the production version: `npm run build`
2. Deploy the `build` folder to your web server

## 📞 Support

If you encounter any issues:
1. Check the troubleshooting section above
2. Verify all prerequisites are installed correctly
3. Ensure all services are running on the correct ports
4. Check the application logs for error messages

## 🔄 Updates and Maintenance

### Updating Dependencies
- **Backend**: Update versions in `pom.xml`
- **Frontend**: Update versions in `package.json`

### Database Migrations
The application uses Hibernate's auto-schema generation. For production, consider using a proper migration tool like Flyway.

---

**Happy Inventory Managing! 🎉**
