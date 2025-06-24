package l9g.webapp.signaturepaddemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(
  exclude = {
    UserDetailsServiceAutoConfiguration.class
  }
)
public class SignaturePadDemoApplication
{

  public static void main(String[] args)
  {
    SpringApplication.run(SignaturePadDemoApplication.class, args);
  }

}
