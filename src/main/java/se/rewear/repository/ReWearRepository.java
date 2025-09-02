package se.rewear.repository;

import java.sql.*;

/**
 * Repository/DAO layer containing all reporting queries.
 * Keeps App.java clean and testable.
 */
public class ReWearRepository {
    private final Connection conn;

    public ReWearRepository(Connection conn) {
        this.conn = conn;
    }

    // --- Utilities -----------------------------------------------------------

    private void runAndPrint(String title, String sql) throws SQLException {
        System.out.println(title);
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();

            // header
            for (int i = 1; i <= cols; i++) {
                System.out.print(md.getColumnLabel(i));
                if (i < cols) System.out.print(" | ");
            }
            System.out.println("\n----------------------------------------");

            // rows
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

    // --- Queries -------------------------------------------------------------

    // 1) Customers who bought 'black pants size 38' (brand ReWear)
    public void customersBoughtBlackPants() throws SQLException {
        System.out.println("== 1) Customers who bought 'black pants size 38' (ReWear) ==");
        String sql =
                "SELECT DISTINCT c.first_name, c.last_name " +
                        "FROM customers c " +
                        "JOIN orders o       ON o.customer_id = c.customer_id " +
                        "JOIN order_items oi ON oi.order_id = o.order_id " +
                        "JOIN product_variants pv ON pv.variant_id = oi.variant_id " +
                        "JOIN products p     ON p.product_id = pv.product_id " +
                        "JOIN brands b       ON b.brand_id = p.brand_id " +
                        "JOIN product_categories pc ON pc.product_id = p.product_id " +
                        "JOIN categories cat ON cat.category_id = pc.category_id " +
                        "WHERE b.name = ? AND pv.color = ? AND pv.size = ? AND cat.name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
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
    }

    // 2) Product count per category
    public void productCountPerCategory() throws SQLException {
        runAndPrint("\n== 2) Product count per category ==",
                "SELECT cat.name AS category, COUNT(DISTINCT pc.product_id) AS product_count " +
                        "FROM categories cat " +
                        "LEFT JOIN product_categories pc ON pc.category_id = cat.category_id " +
                        "GROUP BY cat.category_id, cat.name " +
                        "ORDER BY product_count DESC, category");
    }

    // 3) Total spend per customer
    public void totalSpendPerCustomer() throws SQLException {
        runAndPrint("\n== 3) Total spend per customer ==",
                "SELECT c.first_name, c.last_name, SUM(oi.quantity * oi.unit_price) AS total_spent " +
                        "FROM customers c JOIN orders o ON o.customer_id = c.customer_id " +
                        "JOIN order_items oi ON oi.order_id = o.order_id " +
                        "GROUP BY c.customer_id, c.first_name, c.last_name " +
                        "ORDER BY total_spent DESC");
    }

    // 4) Total order value per city (>1000 SEK)
    public void orderValuePerCity() throws SQLException {
        runAndPrint("\n== 4) Total order value per city (>1000 SEK) ==",
                "SELECT c.city, SUM(oi.quantity * oi.unit_price) AS city_total " +
                        "FROM customers c JOIN orders o ON o.customer_id = c.customer_id " +
                        "JOIN order_items oi ON oi.order_id = o.order_id " +
                        "GROUP BY c.city HAVING SUM(oi.quantity * oi.unit_price) > 1000 " +
                        "ORDER BY city_total DESC");
    }

    // 5) Top-5 most sold products
    public void top5Products() throws SQLException {
        runAndPrint("\n== 5) Top-5 most sold products ==",
                "SELECT p.name AS product, SUM(oi.quantity) AS total_quantity " +
                        "FROM order_items oi JOIN product_variants pv ON pv.variant_id = oi.variant_id " +
                        "JOIN products p ON p.product_id = pv.product_id " +
                        "GROUP BY p.product_id, p.name " +
                        "ORDER BY total_quantity DESC, product LIMIT 5");
    }

    // 6) Best-selling month (robust two-step, no CTE)
    public void bestSellingMonth() throws SQLException {
        System.out.println("\n== 6) Best-selling month (YYYY-MM) ==");
        // Step 1: get max monthly total
        String q6max =
                "SELECT SUM(oi.quantity * oi.unit_price) AS total_sales " +
                        "FROM orders o JOIN order_items oi ON oi.order_id = o.order_id " +
                        "GROUP BY DATE_FORMAT(o.order_date, '%Y-%m') " +
                        "ORDER BY total_sales DESC LIMIT 1";

        Double maxTotal = null;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(q6max)) {
            if (rs.next()) maxTotal = rs.getDouble("total_sales");
        }
        if (maxTotal == null) { System.out.println("Inga ordrar hittades."); return; }

        // Step 2: list all months matching that max
        String q6all =
                "SELECT DATE_FORMAT(o.order_date, '%Y-%m') AS ym, " +
                        "       SUM(oi.quantity * oi.unit_price)   AS total_sales " +
                        "FROM orders o JOIN order_items oi ON oi.order_id = o.order_id " +
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
}
