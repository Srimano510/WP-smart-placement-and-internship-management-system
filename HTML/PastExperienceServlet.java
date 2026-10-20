import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/PastExperienceServlet")
public class PastExperienceServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String companyFilter = request.getParameter("company");
        String typeFilter = request.getParameter("type"); // 'INTERNSHIP' or 'PLACEMENT'

        StringBuilder sql = new StringBuilder("SELECT id, company_name, role, year_hired, opportunity_type, package_offered, " +
                                              "rounds_count, online_test_details, technical_interview_details, hr_round_details, " +
                                              "preparation_tips, contributor_name FROM past_experiences WHERE 1=1");

        if (companyFilter != null && !companyFilter.trim().isEmpty()) {
            sql.append(" AND LOWER(company_name) LIKE ?");
        }
        if (typeFilter != null && !typeFilter.equalsIgnoreCase("ALL") && !typeFilter.trim().isEmpty()) {
            sql.append(" AND LOWER(opportunity_type) = ?");
        }
        sql.append(" ORDER BY year_hired DESC, id DESC");

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (companyFilter != null && !companyFilter.trim().isEmpty()) {
                ps.setString(paramIndex++, "%" + companyFilter.trim().toLowerCase() + "%");
            }
            if (typeFilter != null && !typeFilter.equalsIgnoreCase("ALL") && !typeFilter.trim().isEmpty()) {
                ps.setString(paramIndex++, typeFilter.trim().toLowerCase());
            }

            ResultSet rs = ps.executeQuery();
            StringBuilder json = new StringBuilder("[");
            boolean first = true;

            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                    .append("\"id\":").append(rs.getInt("id")).append(",")
                    .append("\"companyName\":\"").append(escapeJson(rs.getString("company_name"))).append("\",")
                    .append("\"role\":\"").append(escapeJson(rs.getString("role"))).append("\",")
                    .append("\"year\":").append(rs.getInt("year_hired")).append(",")
                    .append("\"type\":\"").append(escapeJson(rs.getString("opportunity_type"))).append("\",")
                    .append("\"package\":\"").append(escapeJson(rs.getString("package_offered"))).append("\",")
                    .append("\"rounds\":").append(rs.getInt("rounds_count")).append(",")
                    .append("\"onlineTestDetails\":\"").append(escapeJson(rs.getString("online_test_details"))).append("\",")
                    .append("\"technicalDetails\":\"").append(escapeJson(rs.getString("technical_interview_details"))).append("\",")
                    .append("\"hrDetails\":\"").append(escapeJson(rs.getString("hr_round_details"))).append("\",")
                    .append("\"tips\":\"").append(escapeJson(rs.getString("preparation_tips"))).append("\",")
                    .append("\"contributor\":\"").append(escapeJson(rs.getString("contributor_name"))).append("\"")
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
        // Allow adding a new past drive experience
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String companyName = request.getParameter("companyName");
        String role = request.getParameter("role");
        String yearStr = request.getParameter("year");
        String type = request.getParameter("type");
        String pkg = request.getParameter("package");
        String roundsStr = request.getParameter("rounds");
        String otDetails = request.getParameter("onlineTestDetails");
        String techDetails = request.getParameter("technicalDetails");
        String hrDetails = request.getParameter("hrDetails");
        String tips = request.getParameter("tips");
        String contributor = request.getParameter("contributor");

        String sql = "INSERT INTO past_experiences (company_name, role, year_hired, opportunity_type, package_offered, " +
                     "rounds_count, online_test_details, technical_interview_details, hr_round_details, preparation_tips, contributor_name) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, companyName);
            ps.setString(2, role);
            ps.setInt(3, Integer.parseInt(yearStr));
            ps.setString(4, type);
            ps.setString(5, pkg != null ? pkg : "Undisclosed");
            ps.setInt(6, roundsStr != null ? Integer.parseInt(roundsStr) : 3);
            ps.setString(7, otDetails);
            ps.setString(8, techDetails);
            ps.setString(9, hrDetails);
            ps.setString(10, tips);
            ps.setString(11, contributor != null ? contributor : "Verified Alumnus");

            ps.executeUpdate();
            out.print("{\"success\":true,\"message\":\"Past experience record added successfully!\"}");

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
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
