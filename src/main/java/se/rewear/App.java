package se.rewear;

import se.rewear.db.DatabaseConnection;
import se.rewear.repository.ReWearRepository;

import java.sql.Connection;

/**
 * Entry point. Keeps main clean; delegates to repository methods.
 */
public class App {
    public static void main(String[] args) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                System.err.println("Could not connect to database.");
                return;
            }

            ReWearRepository repo = new ReWearRepository(conn);

            repo.customersBoughtBlackPants();
            repo.productCountPerCategory();
            repo.totalSpendPerCustomer();
            repo.orderValuePerCity();
            repo.top5Products();
            repo.bestSellingMonth();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
