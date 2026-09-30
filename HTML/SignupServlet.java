import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.*;

@WebServlet("/SignupServlet")
public class SignupServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                           HttpServletResponse response)
                           throws ServletException, IOException {

        // 1. Get data from HTML form
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        // 2. Oracle database details
        String url = "jdbc:oracle:thin:@localhost:1521/XEPDB1";
        String username = "placement_db";
        String dbPassword = "placement@1234";

        try {

            // 3. Create database connection
            Connection con = DriverManager.getConnection(
                url,
                username,
                dbPassword
            );
            // 4. SQL query
            String sql = "INSERT INTO STUDENTS VALUES(?,?)";

            String check = "SELECT * FROM STUDENTS WHERE EMAIL=?";

            // 5. Prepare query
            PreparedStatement ps1 = con.prepareStatement(check);

            // 6. Put form values into ?
            ps1.setString(1,email);

            // 7. Execute SELECT query
            
            ResultSet rs = ps1.executeQuery();

            // 8. Check whether login exists
            if (!rs.next()) {

                // Login successful
                PreparedStatement ps = con.prepareStatement(sql);

                 ps.setString(1, email);
                 ps.setString(2, password);

                int rows = ps.executeUpdate();
                response.sendRedirect(request.getContextPath() + "/HTML/Student.html");
                ps.close();
            } 
            else{

                // Login failed
                response.sendRedirect(request.getContextPath() + "/HTML/Wrong.html");
            }


            // 9. Close resources
            //ps.close();
            ps1.close();
            rs.close();
            con.close();

        } 
        catch (SQLException e) {
            e.printStackTrace();
            response.getWriter().println(
                "Database error: " + e.getMessage()
                );
        }
    }
}