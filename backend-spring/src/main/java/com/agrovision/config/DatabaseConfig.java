package com.agrovision.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.url:}")
    private String rawUrl;

    @Value("${spring.datasource.username:}")
    private String rawUsername;

    @Value("${spring.datasource.password:}")
    private String rawPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        String dbUrl = rawUrl != null ? rawUrl.trim() : "";
        String username = rawUsername;
        String password = rawPassword;

        // Si existe variable de entorno MYSQL_URL o DATABASE_URL
        String envMysqlUrl = System.getenv("MYSQL_URL");
        if (envMysqlUrl == null || envMysqlUrl.isBlank()) {
            envMysqlUrl = System.getenv("DATABASE_URL");
        }
        if (dbUrl.isEmpty() && envMysqlUrl != null && !envMysqlUrl.isBlank()) {
            dbUrl = envMysqlUrl.trim();
        }

        // Si la URL viene en formato Railway / Heroku: mysql://usuario:password@host:port/database
        if (dbUrl.startsWith("mysql://")) {
            try {
                log.info("Transformando formato Railway mysql:// a formato estándar JDBC...");
                URI uri = new URI(dbUrl);
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    username = parts[0];
                    password = parts[1];
                }
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 3306;
                String path = uri.getPath();
                String dbName = (path != null && path.length() > 1) ? path.substring(1) : "railway";

                dbUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                        + "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
                log.info("JDBC URL construida con éxito: jdbc:mysql://{}:{}/{}", host, port, dbName);
            } catch (Exception e) {
                log.error("Error al procesar mysql:// URI: " + e.getMessage(), e);
            }
        } else if (!dbUrl.startsWith("jdbc:mysql://")) {
            // Verificar si existen variables individuales de Railway: MYSQLHOST, MYSQLPORT, etc.
            String mysqlHost = System.getenv("MYSQLHOST");
            if (mysqlHost != null && !mysqlHost.isBlank()) {
                String mysqlPort = System.getenv("MYSQLPORT");
                if (mysqlPort == null || mysqlPort.isBlank()) mysqlPort = "3306";
                String mysqlDb = System.getenv("MYSQLDATABASE");
                if (mysqlDb == null || mysqlDb.isBlank()) mysqlDb = "railway";
                if (username == null || username.isBlank()) username = System.getenv("MYSQLUSER");
                if (password == null || password.isBlank()) password = System.getenv("MYSQLPASSWORD");

                dbUrl = "jdbc:mysql://" + mysqlHost + ":" + mysqlPort + "/" + mysqlDb
                        + "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
                log.info("JDBC URL construida desde variables de Railway: {}", dbUrl);
            } else if (dbUrl.isEmpty()) {
                dbUrl = "jdbc:mysql://localhost:3306/agrovision_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            }
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(username != null ? username : "root");
        config.setPassword(password != null ? password : "");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        try {
            HikariDataSource ds = new HikariDataSource(config);
            try (java.sql.Connection conn = ds.getConnection()) {
                log.info("Conexión MySQL establecida exitosamente con: {}", dbUrl);
                return ds;
            } catch (Exception e) {
                log.warn("No se pudo conectar a MySQL en {}: {}. Activando fallback a base de datos H2 en memoria...", dbUrl, e.getMessage());
                ds.close();
            }
        } catch (Exception e) {
            log.warn("Error inicializando pool de MySQL: {}. Activando fallback a base de datos H2 en memoria...", e.getMessage());
        }

        // Fallback resiliente a H2 en memoria para evitar que la aplicación crashee
        log.info("Iniciando DataSource H2 en memoria (Modo compatibilidad MySQL)...");
        HikariConfig h2Config = new HikariConfig();
        h2Config.setJdbcUrl("jdbc:h2:mem:agrovision_db;DB_CLOSE_DELAY=-1;MODE=MySQL");
        h2Config.setUsername("sa");
        h2Config.setPassword("");
        h2Config.setDriverClassName("org.h2.Driver");
        return new HikariDataSource(h2Config);
    }
}
