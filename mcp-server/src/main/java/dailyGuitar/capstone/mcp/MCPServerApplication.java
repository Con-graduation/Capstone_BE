package dailyGuitar.capstone.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * MCP 서버 애플리케이션
 * HTTP 서버를 통해 REST API를 제공하고, stdio를 통한 MCP 프로토콜도 지원합니다.
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "dailyGuitar.capstone.mcp.repository")
public class MCPServerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MCPServerApplication.class, args);
    }
}

