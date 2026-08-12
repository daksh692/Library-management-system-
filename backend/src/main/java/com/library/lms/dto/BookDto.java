package com.library.lms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class BookDto {
    @NotBlank(message = "ISBN is required")
    @Pattern(regexp = "^(97[89])[- ]?\\d{1,5}[- ]?\\d{1,7}[- ]?\\d{1,7}[- ]?\\d$",
             message = "ISBN must be a valid ISBN-13, e.g. 978-0-13-468599-1")
    private String isbn;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Author is required")
    private String author;

    @NotBlank(message = "Short description is required")
    private String shortDescription;

    private String longDescription;

    @NotBlank(message = "Genre is required")
    private String genre;

    private String photoUrl;

    @NotBlank(message = "Location is required")
    @Pattern(regexp = "^[A-Z]-\\d{2}-S\\d+$", message = "Location must follow Aisle-Shelf-Bin pattern, e.g., A-04-S2")
    private String location;

    @NotNull(message = "Total copies is required")
    @Min(value = 0, message = "Total copies cannot be negative")
    private Integer totalCopies;

    @Min(value = 0, message = "Price cannot be negative")
    private Double price;
}
