package com.library.lms.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
    @CompoundIndex(name = "new_arrivals", def = "{'isDeleted': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "recommendations", def = "{'isDeleted': 1, 'genre': 1}")
})
@Document(collection = "books")
public class Book {

    @Id
    private String id;

    @Indexed
    private String isbn;

    @TextIndexed(weight = 10)
    private String name;

    @TextIndexed(weight = 5)
    private String author;

    @TextIndexed(weight = 1)
    private String shortDescription;

    private String longDescription;

    @TextIndexed(weight = 3)
    private String genre;

    private String photoUrl;

    private String location; // Pattern: Aisle-Shelf-Bin, e.g., 'C-12-S3'

    private Integer totalCopies;

    private Integer availableCopies;

    @Builder.Default
    private Double price = 50.0; // Default price if lost/damaged

    @Builder.Default
    private boolean isDeleted = false;

    @CreatedDate
    private java.util.Date createdAt;
}
