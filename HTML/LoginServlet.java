import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.*;

@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {

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
            String onecrct = "SELECT * FROM students " +
                         "WHERE email = ? AND password <> ?";

            // 4. SQL query
            String sql = "SELECT * FROM students " +
                         "WHERE email = ? AND password = ?";

            // 5. Prepare query
            PreparedStatement ps = con.prepareStatement(sql);
            PreparedStatement ps1 = con.prepareStatement(onecrct);

            // 6. Put form values into ?
            ps.setString(1, email);
            ps.setString(2, password);

            ps1.setString(1, email);
            ps1.setString(2, password);

            // 7. Execute SELECT query
            ResultSet rs = ps.executeQuery();
            ResultSet rs1 = ps1.executeQuery();

            // 8. Check whether login exists
            if (rs.next()) {

                // Login successful
                response.sendRedirect(request.getContextPath() + "/HTML/Student.html");

            } 
            else if(rs1.next()) {

                // Login successful
                response.getWriter().println(
                    "Incorrect password"
                );

            } 
            else {

                // Login failed
                response.getWriter().println(
                    "No Login exist!"
                );
            }


            // 9. Close resources
            rs.close();
            rs1.close();
            ps1.close();
            ps.close();
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