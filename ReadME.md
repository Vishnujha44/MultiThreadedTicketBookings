# Multi Threaded TIcket Bookings

A Java-based application for booking railway tickets, featuring GUI and console interfaces, MySQL database, and multi-threaded processing. Built with Maven, JDK 17, and IntelliJ IDEA.

## Features
- **Book Tickets**: Reserve seats with optional seat selection.
- **Cancel Bookings**: Free up booked seats.
- **View Seat Map**: Display available/booked seats.
- **Booking History**: Review all bookings with status.
- **Random Bookings**: Simulate bookings for testing.
- **Waitlist Management**: Auto-process waitlisted bookings when seats are available.
- **Analytics**: View booking statistics (confirmed, waitlisted, cancelled).
- **Export**: Save booking history to CSV.
- **Notifications**: Simulated email confirmations.
- **Robustness**: Handles invalid inputs and database failures with logging.

## Prerequisites
- **JDK 17**: Install from [Oracle](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html).
- **MySQL 8.0+**: Install from [MySQL](https://dev.mysql.com/downloads/installer/).
- **IntelliJ IDEA**: Ultimate Edition recommended for Database tools.
- **Maven**: Included with IntelliJ or install separately.

## Setup Instructions


- Project structure:

MultiThreadedTIcketBookings/ ├── .idea/ ├── src/main/java/com/railway/ ├── src/main/resources/ │ ├── db.properties │ ├── pom.xml └── README.md


### 2. Configure MySQL
- Start MySQL server:
- **Linux**: `sudo systemctl start mysql`
- **Windows**: Start `MySQL80` in Services.
- **macOS**: `brew services start mysql`
- Update `src/main/resources/db.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/railway_db?useSSL=false
db.user=root
db.password=12345678
total.seats=20





Run schema.sql:

mysql -u root -p < src/main/resources/schema.sql

3. 
Open IntelliJ IDEA.



File > Open, select MultiThreadedTIcketBookings .



Sync Maven: Right-click pom.xml > Maven > Reload Project.



Set JDK 17: File > Project Structure > Project > Project SDK.

4. Configure Database in IntelliJ





View > Tool Windows > Database.



Add MySQL data source:





Name: railway_db



Host: localhost



Port: 3306



User: root



Password: 12345678



Test connection.



Run schema.sql:





Right-click src/main/resources/schema.sql > Run > Select railway_db.

5. Run Application





Open src/main/java/com/railway/Main.java.



Right-click > Run 'Main.main()'.



Choose:





GUI Mode: Enter y.



Console Mode: Enter n.

Usage





GUI:





Enter username and seats.



Select seats via checkboxes (optional).



Use buttons: Book Tickets, Cancel Booking, View Seat Map, View Booking History, View Booking Stats, Export Bookings.



Console:





Select options (1–8) to book, cancel, view stats, etc.



Output:





GUI: Text area.



Console: Terminal.



Logs: Console (via logback.xml).

Database Schema





Database: railway_db



Tables:





bookings:





id: Auto-incremented primary key.



user_name: VARCHAR(50).



seats_booked: INT.



booking_time: TIMESTAMP.



status: ENUM(CONFIRMED, WAITLISTED, CANCELLED).



seats:





seat_id: Primary key.



is_booked: BOOLEAN.



booking_id: Foreign key to bookings.id.

Troubleshooting





Access Denied:

GRANT ALL PRIVILEGES ON railway_db.* TO 'root'@'localhost';
FLUSH PRIVILEGES;



Connection Refused:





Ensure MySQL is running.



Check db.properties URL.



Database Not Found:





Rerun schema.sql.

License

MIT License.
