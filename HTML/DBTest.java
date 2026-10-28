
import java.sql.Connection;

public class DBTest {
    public static void main(String[] args) {
        try {
            Connection con = DBConnection.getConnection();

            System.out.println("MySQL connection successful!");
            System.out.println("Database: " +
                    con.getCatalog());

            con.close();
        } catch (Exception e) {
            System.out.println("MySQL connection failed!");
            e.printStackTrace();
        }
    }
}
