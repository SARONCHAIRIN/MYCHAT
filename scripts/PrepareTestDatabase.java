import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;

/** Used only by test-mysql.py against the disposable database it creates. */
class PrepareTestDatabase {
    public static void main(String[] args) throws Exception {
        String url = System.getenv("DB_URL");
        if (!"true".equals(System.getenv("CHAT_DISPOSABLE_DATABASE"))
                || url == null || !url.startsWith("jdbc:mysql://127.0.0.1:")
                || !url.contains("/chat_verification?")) {
            throw new IllegalStateException("Only the disposable verification database is supported");
        }
        String user = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");
        try (var connection = DriverManager.getConnection(url, user, password);
             var statement = connection.createStatement()) {
            // The source is a schema-only reference, never executed against chat_db.
            String sql = Files.readString(Path.of("docs/schema/existing-chat-db.sql"))
                    .replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)^--.*$", "");
            statement.execute("SET FOREIGN_KEY_CHECKS=0");
            for (String command : sql.split(";")) {
                if (!command.isBlank()) statement.execute(command);
            }
            statement.execute("SET FOREIGN_KEY_CHECKS=1");
        }
        var flyway = Flyway.configure().dataSource(url, user, password)
                .locations("filesystem:src/main/resources/db/migration")
                .baselineVersion("1").baselineOnMigrate(false).cleanDisabled(true).load();
        boolean refused = false;
        try { flyway.migrate(); } catch (FlywayException expected) { refused = true; }
        if (!refused) throw new AssertionError("Unbaselined existing schema was not rejected");
        flyway.baseline();
        var result = flyway.migrate();
        if (result.migrationsExecuted != 1) throw new AssertionError("Expected exactly the V2 migration");
        flyway.validate();
        if (flyway.migrate().migrationsExecuted != 0) throw new AssertionError("Migration is not repeatable");
        try (var connection = DriverManager.getConnection(url, user, password);
             var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()")) {
            rows.next();
            if (rows.getInt(1) != 15) throw new AssertionError("Expected 14 application tables plus Flyway history");
        }
        System.out.println("Verified explicit V1 baseline, V2 migration, validation, and repeat migration.");
    }
}
