package com.library.lms.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "books")
public class Book {

    @Id
    private String id;

    @Indexed
    private String isbn;

    @Indexed
    private String name;

    @Indexed
    private String author;

    private String shortDescription;

    private String longDescription;

    @Indexed
    private String genre;

    private String photoUrl;

    private String location; // Pattern: Aisle-Shelf-Bin, e.g., 'C-12-S3'

    private Integer totalCopies;

    private Integer availableCopies;

    @Builder.Default
    private Double price = 50.0; // Default price if lost/damaged

    @Builder.Default
    private boolean isDeleted = false;
}
