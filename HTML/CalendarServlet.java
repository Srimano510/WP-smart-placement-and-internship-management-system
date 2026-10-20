import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/CalendarServlet")
public class CalendarServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String studentIdStr = request.getParameter("studentId");
        if (studentIdStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"studentId parameter is required\"}");
            return;
        }

        int studentId = Integer.parseInt(studentIdStr);

        // Fetch calendar events + upcoming applied opportunities
        String sql = "SELECT c.id, c.event_title, c.event_type, TO_CHAR(c.event_date, 'YYYY-MM-DD') AS event_date, " +
                     "c.event_time, c.details, o.company_name, o.title AS opp_title " +
                     "FROM calendar_events c " +
                     "LEFT JOIN opportunities o ON c.opportunity_id = o.id " +
                     "WHERE c.student_id = ? " +
                     "ORDER BY c.event_date ASC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            StringBuilder json = new StringBuilder("[");
            boolean first = true;

            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                    .append("\"id\":").append(rs.getInt("id")).append(",")
                    .append("\"title\":\"").append(escapeJson(rs.getString("event_title"))).append("\",")
                    .append("\"type\":\"").append(escapeJson(rs.getString("event_type"))).append("\",")
                    .append("\"date\":\"").append(rs.getString("event_date")).append("\",")
                    .append("\"time\":\"").append(rs.getString("event_time") != null ? rs.getString("event_time") : "09:00 AM").append("\",")
                    .append("\"details\":\"").append(escapeJson(rs.getString("details"))).append("\",")
                    .append("\"companyName\":\"").append(rs.getString("company_name") != null ? escapeJson(rs.getString("company_name")) : "").append("\",")
                    .append("\"oppTitle\":\"").append(rs.getString("opp_title") != null ? escapeJson(rs.getString("opp_title")) : "").append("\"")
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
