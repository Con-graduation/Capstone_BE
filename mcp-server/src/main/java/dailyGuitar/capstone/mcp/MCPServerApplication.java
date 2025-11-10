package dailyGuitar.capstone.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * MCP 서버 애플리케이션
 * 웹 서버는 실행하지 않고, stdio를 통해 MCP 프로토콜로 통신합니다.
 */
@SpringBootApplication(exclude = {WebMvcAutoConfiguration.class})
@EnableJpaRepositories(basePackages = "dailyGuitar.capstone.mcp.repository")
public class MCPServerApplication {
    
    public static void main(String[] args) {
        // 웹 서버 없이 실행
        SpringApplication app = new SpringApplication(MCPServerApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        app.run(args);
    }
}

