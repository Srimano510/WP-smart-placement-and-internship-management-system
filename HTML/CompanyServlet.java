import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/CompanyServlet")
public class CompanyServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // View current company's postings
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String companyIdStr = request.getParameter("companyId");
        if (companyIdStr == null) {
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("companyId") != null) {
                companyIdStr = session.getAttribute("companyId").toString();
            }
        }

        if (companyIdStr == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"Company not authenticated\"}");
            return;
        }

        int companyId = Integer.parseInt(companyIdStr);
        String sql = "SELECT o.id, o.title, o.type, o.duration, o.eligible_departments, o.location, " +
                     "o.compensation, o.min_gpa, TO_CHAR(o.deadline, 'YYYY-MM-DD') AS deadline, " +
                     "TO_CHAR(o.drive_date, 'YYYY-MM-DD') AS drive_date, o.status, " +
                     "(SELECT COUNT(*) FROM applications a WHERE a.opportunity_id = o.id) AS applicant_count " +
                     "FROM opportunities o WHERE o.company_id = ? ORDER BY o.id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, companyId);
            ResultSet rs = ps.executeQuery();
            StringBuilder json = new StringBuilder("[");
            boolean first = true;

            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                    .append("\"id\":").append(rs.getInt("id")).append(",")
                    .append("\"title\":\"").append(escapeJson(rs.getString("title"))).append("\",")
                    .append("\"type\":\"").append(escapeJson(rs.getString("type"))).append("\",")
                    .append("\"duration\":\"").append(escapeJson(rs.getString("duration"))).append("\",")
                    .append("\"departments\":\"").append(escapeJson(rs.getString("eligible_departments"))).append("\",")
                    .append("\"location\":\"").append(escapeJson(rs.getString("location"))).append("\",")
                    .append("\"compensation\":\"").append(escapeJson(rs.getString("compensation"))).append("\",")
                    .append("\"minGpa\":").append(rs.getDouble("min_gpa")).append(",")
                    .append("\"deadline\":\"").append(rs.getString("deadline")).append("\",")
                    .append("\"driveDate\":\"").append(rs.getString("drive_date") != null ? rs.getString("drive_date") : "").append("\",")
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

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String action = request.getParameter("action"); // 'login' or 'add_opportunity'

        if ("login".equalsIgnoreCase(action)) {
            handleLogin(request, response, out);
        } else if ("add_opportunity".equalsIgnoreCase(action)) {
            handleAddOpportunity(request, response, out);
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Unknown action\"}");
        }
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        String sql = "SELECT id, company_name, status FROM companies WHERE email = ? AND password = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String status = rs.getString("status");
                if ("PENDING".equalsIgnoreCase(status) || "SUSPENDED".equalsIgnoreCase(status)) {
                    out.print("{\"success\":false,\"message\":\"Company access has not yet been approved by the Admin.\"}");
                    return;
                }
                int companyId = rs.getInt("id");
                String companyName = rs.getString("company_name");

                HttpSession session = request.getSession(true);
                session.setAttribute("companyId", companyId);
                session.setAttribute("companyName", companyName);

                out.print("{\"success\":true,\"companyId\":" + companyId + ",\"companyName\":\"" + escapeJson(companyName) + "\"}");
            } else {
                out.print("{\"success\":false,\"message\":\"Invalid company email or password\"}");
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleAddOpportunity(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        String companyIdStr = request.getParameter("companyId");
        String companyName = request.getParameter("companyName");
        String title = request.getParameter("title");
        String type = request.getParameter("type");
        String duration = request.getParameter("duration");
        String departments = request.getParameter("departments");
        String location = request.getParameter("location");
        String compensation = request.getParameter("compensation");
        String minGpaStr = request.getParameter("minGpa");
        String deadlineStr = request.getParameter("deadline");
        String driveDateStr = request.getParameter("driveDate");
        String description = request.getParameter("description");

        if (companyIdStr == null || title == null || type == null || deadlineStr == null) {
            out.print("{\"success\":false,\"message\":\"Please fill in all mandatory fields.\"}");
            return;
        }

        String sql = "INSERT INTO opportunities (company_id, company_name, title, type, duration, eligible_departments, " +
                     "location, compensation, min_gpa, deadline, drive_date, description, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TO_DATE(?, 'YYYY-MM-DD'), TO_DATE(?, 'YYYY-MM-DD'), ?, 'OPEN')";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            double minGpa = (minGpaStr != null && !minGpaStr.trim().isEmpty()) ? Double.parseDouble(minGpaStr) : 0.0;

            ps.setInt(1, Integer.parseInt(companyIdStr));
            ps.setString(2, companyName != null ? companyName : "Partner Company");
            ps.setString(3, title);
            ps.setString(4, type);
            ps.setString(5, duration != null ? duration : "Standard");
            ps.setString(6, departments != null ? departments : "All Departments");
            ps.setString(7, location != null ? location : "Pan India");
            ps.setString(8, compensation != null ? compensation : "Competitive");
            ps.setDouble(9, minGpa);
            ps.setString(10, deadlineStr);
            ps.setString(11, driveDateStr != null && !driveDateStr.isEmpty() ? driveDateStr : deadlineStr);
            ps.setString(12, description != null ? description : "");

            int rows = ps.executeUpdate();
            if (rows > 0) {
                out.print("{\"success\":true,\"message\":\"Opportunity posted successfully!\"}");
            } else {
                out.print("{\"success\":false,\"message\":\"Failed to post opportunity.\"}");
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
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
