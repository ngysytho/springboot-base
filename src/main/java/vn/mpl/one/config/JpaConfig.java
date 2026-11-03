package vn.mpl.one.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** Application and persistence adapter methods delimit transactions; Open Session in View stays disabled. */
@Configuration
@EnableTransactionManagement
public class JpaConfig {}
