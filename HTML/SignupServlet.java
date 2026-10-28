
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;

@WebServlet("/SignupServlet")
public class SignupServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        // Read all six fields from the signup form
        String rollNo = request.getParameter("roll_no");
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String department = request.getParameter("department");
        String gpaValue = request.getParameter("gpa");

        // Check for missing fields
        if (rollNo == null || name == null || email == null
                || password == null || department == null
                || gpaValue == null
                || rollNo.trim().isEmpty()
                || name.trim().isEmpty()
                || email.trim().isEmpty()
                || password.isEmpty()
                || department.trim().isEmpty()
                || gpaValue.trim().isEmpty()) {

            response.sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                "All six fields are required."
            );
            return;
        }

        rollNo = rollNo.trim();
        name = name.trim();
        email = email.trim();
        department = department.trim();

        // Validate field lengths
        if (rollNo.length() > 20 || name.length() > 100
                || email.length() > 100 || department.length() > 50) {
            response.sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                "One or more fields exceed the allowed length."
            );
            return;
        }

        // Validate email format
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            response.sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                "Please enter a valid email address."
            );
            return;
        }

        // Validate password
        String passwordPattern =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])"
            + "(?=.*[^A-Za-z0-9]).{8,72}$";

        if (!password.matches(passwordPattern)) {
            response.sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                "Password must contain uppercase, lowercase, "
                + "a number, a special character, and 8-72 characters."
            );
            return;
        }

        // Convert and validate CGPA
        BigDecimal gpa;

        try {
            gpa = new BigDecimal(gpaValue.trim());

            if (gpa.compareTo(BigDecimal.ZERO) < 0
                    || gpa.compareTo(BigDecimal.TEN) > 0
                    || gpa.scale() > 2) {
                response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "CGPA must be between 0 and 10, with at most 2 decimal places."
                );
                return;
            }

        } catch (NumberFormatException e) {
            response.sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                "Please enter a valid CGPA."
            );
            return;
        }

        String checkSQL =
            "SELECT roll_no FROM students WHERE email = ? OR roll_no = ?";

        String insertSQL =
            "INSERT INTO students "
            + "(roll_no, email, password, name, department, gpa) "
            + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection()) {

            // Check whether the email or roll number already exists
            try (PreparedStatement checkPS =
                    con.prepareStatement(checkSQL)) {

                checkPS.setString(1, email);
                checkPS.setString(2, rollNo);

                try (ResultSet rs = checkPS.executeQuery()) {
                    if (rs.next()) {
                        response.sendRedirect(
                            request.getContextPath() + "/Wrong.html"
                        );
                        return;
                    }
                }
            }

            // Insert all six student details
            try (PreparedStatement ps =
                    con.prepareStatement(insertSQL)) {

                ps.setString(1, rollNo);
                ps.setString(2, email);
                ps.setString(3, password);
                ps.setString(4, name);
                ps.setString(5, department);
                ps.setBigDecimal(6, gpa);

                int rows = ps.executeUpdate();

                if (rows > 0) {
                    response.sendRedirect(
                        request.getContextPath() + "/LoginStudent.html"
                    );
                } else {
                    response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Registration failed. Please try again."
                    );
                }
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();

            response.sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Database error. Check the server logs."
            );
        }
    }
}
