package com.epam.esm.model.dto;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class CreatingCertificateDto {

    Double price;

    Integer duration;

    String name;

    String description;

    List<TagDto> tags;
}