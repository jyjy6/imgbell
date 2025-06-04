package ImgBell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class ImgBellApplication {

	public static void main(String[] args) {
		SpringApplication.run(ImgBellApplication.class, args);
		System.out.println("안녕하세요");
	}

}
