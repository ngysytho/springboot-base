package vn.mpl.one;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MplOneApplication {
  public static void main(String[] args) {
    SpringApplication.run(MplOneApplication.class, args);
  }
}
