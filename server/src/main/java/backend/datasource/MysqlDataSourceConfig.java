package backend.datasource;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
@MapperScan(
    basePackages = "backend.mapper.mysql",
    sqlSessionTemplateRef = "mysqlSqlSessionTemplate")
public class MysqlDataSourceConfig {

  @Value("${application.datasource.mysql.url}")
  private String url;

  @Value("${application.datasource.mysql.username}")
  private String username;

  @Value("${application.datasource.mysql.password}")
  private String password;

  @Bean
  @Primary
  public DataSource mysqlDataSource() {
    HikariDataSource ds = new HikariDataSource();
    ds.setJdbcUrl(url);
    ds.setUsername(username);
    ds.setPassword(password);
    ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
    return ds;
  }

  @Bean
  @Primary
  public SqlSessionFactory mysqlSqlSessionFactory(
      @Qualifier("mysqlDataSource") DataSource dataSource) throws Exception {

    SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
    factory.setDataSource(dataSource);

    // 1. MySQL mapper.xml
    factory.setMapperLocations(
        new PathMatchingResourcePatternResolver()
            .getResources("classpath:backend/mapper/mysql/*.xml"));

    // 2. 实体别名
    factory.setTypeAliasesPackage("backend.model.entity");

    // 3. MyBatis 配置（日志）
    org.apache.ibatis.session.Configuration configuration =
        new org.apache.ibatis.session.Configuration();
    configuration.setLogImpl(StdOutImpl.class);
    factory.setConfiguration(configuration);

    return factory.getObject();
  }

  @Bean
  @Primary
  public SqlSessionTemplate mysqlSqlSessionTemplate(
      @Qualifier("mysqlSqlSessionFactory") SqlSessionFactory factory) {
    return new SqlSessionTemplate(factory);
  }
}
