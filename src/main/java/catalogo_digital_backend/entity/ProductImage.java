
package catalogo_digital_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(
            name = "image_url",
            nullable = false,
            length = 2048
    )
    private String imageUrl;

    @Column(
            name = "sort_order",
            nullable = false
    )
    private int sortOrder;

    @Column(
            name = "is_main",
            nullable = false
    )
    private boolean mainImage = false;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "product_id",
            nullable = false
    )
    private Product product;

    // Constructor requerido por JPA
    protected ProductImage() {
    }

    // Constructor para crear imágenes
    public ProductImage(
            String imageUrl,
            int sortOrder,
            boolean mainImage,
            Product product
    ) {
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
        this.mainImage = mainImage;
        this.product = product;
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isMainImage() {
        return mainImage;
    }

    public Product getProduct() {
        return product;
    }

    // Setters

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setMainImage(boolean mainImage) {
        this.mainImage = mainImage;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
