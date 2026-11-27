package com.barboya.businesses;

import com.barboya.businesses.BusinessDtos.ProductResponse;
import com.barboya.common.ApiException;
import com.barboya.files.FileStorage;
import com.barboya.files.ProductPhotos;
import com.barboya.files.Uploads;
import com.barboya.products.Product;
import com.barboya.products.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.util.UUID;

@Service
@Transactional
public class ProductPhotoService {

    private final BusinessService businesses;
    private final ProductRepository products;
    private final FileStorage storage;
    private final Clock clock;

    ProductPhotoService(BusinessService businesses, ProductRepository products, FileStorage storage, Clock clock) {
        this.businesses = businesses;
        this.products = products;
        this.storage = storage;
        this.clock = clock;
    }

    public ProductResponse set(UUID ownerId, UUID productId, MultipartFile file) {
        Product product = businesses.requireProduct(ownerId, productId);
        byte[] photo = ProductPhotos.normalize(Uploads.read(file, false).content());
        return replace(product, storage.store(photo));
    }

    public ProductResponse remove(UUID ownerId, UUID productId) {
        return replace(businesses.requireProduct(ownerId, productId), null);
    }

    /**
     * Solo entrega archivos que son la foto de un producto: en el mismo almacen
     * hay documentos privados que nunca deben salir por aqui.
     */
    @Transactional(readOnly = true)
    public byte[] read(String key) {
        if (!products.existsByPhotoKey(key)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PHOTO_NOT_FOUND", "Foto no encontrada");
        }
        return storage.read(key);
    }

    /** El producto se archiva y su foto deja de existir; los pedidos anteriores no la usan. */
    public void archive(UUID ownerId, UUID productId) {
        Product product = businesses.requireProduct(ownerId, productId);
        product.setArchived(true);
        replace(product, null);
    }

    /** El archivo anterior se borra solo si el cambio queda confirmado, y al reves. */
    private ProductResponse replace(Product product, String key) {
        String previous = product.getPhotoKey();
        product.setPhotoKey(key);
        product.setUpdatedAt(clock.instant());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                String obsolete = status == STATUS_COMMITTED ? previous : key;
                if (obsolete != null) {
                    storage.delete(obsolete);
                }
            }
        });
        return ProductResponse.from(products.saveAndFlush(product));
    }
}
