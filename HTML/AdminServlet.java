import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/AdminServlet")
public class AdminServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String section = request.getParameter("section"); // 'companies', 'students', 'stats', 'all_opportunities'

        if ("companies".equalsIgnoreCase(section)) {
            fetchCompanies(out, response);
        } else if ("students".equalsIgnoreCase(section)) {
            fetchStudents(out, response);
        } else if ("stats".equalsIgnoreCase(section)) {
            fetchPlatformStats(out, response);
        } else if ("all_opportunities".equalsIgnoreCase(section)) {
            fetchAllOpportunities(out, response);
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Invalid admin section parameter\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String action = request.getParameter("action"); // 'login', 'add_company', 'toggle_company_status', 'update_student_status', 'delete_opportunity'

        if ("login".equalsIgnoreCase(action)) {
            handleAdminLogin(request, response, out);
        } else if ("add_company".equalsIgnoreCase(action)) {
            handleAddCompanyAccess(request, response, out);
        } else if ("toggle_company_status".equalsIgnoreCase(action)) {
            handleToggleCompany(request, response, out);
        } else if ("update_student_status".equalsIgnoreCase(action)) {
            handleUpdateStudent(request, response, out);
        } else if ("delete_opportunity".equalsIgnoreCase(action)) {
            handleDeleteOpportunity(request, response, out);
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Unknown admin action\"}");
        }
    }

    private void handleAdminLogin(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        String sql = "SELECT id, name FROM admins WHERE (username = ? OR email = ?) AND password = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, username);
            ps.setString(3, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int adminId = rs.getInt("id");
                String name = rs.getString("name");
                out.print("{\"success\":true,\"adminId\":" + adminId + ",\"name\":\"" + escapeJson(name) + "\"}");
            } else {
                out.print("{\"success\":false,\"message\":\"Invalid Admin credentials\"}");
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleAddCompanyAccess(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        String companyName = request.getParameter("companyName");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String industry = request.getParameter("industry");
        String website = request.getParameter("website");
        String adminIdStr = request.getParameter("adminId");

        if (companyName == null || email == null || password == null) {
            out.print("{\"success\":false,\"message\":\"Company Name, Email, and Password are required.\"}");
            return;
        }

        String sql = "INSERT INTO companies (company_name, email, password, industry, website, access_granted_by, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, companyName);
            ps.setString(2, email);
            ps.setString(3, password);
            ps.setString(4, industry != null ? industry : "Technology");
            ps.setString(5, website != null ? website : "");
            ps.setInt(6, adminIdStr != null ? Integer.parseInt(adminIdStr) : 1);

            ps.executeUpdate();
            out.print("{\"success\":true,\"message\":\"Company access granted successfully! Credentials created.\"}");
        } catch (SQLException e) {
            if (e.getErrorCode() == 1 || e.getMessage().toLowerCase().contains("unique")) {
                out.print("{\"success\":false,\"message\":\"A company with this email already exists!\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"success\":false,\"message\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleToggleCompany(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        String companyIdStr = request.getParameter("companyId");
        String newStatus = request.getParameter("status"); // 'ACTIVE' or 'SUSPENDED'

        String sql = "UPDATE companies SET status = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setInt(2, Integer.parseInt(companyIdStr));
            ps.executeUpdate();
            out.print("{\"success\":true,\"message\":\"Company status updated to " + newStatus + "\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleUpdateStudent(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        String studentIdStr = request.getParameter("studentId");
        String newStatus = request.getParameter("status"); // 'ACTIVE' or 'SUSPENDED'

        String sql = "UPDATE students SET status = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setInt(2, Integer.parseInt(studentIdStr));
            ps.executeUpdate();
            out.print("{\"success\":true,\"message\":\"Student status updated to " + newStatus + "\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleDeleteOpportunity(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        String oppIdStr = request.getParameter("opportunityId");

        String sql = "DELETE FROM opportunities WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, Integer.parseInt(oppIdStr));
            ps.executeUpdate();
            out.print("{\"success\":true,\"message\":\"Opportunity removed successfully\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void fetchCompanies(PrintWriter out, HttpServletResponse response) {
        String sql = "SELECT id, company_name, email, industry, website, status, TO_CHAR(created_at, 'YYYY-MM-DD') AS registered_date, " +
                     "(SELECT COUNT(*) FROM opportunities WHERE company_id = companies.id) AS postings_count " +
                     "FROM companies ORDER BY id DESC";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                    .append("\"id\":").append(rs.getInt("id")).append(",")
                    .append("\"companyName\":\"").append(escapeJson(rs.getString("company_name"))).append("\",")
                    .append("\"email\":\"").append(escapeJson(rs.getString("email"))).append("\",")
                    .append("\"industry\":\"").append(escapeJson(rs.getString("industry"))).append("\",")
                    .append("\"website\":\"").append(escapeJson(rs.getString("website"))).append("\",")
                    .append("\"status\":\"").append(rs.getString("status")).append("\",")
                    .append("\"registeredDate\":\"").append(rs.getString("registered_date")).append("\",")
                    .append("\"postingsCount\":").append(rs.getInt("postings_count"))
                    .append("}");
                first = false;
            }
            json.append("]");
            out.print(json.toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void fetchStudents(PrintWriter out, HttpServletResponse response) {
        String sql = "SELECT id, roll_no, email, name, department, gpa, status, " +
                     "(SELECT COUNT(*) FROM applications WHERE student_id = students.id) AS applications_count " +
                     "FROM students ORDER BY id DESC";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                    .append("\"id\":").append(rs.getInt("id")).append(",")
                    .append("\"rollNo\":\"").append(escapeJson(rs.getString("roll_no"))).append("\",")
                    .append("\"email\":\"").append(escapeJson(rs.getString("email"))).append("\",")
                    .append("\"name\":\"").append(escapeJson(rs.getString("name"))).append("\",")
                    .append("\"department\":\"").append(escapeJson(rs.getString("department"))).append("\",")
                    .append("\"gpa\":").append(rs.getDouble("gpa")).append(",")
                    .append("\"status\":\"").append(rs.getString("status")).append("\",")
                    .append("\"applicationsCount\":").append(rs.getInt("applications_count"))
                    .append("}");
                first = false;
            }
            json.append("]");
            out.print(json.toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void fetchPlatformStats(PrintWriter out, HttpServletResponse response) {
        String sql = "SELECT " +
                     "(SELECT COUNT(*) FROM students) AS total_students, " +
                     "(SELECT COUNT(*) FROM companies) AS total_companies, " +
                     "(SELECT COUNT(*) FROM opportunities WHERE status = 'OPEN') AS active_drives, " +
                     "(SELECT COUNT(*) FROM applications) AS total_applications " +
                     "FROM DUAL";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                out.print("{" +
                    "\"totalStudents\":" + rs.getInt("total_students") + "," +
                    "\"totalCompanies\":" + rs.getInt("total_companies") + "," +
                    "\"activeDrives\":" + rs.getInt("active_drives") + "," +
                    "\"totalApplications\":" + rs.getInt("total_applications") +
                    "}");
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void fetchAllOpportunities(PrintWriter out, HttpServletResponse response) {
        String sql = "SELECT id, company_name, title, type, duration, eligible_departments, location, compensation, min_gpa, " +
                     "TO_CHAR(deadline, 'YYYY-MM-DD') AS deadline, status, " +
                     "(SELECT COUNT(*) FROM applications WHERE opportunity_id = opportunities.id) AS applicant_count " +
                     "FROM opportunities ORDER BY id DESC";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                    .append("\"id\":").append(rs.getInt("id")).append(",")
                    .append("\"companyName\":\"").append(escapeJson(rs.getString("company_name"))).append("\",")
                    .append("\"title\":\"").append(escapeJson(rs.getString("title"))).append("\",")
                    .append("\"type\":\"").append(escapeJson(rs.getString("type"))).append("\",")
                    .append("\"duration\":\"").append(escapeJson(rs.getString("duration"))).append("\",")
                    .append("\"departments\":\"").append(escapeJson(rs.getString("eligible_departments"))).append("\",")
                    .append("\"location\":\"").append(escapeJson(rs.getString("location"))).append("\",")
                    .append("\"compensation\":\"").append(escapeJson(rs.getString("compensation"))).append("\",")
                    .append("\"minGpa\":").append(rs.getDouble("min_gpa")).append(",")
                    .append("\"deadline\":\"").append(rs.getString("deadline")).append("\",")
                    .append("\"status\":\"").append(rs.getString("status")).append("\",")
                    .append("\"applicantCount\":").append(rs.getInt("applicant_count"))
                    .append("}");
                first = false;
            }
            json.append("]");
            out.print(json.toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
