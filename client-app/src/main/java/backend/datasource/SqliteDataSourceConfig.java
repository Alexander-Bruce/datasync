package backend.datasource;

import javax.sql.DataSource;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Configuration
@MapperScan(
    basePackages = "backend.mapper.sqlite",
    sqlSessionTemplateRef = "sqliteSqlSessionTemplate")
public class SqliteDataSourceConfig {

  @Bean
  @ConfigurationProperties("spring.datasource.sqlite")
  public DataSource sqliteDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean
  public SqlSessionFactory sqliteSqlSessionFactory(
      @Qualifier("sqliteDataSource") DataSource dataSource) throws Exception {

    SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
    factory.setDataSource(dataSource);

    // 1. mapper.xml 路径（你原来的 mapper-locations）
    factory.setMapperLocations(
        new PathMatchingResourcePatternResolver()
            .getResources("classpath:backend/mapper/sqlite/*.xml"));

    // 2. typeAliases
    factory.setTypeAliasesPackage("backend.model.entity");

    // 3. MyBatis configuration（日志等）
    org.apache.ibatis.session.Configuration configuration =
        new org.apache.ibatis.session.Configuration();
    configuration.setLogImpl(StdOutImpl.class);

    factory.setConfiguration(configuration);

    factory.setTransactionFactory(new SpringManagedTransactionFactory());

    return factory.getObject();
  }

  @Bean
  public SqlSessionTemplate sqliteSqlSessionTemplate(
      @Qualifier("sqliteSqlSessionFactory") SqlSessionFactory factory) {
    factory.getConfiguration().setMapUnderscoreToCamelCase(true);
    return new SqlSessionTemplate(factory);
  }

  @Bean("sqliteTransactionManager")
  public DataSourceTransactionManager transactionManager(
      @Qualifier("sqliteDataSource") DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
  }
}
