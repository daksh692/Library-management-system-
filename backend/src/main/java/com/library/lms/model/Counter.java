package com.library.lms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Backing document for {@link com.library.lms.service.SequenceGeneratorService}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "counters")
public class Counter {

    @Id
    private String id;

    private long seq;
}
