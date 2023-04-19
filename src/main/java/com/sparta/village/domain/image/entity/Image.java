package com.sparta.village.domain.image.entity;

import com.sparta.village.domain.product.entity.Product;
import lombok.*;


import javax.persistence.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @ManyToOne
    private Product product;

    public Image(Product product, String imageUrl) {
        this.product = product;
        this.imageUrl = imageUrl;
    }

}
