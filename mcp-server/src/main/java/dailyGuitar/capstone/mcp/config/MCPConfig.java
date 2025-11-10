package dailyGuitar.capstone.mcp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * MCP 서버 설정
 */
@Configuration
@EnableJpaRepositories(basePackages = "dailyGuitar.capstone.mcp.repository")
public class MCPConfig {
    // MCP 서버 관련 설정
}

