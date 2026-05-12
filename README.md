
  OPERIX — Equipment Booking & Fault Tracking System


--------------------------------------------------------------
  PROJECT OVERVIEW
--------------------------------------------------------------
Operix is a JavaFX desktop application for managing classroom
equipment bookings and fault tracking. It supports three roles:
Teacher, Lab Manager, and Technician.

Architecture : 3-Layer (UI → Service → DAO)
UI Framework : JavaFX
Build Tool   : Apache Maven 3.8+
Language     : Java 17 (LTS)
Database     : Microsoft SQL Server (Express Edition)

--------------------------------------------------------------
  PREREQUISITES — INSTALL THESE FIRST
--------------------------------------------------------------

1. Java 17 (LTS)
   Download : https://adoptium.net
   After install, verify in terminal:
     java -version
   Should show: openjdk 17.x.x

2. Apache Maven 3.8+
   Download : https://maven.apache.org/download.cgi
   After install, verify in terminal:
     mvn -version
   Should show: Apache Maven 3.8.x

3. Visual Studio Code
   Download : https://code.visualstudio.com
   
   Install these VS Code Extensions:
   - Extension Pack for Java  (by Microsoft)
   - Maven for Java           (by Microsoft)
   - Language Support for Java (by Red Hat)

4. Microsoft SQL Server Express
   Download : https://www.microsoft.com/en-us/sql-server/sql-server-downloads
   Choose   : Express (free edition)
   
   Also install SQL Server Management Studio (SSMS):
   Download : https://aka.ms/ssmsfullsetup

--------------------------------------------------------------
  STEP 1 — DATABASE SETUP (DO THIS FIRST)
--------------------------------------------------------------

1. Open SQL Server Management Studio (SSMS)

2. Connect to your SQL Server instance
   Server name : YOUR_PC_NAME\SQLEXPRESS
   Auth type   : SQL Server Authentication
                 OR Windows Authentication

3. Open a the YOURPATH\equipment_project\src\main\resources\schema.sql window and run this SQL script to create the database and all tables.

--------------------------------------------------------------
  STEP 2 — CREATE SQL SERVER LOGIN (if using SQL Auth)
--------------------------------------------------------------

Run this in SSMS to create the app_user login:

------------------------------------------------------------
USE master;
GO
CREATE LOGIN app_user WITH PASSWORD = 'your_password';
GO

USE equipment_booking_system;
GO
CREATE USER app_user FOR LOGIN app_user;
GO
ALTER ROLE db_owner ADD MEMBER app_user;
GO
------------------------------------------------------------

--------------------------------------------------------------
  STEP 3 — CONFIGURE DATABASE CONNECTION
--------------------------------------------------------------

Open this file in the project:
  src/main/java/com/equipment/persistence/DatabaseConnection.java

Find these lines and update to match YOUR machine:

  private static final String SERVER   = "YOUR_PC_NAME\\SQLEXPRESS";
  private static final String DATABASE = "equipment_booking_system";
  private static final String USER     = "app_user";
  private static final String PASSWORD = "YOUR_PASSWORD";

  CHANGE SERVER to your own PC name, for example:
  "YOUR_PC_NAME\\SQLEXPRESS"

  To find your PC name: open Command Prompt and type:
    hostname
  That output is your PC name.

--------------------------------------------------------------
  STEP 4 — OPEN PROJECT IN VS CODE
--------------------------------------------------------------

1. Open Visual Studio Code

2. Click File → Open Folder

3. Select the root folder of this project
   (the folder that contains pom.xml)

4. VS Code will detect it as a Maven project automatically
   Wait for Java indexing to complete (bottom status bar)

--------------------------------------------------------------
  STEP 5 — CHECK pom.xml DEPENDENCIES
--------------------------------------------------------------

Make sure your pom.xml contains these dependencies.
If any are missing, add them:

------------------------------------------------------------
<!-- JavaFX -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>17.0.6</version>
</dependency>
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-fxml</artifactId>
    <version>17.0.6</version>
</dependency>

<!-- SQL Server JDBC Driver -->
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <version>12.4.2.jre11</version>
</dependency>

<!-- JavaFX Maven Plugin -->
<plugin>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>0.0.8</version>
    <configuration>
        <mainClass>com.equipment.ui.MainApp</mainClass>
    </configuration>
</plugin>
------------------------------------------------------------

--------------------------------------------------------------
  STEP 6 — RUN THE APPLICATION
--------------------------------------------------------------

OPTION A — Using VS Code Terminal:
  1. Open terminal in VS Code (Ctrl + `)
  2. Type:
       mvn javafx:run
  3. Press Enter
  4. The login screen will appear

OPTION B — Using VS Code Maven Panel:
  1. Click the Maven icon in the left sidebar
  2. Expand your project
  3. Expand Plugins → javafx
  4. Click "javafx:run"

--------------------------------------------------------------
  SAMPLE LOGIN CREDENTIALS
--------------------------------------------------------------

  Role          Username    Password
  ----------    ---------   -----------
  Teacher       teacher1    pass123
  Teacher       teacher2    pass123
  Lab Manager   manager1    manager123
  Technician    tech1       tech123
  Technician    tech2       tech123

NOTE: These match the seed data inserted in Step 1.
      If you used different passwords during seeding,
      use those passwords here instead.

--------------------------------------------------------------
  PROJECT FOLDER STRUCTURE
--------------------------------------------------------------

```
  operix/
  ├── pom.xml
  └── src/
      └── main/
          ├── java/
          │   └── com/equipment/
          │       ├── Main.java                  (console demo)
          │       ├── model/                     (domain entities)
          │       │   ├── User.java
          │       │   ├── Booking.java
          │       │   ├── Equipment.java
          │       │   ├── FaultReport.java
          │       │   ├── MaintenanceTask.java
          │       │   └── AuditLog.java
          │       ├── service/                   (business logic)
          │       │   ├── AuthService.java
          │       │   ├── EquipmentService.java
          │       │   ├── FaultService.java
          │       │   └── LabManagerService.java
          │       ├── dao/                       (database layer)
          │       │   ├── UserDAO.java
          │       │   ├── BookingDAO.java
          │       │   ├── EquipmentDAO.java
          │       │   ├── FaultReportDAO.java
          │       │   ├── MaintenanceTaskDAO.java
          │       │   └── AuditLogDAO.java
          │       ├── persistence/
          │       │   └── DatabaseConnection.java
          │       ├── util/
          │       │   ├── PasswordUtil.java
          │       │   └── ValidationUtil.java
          │       └── ui/                        (JavaFX UI layer)
          │           ├── MainApp.java
          │           ├── SessionManager.java
          │           └── controller/
          │               ├── LoginController.java
          │               ├── TeacherDashboardController.java
          │               ├── LabManagerDashboardController.java
          │               └── TechnicianDashboardController.java
          └── resources/
              └── com/equipment/ui/
                  ├── fxml/
                  │   ├── login.fxml
                  │   ├── dashboard_teacher.fxml
                  │   ├── dashboard_manager.fxml
                  │   └── dashboard_technician.fxml
                  └── css/
                      └── style.css

```
--------------------------------------------------------------
  COMMON ERRORS AND FIXES
--------------------------------------------------------------

ERROR: "DB Connection failed"
FIX  : Check SERVER name in DatabaseConnection.java
       Make sure SQL Server service is running:
       Open Services (Win+R → services.msc) →
       find "SQL Server (SQLEXPRESS)" → Start it

ERROR: "JDBC Driver missing"
FIX  : Run: mvn clean install
       This downloads the mssql-jdbc driver from Maven

ERROR: "JavaFX runtime components are missing"
FIX  : Do NOT run with "java -jar"
       Always run with: mvn javafx:run

ERROR: Login says "Invalid username or password"
FIX  : Make sure you ran the seed INSERT statements
       in Step 1 and are using the correct passwords
       listed in the credentials table above

ERROR: "Cannot connect to SQL Server"
FIX  : In SSMS → right click server → Properties →
       Security → enable "SQL Server and Windows
       Authentication mode" → restart SQL Server service
--------------------------------------------------------------
  Documentation
--------------------------------------------------------------
SRS Document : A complete picture of all te implied usecases and other calculations are added into this 
Validation Document : Complete project is validated by other stalkholders 
==============================================================
  END OF README
==============================================================
