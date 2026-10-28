package com.ldd.initialization;

import com.ldd.initialization.config.ai.PgVectorConnectionPool;
import java.sql.ResultSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MigrationRollbackTest {

  @Autowired
  private PgVectorConnectionPool connectionPool;

  @Test
  void testRollbackAISettings() throws Exception {
    try (var conn = connectionPool.getConnection()) {
      try (var stmt = conn.createStatement()) {
        stmt.execute("SELECT public.fn_migration_v2_ai_settings_down()");
      }
      boolean tableExists = false;
      try (var stmt = conn.prepareStatement(
          "SELECT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema='public' AND table_name='t_user_ai_settings')")) {
        try (ResultSet rs = stmt.executeQuery()) {
          if (rs.next()) {
            tableExists = rs.getBoolean(1);
          }
        }
      }
      Assertions.assertFalse(tableExists);
    }
  }
}

