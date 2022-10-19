package net.da.net.camel;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

@SpringBootApplication
public class SpringBootCamelApplication {

    @Autowired
    private Environment env;

    @Autowired
    private ApplicationContext context;

    public static void main(String[] args) {
        SpringApplication.run(SpringBootCamelApplication.class, args);
    }

    @PostConstruct
    private void dataSourceInfo() {
        HikariDataSource hds = context.getBean(HikariDataSource.class);
        Map<String, Object> objectMAP = context.getBeansOfType(Object.class);
        String url = hds.getJdbcUrl();
        String username = hds.getUsername();
        String password = hds.getUsername();
//        objectMAP.entrySet().forEach(System.out::println);
    }

    @Component
    class RestApi extends RouteBuilder {

        @Override
        public void configure() {
            restConfiguration()
                    .contextPath("/camel")
                    .apiContextPath("/api-doc")
                    .apiProperty("api.title", "JAVA DEV JOURNAL REST API")
                    .apiProperty("api.version", "1.0")
                    .apiProperty("cors", "true")
                    .apiContextRouteId("doc-api")
                    .port(env.getProperty("server.port", "8080"))
                    .bindingMode(RestBindingMode.json);

            rest("/products").description("Details of products")
                    .get("/").description("List of all products")
                    .route().routeId("products-api")
                    .bean(ProductService.class, "findAll")
                    .endRest()
                    .get("discounts/{id}").description("Discount of a product")
                    .route().routeId("discount-api")
                    .bean(DiscountService.class, "findDiscount(${header.id})");
        }
    }

    @Component
    class TimedJobs extends RouteBuilder {

        @Override
        public void configure() {
            from("timer:new-discount?delay=1000&period={{discount.newDiscountPeriod:2000}}")
                    .routeId("make-discount")
                    .bean("discountService", "makeDiscount")
                    .to("jpa:org.apache.camel.example.spring.boot.rest.jpa.Discount")
                    .log("Created %${body.amount} discount for ${body.product.name}");

            from("jpa:org.apache.camel.example.spring.boot.rest.jpa.Product"
                    + "?namedQuery=discounted-products"
                    + "&delay={{discount.listDiscountPeriod:6000}}"
                    + "&consumeDelete=false")
                    .routeId("list-discounted-products")
                    .log("Discounted product ${body.name}. Price dropped from ${body.price} to ${body.discounted}");
        }
    }
}