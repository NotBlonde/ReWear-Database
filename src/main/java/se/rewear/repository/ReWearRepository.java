/*
package se.rewear;

import java.sql.*;
import java.util.Properties;
import java.io.InputStream;

public class ReWearQueriesMinimal {

    private static String URL, USER, PASS;

    public static void main(String[] args) {
        loadProps();

        try (Connection conn = getConnection()) {
            if (conn == null) {
                System.err.println("Connection failed.");
                return;
            }

            // 1) Kunder som köpt svarta byxor strl 38 av brand ReWear (i denna modell ligger brand i products)
            System.out.println("== 1) Customers who bought 'black pants size 38' (ReWear) ==");
            String q1 =
                    "SELECT DISTINCT c.first_name, c.last_name " +
                            "FROM customers c " +
                            "JOIN orders o        ON o.customer_id = c.customer_id " +
                            "JOIN order_items oi  ON oi.order_id = o.order_id " +
                            "JOIN products p      ON p.product_id = oi.product_id " +
                            "JOIN product_categories pc ON pc.product_id = p.product_id " +
                            "JOIN categories cat  ON cat.category_id = pc.category_id " +
                            "WHERE p.brand_name = ? AND p.color = ? AND p.size = ? AND cat.name = ?";
            try (PreparedStatement ps = conn.prepareStatement(q1)) {
                ps.setString(1, "ReWear");
                ps.setString(2, "Black");
                ps.setString(3, "38");
                ps.setString(4, "Pants");
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        System.out.printf("- %s %s%n", rs.getString(1), rs.getString(2));
                    }
                }
            }

            // 2) Antal produkter per kategori
            runAndPrint(conn, "\n== 2) Product count per category ==",
                    "SELECT cat.name AS category, COUNT(DISTINCT pc.product_id) AS product_count " +
                            "FROM categories cat " +
                            "LEFT JOIN product_categories pc ON pc.category_id = cat.category_id " +
                            "GROUP BY cat.category_id, cat.name " +
                            "ORDER BY product_count DESC, category");

            // 3) Total spend per customer
            runAndPrint(conn, "\n== 3) Total spend per customer ==",
                    "SELECT c.first_name, c.last_name, SUM(oi.quantity * oi.unit_price) AS total_spent " +
                            "FROM customers c " +
                            "JOIN orders o       ON o.customer_id = c.customer_id " +
                            "JOIN order_items oi ON oi.order_id = o.order_id " +
                            "GROUP BY c.customer_id, c.first_name, c.last_name " +
                            "ORDER BY total_spent DESC");

            // 4) Total order value per city (>1000 SEK)
            runAndPrint(conn, "\n== 4) Total order value per city (>1000 SEK) ==",
                    "SELECT c.city, SUM(oi.quantity * oi.unit_price) AS city_total " +
                            "FROM customers c " +
                            "JOIN orders o       ON o.customer_id = c.customer_id " +
                            "JOIN order_items oi ON oi.order_id = o.order_id " +
                            "GROUP BY c.city HAVING SUM(oi.quantity * oi.unit_price) > 1000 " +
                            "ORDER BY city_total DESC");

            // 5) Top-5 most sold products (variantnivå)
            runAndPrint(conn, "\n== 5) Top-5 most sold products ==",
                    "SELECT p.name AS product, SUM(oi.quantity) AS total_quantity " +
                            "FROM order_items oi " +
                            "JOIN products p ON p.product_id = oi.product_id " +
                            "GROUP BY p.product_id, p.name " +
                            "ORDER BY total_quantity DESC, product " +
                            "LIMIT 5");

            // 6) Best-selling month (YYYY-MM) – två-stegs utan CTE
            System.out.println("\n== 6) Best-selling month (YYYY-MM) ==");
            String q6max =
                    "SELECT SUM(oi.quantity * oi.unit_price) AS total_sales " +
                            "FROM orders o " +
                            "JOIN order_items oi ON oi.order_id = o.order_id " +
                            "GROUP BY DATE_FORMAT(o.order_date, '%Y-%m') " +
                            "ORDER BY total_sales DESC LIMIT 1";

            Double maxTotal = null;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(q6max)) {
                if (rs.next()) maxTotal = rs.getDouble("total_sales");
            }

            if (maxTotal == null) {
                System.out.println("Inga ordrar hittades.");
            } else {
                String q6all =
                        "SELECT DATE_FORMAT(o.order_date, '%Y-%m') AS ym, " +
                                "       SUM(oi.quantity * oi.unit_price)   AS total_sales " +
                                "FROM orders o " +
                                "JOIN order_items oi ON oi.order_id = o.order_id " +
                                "GROUP BY DATE_FORMAT(o.order_date, '%Y-%m') " +
                                "HAVING total_sales = ?";

                try (PreparedStatement ps = conn.prepareStatement(q6all)) {
                    ps.setDouble(1, maxTotal);
                    try (ResultSet rs = ps.executeQuery()) {
                        System.out.println("ym | total_sales");
                        System.out.println("----------------------------------------");
                        while (rs.next()) {
                            System.out.println(rs.getString("ym") + " | " + rs.getString("total_sales"));
                        }
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("SQL error: " + e.getMessage());
        }
    }

    // --- helpers ---

    private static void loadProps() {
        try (InputStream in = ReWearQueriesMinimal.class
                .getClassLoader().getResourceAsStream("application.properties")) {
            if (in == null) throw new IllegalStateException("application.properties not found");
            Properties p = new Properties();
            p.load(in);
            URL  = p.getProperty("db.url");
            USER = p.getProperty("db.user");
            PASS = p.getProperty("db.pass");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load DB properties: " + e.getMessage(), e);
        }
    }

    private static Connection getConnection() {
        try {
            Properties props = new Properties();
            props.setProperty("user", USER);
            props.setProperty("password", PASS);
            props.setProperty("useSSL", "false");
            return DriverManager.getConnection(URL, props);
        } catch (SQLException e) {
            System.err.println("Could not open connection: " + e.getMessage());
            return null;
        }
    }

    private static void runAndPrint(Connection conn, String title, String sql) throws SQLException {
        System.out.println(title);
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();

            for (int i = 1; i <= cols; i++) {
                System.out.print(md.getColumnLabel(i));
                if (i < cols) System.out.print(" | ");
            }
            System.out.println("\n----------------------------------------");

            while (rs.next()) {
                for (int i = 1; i <= cols; i++) {
                    Object v = rs.getObject(i);
                    System.out.print(v == null ? "NULL" : v.toString());
                    if (i < cols) System.out.print(" | ");
                }
                System.out.println();
            }
        }
    }
}
 */