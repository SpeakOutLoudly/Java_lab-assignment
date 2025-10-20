package com.study.domain.repository;

import com.study.domain.model.Product;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(long id);

    Product save(Product product);

    boolean onListed(Product product, String newStatus);
    boolean offShelves();
}
