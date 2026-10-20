import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/OpportunityServlet")
public class OpportunityServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String search = request.getParameter("search");
        String sortBy = request.getParameter("sort"); // 'gpa', 'alpha', 'deadline'
        String order = request.getParameter("order"); // 'ASC', 'DESC'
        String type = request.getParameter("type");   // 'ALL', 'INTERNSHIP', 'PLACEMENT'

        StringBuilder sql = new StringBuilder("SELECT id, company_name, title, type, duration, eligible_departments, location, compensation, min_gpa, TO_CHAR(deadline, 'YYYY-MM-DD') AS deadline, TO_CHAR(drive_date, 'YYYY-MM-DD') AS drive_date, description, status FROM opportunities WHERE status = 'OPEN'");

        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (LOWER(company_name) LIKE ? OR LOWER(title) LIKE ? OR LOWER(eligible_departments) LIKE ?)");
        }
        if (type != null && !type.equalsIgnoreCase("ALL") && !type.trim().isEmpty()) {
            sql.append(" AND LOWER(type) = ?");
        }

        // Sorting logic based on user specifications
        if ("gpa".equalsIgnoreCase(sortBy)) {
            sql.append(" ORDER BY min_gpa ").append("DESC".equalsIgnoreCase(order) ? "DESC" : "ASC");
        } else if ("alpha".equalsIgnoreCase(sortBy)) {
            sql.append(" ORDER BY company_name ").append("DESC".equalsIgnoreCase(order) ? "DESC" : "ASC");
        } else if ("deadline".equalsIgnoreCase(sortBy)) {
            // Closest expiring first
            sql.append(" ORDER BY deadline ASC");
        } else {
            sql.append(" ORDER BY id DESC");
        }

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                ps.setString(paramIndex++, searchPattern);
                ps.setString(paramIndex++, searchPattern);
                ps.setString(paramIndex++, searchPattern);
            }
            if (type != null && !type.equalsIgnoreCase("ALL") && !type.trim().isEmpty()) {
                ps.setString(paramIndex++, type.trim().toLowerCase());
            }

            ResultSet rs = ps.executeQuery();
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
                    .append("\"driveDate\":\"").append(rs.getString("drive_date") != null ? rs.getString("drive_date") : "").append("\",")
                    .append("\"description\":\"").append(escapeJson(rs.getString("description"))).append("\",")
                    .append("\"status\":\"").append(rs.getString("status")).append("\"")
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
        // Apply for opportunity
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String studentIdStr = request.getParameter("studentId");
        String oppIdStr = request.getParameter("opportunityId");

        if (studentIdStr == null || oppIdStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Missing studentId or opportunityId\"}");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            int studentId = Integer.parseInt(studentIdStr);
            int oppId = Integer.parseInt(oppIdStr);

            // 1. Check GPA requirement
            String gpaSql = "SELECT o.min_gpa, s.gpa, o.title, o.company_name, o.deadline, o.drive_date FROM opportunities o, students s WHERE o.id = ? AND s.id = ?";
            try (PreparedStatement checkPs = con.prepareStatement(gpaSql)) {
                checkPs.setInt(1, oppId);
                checkPs.setInt(2, studentId);
                ResultSet rs = checkPs.executeQuery();
                if (rs.next()) {
                    double minGpa = rs.getDouble("min_gpa");
                    double studentGpa = rs.getDouble("gpa");
                    if (studentGpa < minGpa && minGpa > 0) {
                        out.print("{\"success\":false,\"message\":\"Your GPA (" + studentGpa + ") does not meet the minimum requirement of " + minGpa + "\"}");
                        return;
                    }
                    String oppTitle = rs.getString("title");
                    String compName = rs.getString("company_name");
                    Date deadline = rs.getDate("deadline");
                    Date driveDate = rs.getDate("drive_date");

                    // 2. Insert into applications
                    String insertSql = "INSERT INTO applications (student_id, opportunity_id, status) VALUES (?, ?, 'APPLIED')";
                    try (PreparedStatement insertPs = con.prepareStatement(insertSql)) {
                        insertPs.setInt(1, studentId);
                        insertPs.setInt(2, oppId);
                        insertPs.executeUpdate();
                    }

                    // 3. Automatically add to student calendar events!
                    if (deadline != null) {
                        String calSql = "INSERT INTO calendar_events (student_id, opportunity_id, event_title, event_type, event_date, details) VALUES (?, ?, ?, 'DEADLINE', ?, ?)";
                        try (PreparedStatement calPs = con.prepareStatement(calSql)) {
                            calPs.setInt(1, studentId);
                            calPs.setInt(2, oppId);
                            calPs.setString(3, compName + " - " + oppTitle + " Deadline");
                            calPs.setDate(4, deadline);
                            calPs.setString(5, "Application deadline closes today");
                            calPs.executeUpdate();
                        }
                    }
                    if (driveDate != null) {
                        String calSql2 = "INSERT INTO calendar_events (student_id, opportunity_id, event_title, event_type, event_date, details) VALUES (?, ?, ?, 'ONLINE_TEST', ?, ?)";
                        try (PreparedStatement calPs2 = con.prepareStatement(calSql2)) {
                            calPs2.setInt(1, studentId);
                            calPs2.setInt(2, oppId);
                            calPs2.setString(3, compName + " - Online Assessment / Drive");
                            calPs2.setDate(4, driveDate);
                            calPs2.setString(5, "Recruitment drive / online test round");
                            calPs2.executeUpdate();
                        }
                    }

                    out.print("{\"success\":true,\"message\":\"Application submitted successfully! Calendar events added.\"}");
                    return;
                } else {
                    out.print("{\"success\":false,\"message\":\"Invalid Student or Opportunity record.\"}");
                    return;
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1 || e.getMessage().contains("UQ_STUDENT_OPP") || e.getMessage().toLowerCase().contains("unique")) {
                out.print("{\"success\":false,\"message\":\"You have already applied for this opportunity!\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"success\":false,\"message\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Error: " + escapeJson(e.getMessage()) + "\"}");
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
