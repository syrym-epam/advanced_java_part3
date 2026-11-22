package advanced.java.part3.configs;

import advanced.java.part3.services.SportHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class SportRouter {

    @Bean
    public RouterFunction<ServerResponse> sportRoutes(SportHandler sportHandler) {
        return route()
                .POST("/api/v1/sport/{sportname}", sportHandler::createSport)
                .GET("/api/v1/sport", sportHandler::searchSports)
                .build();
    }
}