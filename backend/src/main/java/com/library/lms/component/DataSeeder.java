package com.library.lms.component;

import com.library.lms.model.Book;
import com.library.lms.model.User;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByUserId("admin").isEmpty()) {
            User admin = User.builder()
                    .userId("admin")
                    .name("System Administrator")
                    .email("admin@library.local")
                    .phone("0000000000")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role("ROLE_ADMIN")
                    .isDeleted(false)
                    .build();
            userRepository.save(admin);
            System.out.println("Default admin user created: admin / admin123");
        }

        // Add some test users
        if (userRepository.findByUserId("user1").isEmpty()) {
            User user1 = User.builder()
                    .userId("user1")
                    .name("Alice Smith")
                    .email("alice@example.com")
                    .phone("1111111111")
                    .passwordHash(passwordEncoder.encode("password"))
                    .role("ROLE_USER")
                    .isDeleted(false)
                    .build();
            userRepository.save(user1);

            User user2 = User.builder()
                    .userId("user2")
                    .name("Bob Johnson")
                    .email("bob@example.com")
                    .phone("2222222222")
                    .passwordHash(passwordEncoder.encode("password"))
                    .role("ROLE_USER")
                    .isDeleted(false)
                    .build();
            userRepository.save(user2);
            System.out.println("Test users created: user1 / password, user2 / password");
        }

        // Add some test books
        if (bookRepository.count() == 0) {
            Book book1 = Book.builder()
                    .isbn("978-0134685991")
                    .name("Effective Java")
                    .author("Joshua Bloch")
                    .shortDescription("A must-have book for Java developers")
                    .longDescription("Are you looking for a deeper understanding of the Java programming language? This book will provide you with practical advice on how to write better Java code.")
                    .genre("Technology")
                    .photoUrl("https://images-na.ssl-images-amazon.com/images/I/41OvwA1y6NL._SX379_BO1,204,203,200_.jpg")
                    .location("A-01-S1")
                    .totalCopies(5)
                    .availableCopies(5)
                    .price(45.0)
                    .isDeleted(false)
                    .build();

            Book book2 = Book.builder()
                    .isbn("978-0201616224")
                    .name("The Pragmatic Programmer")
                    .author("Andrew Hunt, David Thomas")
                    .shortDescription("Your journey to mastery")
                    .longDescription("The Pragmatic Programmer cuts through the increasing specialization and technicalities of modern software development to examine the core process--taking a requirement and producing working, maintainable code that delights its users.")
                    .genre("Technology")
                    .photoUrl("https://images-na.ssl-images-amazon.com/images/I/41as+WafrFL._SX396_BO1,204,203,200_.jpg")
                    .location("A-01-S2")
                    .totalCopies(3)
                    .availableCopies(3)
                    .price(40.0)
                    .isDeleted(false)
                    .build();

            Book book3 = Book.builder()
                    .isbn("978-0132350884")
                    .name("Clean Code")
                    .author("Robert C. Martin")
                    .shortDescription("A Handbook of Agile Software Craftsmanship")
                    .longDescription("Even bad code can function. But if code isn’t clean, it can bring a development organization to its knees.")
                    .genre("Technology")
                    .photoUrl("https://images-na.ssl-images-amazon.com/images/I/41xShlnTZTL._SX376_BO1,204,203,200_.jpg")
                    .location("A-02-S1")
                    .totalCopies(4)
                    .availableCopies(4)
                    .price(50.0)
                    .isDeleted(false)
                    .build();

            bookRepository.saveAll(List.of(book1, book2, book3));
            System.out.println("Test books created");
        }
    }
}
