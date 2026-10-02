package com.example.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false"
})
class UserStatusMigrationIntegrationTests {
    @Autowired
    private DataSource dataSource;

    @Test
    void migrationNormalizesMissingStatusAndConstrainsFutureWrites() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // All migration SQL resolves to this temporary table, never the application's users.
                connection.createStatement().execute("""
                        CREATE TEMPORARY TABLE users (
                            id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            status VARCHAR(255)
                        ) ON COMMIT DROP
                        """);
                connection.createStatement().execute("""
                        INSERT INTO users(status) VALUES
                        (NULL), (''), ('  '), ('PENDING'), ('ACTIVE'), ('LOCKED'), ('DISABLED')
                        """);

                ScriptUtils.executeSqlScript(connection,
                        new ClassPathResource("db/migration/V5__constrain_user_status.sql"));
                connection.createStatement().execute("INSERT INTO users DEFAULT VALUES");
                List<String> statuses = new ArrayList<>();
                try (var result = connection.createStatement().executeQuery("SELECT status FROM users ORDER BY id")) {
                    while (result.next()) {
                        statuses.add(result.getString(1));
                    }
                }
                assertEquals(List.of("PENDING", "PENDING", "PENDING", "PENDING",
                        "ACTIVE", "LOCKED", "DISABLED", "PENDING"), statuses);

                assertRejected(connection, "INSERT INTO users(status) VALUES ('UNKNOWN')", "23514");
                assertRejected(connection, "INSERT INTO users(status) VALUES ('active')", "23514");
                assertRejected(connection, "INSERT INTO users(status) VALUES (NULL)", "23502");
            } finally {
                connection.rollback();
            }
        }
    }

    private void assertRejected(Connection connection, String sql, String sqlState) throws SQLException {
        var savepoint = connection.setSavepoint();
        try {
            SQLException exception = assertThrows(SQLException.class,
                    () -> connection.createStatement().execute(sql));
            assertEquals(sqlState, exception.getSQLState());
        } finally {
            connection.rollback(savepoint);
        }
    }
}
