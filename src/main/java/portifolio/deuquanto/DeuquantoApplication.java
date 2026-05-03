package portifolio.deuquanto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DeuquantoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeuquantoApplication.class, args);
	}

}
