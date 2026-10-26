package dastec.one.demo;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@SpringBootApplication

public class SpringBootApp {

    private final ProductRepository productRepository;

    public SpringBootApp(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }

    @Bean
    ApplicationRunner init() {
        return args -> {
            addProduct("MacBook Pro M3", BigDecimal.valueOf(1999), 25);
            addProduct("Dell XPS 15", BigDecimal.valueOf(11499), 18);
            addProduct("Sony WH-1000XM5", BigDecimal.valueOf(1399), 50);
        };
    }

    private void addProduct(String name, BigDecimal price, int stock) {
        var product = new Product(null,name, price, stock);
        productRepository.save(product);
    }
}

@RestController
@RequestMapping("/api/products")
class ProductController {

    private final ProductRepository productRepository;

    ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<Product> handleGetAll() {
        return productRepository.findAll();
    }

    @GetMapping("/{id}")
    public Product handleGetById(@PathVariable int id) {
        return productRepository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with id " + id));
    }

    @PostMapping
    public  Product handleCreate(String name, BigDecimal price, int stock) {
        var product = new Product(null, name, price, stock);
        return productRepository.save(product);
    }

    @PutMapping("/{id}")
    public Product handleUpdate(@PathVariable int id, String name, BigDecimal price, int stock) {
        var product = productRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with id " + id));
        product.setName(name);
        product.setPrice(price);
        product.setStock(stock);
        return productRepository.save(product);
    }

    @DeleteMapping("/{id}")
    public void handleDelete(@PathVariable int id) {
        productRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with id " + id));
        productRepository.deleteById(id);
    }
}
