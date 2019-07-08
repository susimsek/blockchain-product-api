package com.org.app.service.mapper;

import com.org.app.domain.Product;
import com.org.app.service.dto.ProductDTO;
import org.mapstruct.Mapper;

/**
 * Mapper for the entity Product and its DTO ProductDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface ProductMapper extends EntityMapper<ProductDTO, Product> {



    default Product fromId(Long id) {
        if (id == null) {
            return null;
        }
        Product product = new Product();
        product.setId(id);
        return product;
    }
}
