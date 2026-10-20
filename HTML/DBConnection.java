import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // Oracle JDBC configuration matching LoginServlet.java
    private static final String URL = "jdbc:oracle:thin:@localhost:1521/XEPDB1";
    private static final String USER = "placement_db";
    private static final String PASSWORD = "placement@1234";

    // Alternative MySQL Configuration:
    // private static final String URL = "jdbc:mysql://localhost:3306/placement_db?useSSL=false&serverTimezone=UTC";
    // private static final String USER = "root";
    // private static final String PASSWORD = "password";

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        // Load driver if required
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            // Fallback for MySQL if running on MySQL
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException ignored) {}
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
