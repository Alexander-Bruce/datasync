package backend.datasource;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class TransactionManagerConfig {

  @Bean
  @Primary
  public PlatformTransactionManager mysqlTxManager(@Qualifier("mysqlDataSource") DataSource ds) {
    return new DataSourceTransactionManager(ds);
  }
}
