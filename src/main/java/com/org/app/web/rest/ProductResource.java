
package com.org.app.web.rest;

import com.org.app.domain.Product;
import com.org.app.network.networkException.A_BlockchainException;
import com.org.app.network.networkException.EntityNotFound;
import com.org.app.network.request.Add;
import com.org.app.network.request.Delete;
import com.org.app.network.request.Get;
import com.org.app.network.request.Set;
import com.org.app.repository.ProductRepository;
import com.org.app.web.rest.errors.BadRequestAlertException;
import com.org.app.web.rest.util.HeaderUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Product.
 */
@RestController
@RequestMapping("/api")
public class ProductResource {

    private final Logger log = LoggerFactory.getLogger(ProductResource.class);

    private static final String ENTITY_NAME = "product";

    private final ProductRepository productRepository;

    public ProductResource(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * POST /products : Create a new product.
     *
     * @param product the product to create
     * @return the ResponseEntity with status 201 (Created) and with body the new
     *         product, or with status 400 (Bad Request) if the product has already
     *         an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/products")
    public ResponseEntity<Product> createProduct(@RequestBody Product product) throws URISyntaxException {
        log.debug("REST request to save Product : {}", product);
        if (product.getId() != null) {
            throw new BadRequestAlertException("A new product cannot already have an ID", ENTITY_NAME, "idexists");
        }

        Product result = productRepository.save(product);

        // Process blockchain add request
        log.debug("BLOCKCHAIN ADD: " + product.getId().toString() + " with the value: " + product.toString());
        ResponseEntity<String> response = addRequest(product.getId().toString(), product.toString());
        log.debug("BLOCKCHAIN ADD RESPONSE: " + response);

        return ResponseEntity.created(new URI("/api/products/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString())).body(result);
    }

    /**
     * PUT /products : Updates an existing product.
     *
     * @param product the product to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated
     *         product, or with status 400 (Bad Request) if the product is not
     *         valid, or with status 500 (Internal Server Error) if the product
     *         couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/products")
    public ResponseEntity<Product> updateProduct(@RequestBody Product product) throws URISyntaxException {
        log.debug("REST request to update Product : {}", product);
        if (product.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }

        // Process blockchain set request
        log.debug("BLOCKCHAIN UPDATE: " + product.getId().toString() + " to the value: " + product.toString());
        ResponseEntity<String> response = setRequest(product.getId().toString(), product.toString());
        log.debug("BLOCKCHAIN UPDATE RESPONSE: " + response);

        Product result = productRepository.save(product);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, product.getId().toString()))
                .body(result);
    }

    /**
     * GET /products : get all the products.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of products in
     *         body
     */
    @GetMapping("/products")
    public List<Product> getAllProducts() {
        log.debug("REST request to get all Products");
        return productRepository.findAll();
    }

    /**
     * GET /products/:id : get the "id" product.
     *
     * @param id the id of the product to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the product, or
     *         with status 404 (Not Found)
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getRequest(@PathVariable Long id) {
        log.debug("REST request to get Product : {}", id);
        Optional<Product> product = productRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(product);
    }

    /**
     * DELETE /products/:id : delete the "id" product.
     *
     * @param id the id of the product to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.debug("REST request to delete Product : {}", id);

        // Process blockchain delete request
        log.debug("BLOCKCHAIN DELETE: " + id.toString());
        ResponseEntity<String> response = deleteRequest(id.toString());
        log.debug("BLOCKCHAIN DELETE RESPONSE: " + response);

        productRepository.deleteById(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * POST /products/add : add a new value to the blockchain.
     *
     * @param value the hash of the diploma we want to add to the BC
     * @return the ResponseEntity with status 200 (OK) and the transaction ID, or
     *         with status 417 (EXPECTATION_FAILED), or with status 500
     *         (INTERNAL_SERVER_ERROR)
     */
    @PostMapping("/products/add")
    public ResponseEntity<String> addRequest(@RequestParam String entity, String value) {
        if (entity.isEmpty()) {
            log.debug("Empty entity name");
            return new ResponseEntity<String>("EMPTY_ENTITY_NAME", HttpStatus.EXPECTATION_FAILED);
        }
        if (value.isEmpty()) {
            log.debug("Empty value");
            return new ResponseEntity<String>("EMPTY_VALUE", HttpStatus.EXPECTATION_FAILED);
        }

        Add blockchainRequest;
        String transactionID;
        try {
            blockchainRequest = new Add(entity, value);
            blockchainRequest.send();
            transactionID = blockchainRequest.transactionID;
        } catch (A_BlockchainException e) {
            String errored = "BLOCKCHAIN ERROR: " + e.toString();
            log.debug(errored);
            return new ResponseEntity<String>(errored, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            String errored = "BLOCKCHAIN ERROR: " + e.toString();
            log.debug(errored);
            return new ResponseEntity<String>(errored, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Create JSON string
        String returned = "{" + '"' + "transactionID" + '"' + ":" + '"' + transactionID + '"' + "}";
        return new ResponseEntity<String>(returned, HttpStatus.OK);
    }

    /**
     * GET /products/get : Get an entity value from the blockchain
     *
     * @param entity the entity to query
     * @return the ResponseEntity with status 200 (OK) and the value of the entity,
     *         or with status 417 (EXPECTATION_FAILED), or with status 500
     *         (INTERNAL_SERVER_ERROR)
     */
    @GetMapping("/products/get")
    public ResponseEntity<String> getRequest(@RequestParam String entity) {
        if (entity.isEmpty()) {
            log.debug("Empty entity name");
            return new ResponseEntity<String>("EMPTY_ENTITY_NAME", HttpStatus.EXPECTATION_FAILED);
        }

        String value = null;
        Get get;

        try {
            get = new Get(entity);
            get.send();
            value = get.state;
        } catch (EntityNotFound e) {
            String errored = "BLOCKCHAIN ERROR: " + e.toString();
            log.debug(errored);

            // Create JSON string
            String returned = "{" + '"' + "entityState" + '"' + ":" + '"' + "NOT_FOUND" + '"' + "}";
            return new ResponseEntity<String>(returned, HttpStatus.OK);
        } catch (Exception e) {
            String errored = "BLOCKCHAIN ERROR: " + e.toString();
            log.debug(errored);
            return new ResponseEntity<String>(errored, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (value == null) {
            log.debug("The query has failed");
            return new ResponseEntity<String>("QUERY FAILED", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        switch (value) {
        case "NOT_FOUND":
            // Create JSON string
            String returned = "{" + '"' + "entityState" + '"' + ":" + '"' + "NOT_FOUND" + '"' + "}";
            return new ResponseEntity<String>(returned, HttpStatus.OK);
        }

        // Create JSON string
        String returned = "{" + '"' + "entityState" + '"' + ":" + '"' + value + '"' + "}";
        return new ResponseEntity<String>(returned, HttpStatus.OK);
    }

    /**
     * DELETE /products/delete : delete an entity from the blockchain.
     *
     * @param entity to delete from the blockchain
     * @return the ResponseEntity with status 200 (OK) and the transaction ID, or
     *         with status 417 (EXPECTATION_FAILED), or with status 500
     *         (INTERNAL_SERVER_ERROR)
     */
    @DeleteMapping("/products/delete")
    public ResponseEntity<String> deleteRequest(@RequestParam String entity) {
        if (entity.isEmpty()) {
            log.debug("Empty entity name");
            return new ResponseEntity<String>("EMPTY_ENTITY_NAME", HttpStatus.EXPECTATION_FAILED);
        }

        Delete blockchainRequest;
        String transactionID;
        try {
            blockchainRequest = new Delete(entity);
            blockchainRequest.send();
            transactionID = blockchainRequest.transactionID;
        } catch (A_BlockchainException e) {
            String errored = "BLOCKCHAIN ERROR: " + e.toString();
            log.debug(errored);
            return new ResponseEntity<String>(errored, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            String errored = "BLOCKCHAIN ERROR: " + e.toString();
            log.debug(errored);
            return new ResponseEntity<String>(errored, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Create JSON string
        String returned = "{" + '"' + "transactionID" + '"' + ":" + '"' + transactionID + '"' + "}";
        return new ResponseEntity<String>(returned, HttpStatus.OK);
    }

    /**
     * POST /products/set : set an entity in the blockchain.
     *
     *
     * @param entity the entity to add to the blockchain
     * @param value  the value to set the entity to
     * @return the ResponseEntity with status 200 (OK) and the transaction ID, or
     *         with status 417 (EXPECTATION_FAILED), or with status 500
     *         (INTERNAL_SERVER_ERROR)
     */
    @PostMapping("/products/set")
    public ResponseEntity<String> setRequest(@RequestParam String entity, String value) {
        if (entity.isEmpty()) {
            log.debug("Empty entity name");
            return new ResponseEntity<String>("EMPTY_ENTITY_NAME", HttpStatus.EXPECTATION_FAILED);
        }
        if (value.isEmpty()) {
            log.debug("Empty value");
            return new ResponseEntity<String>("EMPTY_VALUE", HttpStatus.EXPECTATION_FAILED);
        }

        Set blockchainRequest;
        String transactionID;
        try {
            blockchainRequest = new Set(entity, value);
            blockchainRequest.send();
            transactionID = blockchainRequest.transactionID;
        } catch (A_BlockchainException e) {
            String errored = "BLOCKCHAIN ERROR: " + e.toString();
            log.debug(errored);
            return new ResponseEntity<String>(errored, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            String errored = "BLOCKCHAIN ERROR: " + e.toString();
            log.debug(errored);
            return new ResponseEntity<String>(errored, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Create JSON string
        String returned = "{" + '"' + "transactionID" + '"' + ":" + '"' + transactionID + '"' + "}";
        return new ResponseEntity<String>(returned, HttpStatus.OK);
    }

}

