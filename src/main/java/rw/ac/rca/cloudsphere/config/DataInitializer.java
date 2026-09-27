package rw.ac.rca.cloudsphere.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.rca.cloudsphere.catalog.Category;
import rw.ac.rca.cloudsphere.catalog.CategoryRepository;
import rw.ac.rca.cloudsphere.catalog.Product;
import rw.ac.rca.cloudsphere.catalog.ProductRepository;
import rw.ac.rca.cloudsphere.catalog.ProductStatus;
import rw.ac.rca.cloudsphere.commerce.DiscountCode;
import rw.ac.rca.cloudsphere.commerce.DiscountCodeRepository;
import rw.ac.rca.cloudsphere.identity.LoyaltyTier;
import rw.ac.rca.cloudsphere.identity.Role;
import rw.ac.rca.cloudsphere.identity.User;
import rw.ac.rca.cloudsphere.identity.UserRepository;
import rw.ac.rca.cloudsphere.identity.UserStatus;

import java.time.Instant;
import java.util.Set;

@Component
public class DataInitializer implements ApplicationRunner {

    public static final String DEMO_PASSWORD = "CloudSphere1";
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CloudSphereProperties properties;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final DiscountCodeRepository discountCodes;

    public DataInitializer(CloudSphereProperties properties, UserRepository users, PasswordEncoder encoder,
                           CategoryRepository categories, ProductRepository products,
                           DiscountCodeRepository discountCodes) {
        this.properties = properties;
        this.users = users;
        this.encoder = encoder;
        this.categories = categories;
        this.products = products;
        this.discountCodes = discountCodes;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.seed().enabled()) {
            return;
        }
        seedUser("demo@cloudsphere.rw", LoyaltyTier.STANDARD, UserStatus.ACTIVE, Set.of(Role.CUSTOMER));
        seedUser("gold@cloudsphere.rw", LoyaltyTier.GOLD, UserStatus.ACTIVE, Set.of(Role.CUSTOMER));
        User locked = seedUser("locked@cloudsphere.rw", LoyaltyTier.STANDARD, UserStatus.LOCKED, Set.of(Role.CUSTOMER));
        locked.setLockedUntil(Instant.now().plusSeconds(3600));
        users.save(locked);
        seedUser("ops@cloudsphere.rw", LoyaltyTier.STANDARD, UserStatus.ACTIVE, Set.of(Role.ADMIN));

        Category cables = categories.findByNameIgnoreCase("Cables")
                .orElseGet(() -> categories.save(new Category("Cables")));
        seedProduct("USB-C-01", "USB-C Cable", 15_000, 20, ProductStatus.PUBLISHED, cables,
                "A Cloud Sphere catalogue item. Money is integer RWF.", null);
        seedProduct("OOS-01", "Out of stock demo", 5_000, 0, ProductStatus.PUBLISHED, cables,
                "Used in UAT for out-of-stock behaviour.", null);
        seedProduct("HIDDEN-01", "Unpublished draft", 9_999, 10, ProductStatus.DRAFT, cables, null, null);

        Category phones = categories.findByNameIgnoreCase("Phones")
                .orElseGet(() -> categories.save(new Category("Phones")));
        seedProduct("PHN-01", "Sphere Phone Silver", 185_000, 8, ProductStatus.PUBLISHED, phones,
                "Light studio silver handset. Integer RWF, 10% off at qty 5+.", "/products/phone-01.jpg");
        seedProduct("PHN-02", "Sphere Phone Graphite", 245_000, 6, ProductStatus.PUBLISHED, phones,
                "Graphite finish, catalogue studio lighting.", "/products/phone-02.jpg");
        seedProduct("PHN-03", "Sphere Phone Sky", 165_000, 10, ProductStatus.PUBLISHED, phones,
                "Sky-blue accent phone on a pale backdrop.", "/products/phone-03.jpg");
        seedProduct("PHN-04", "Sphere Phone Champagne", 210_000, 7, ProductStatus.PUBLISHED, phones,
                "Champagne gold, compressed studio photo.", "/products/phone-04.jpg");

        if (discountCodes.findByCodeIgnoreCase("SAVE2026").isEmpty()) {
            DiscountCode code = new DiscountCode();
            code.setCode("SAVE2026");
            code.setPercentOff(10);
            code.setMinQty(1);
            code.setActive(true);
            discountCodes.save(code);
        }
        log.info("Demo catalogue and accounts are ready (password length {})", DEMO_PASSWORD.length());
    }

    private User seedUser(String email, LoyaltyTier tier, UserStatus status, Set<Role> roles) {
        return users.findByEmailIgnoreCase(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(encoder.encode(DEMO_PASSWORD));
            user.setPhone("0780000000");
            user.setLoyaltyTier(tier);
            user.setStatus(status);
            user.setRoles(roles);
            return users.save(user);
        });
    }

    private void seedProduct(String sku, String name, long price, int stock, ProductStatus status, Category category,
                             String description, String imageUrl) {
        if (products.findBySkuIgnoreCase(sku).isPresent()) {
            return;
        }
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setDescription(description);
        product.setPriceRwf(price);
        product.setStockQty(stock);
        product.setStatus(status);
        product.setCategory(category);
        product.setImageUrl(imageUrl);
        products.save(product);
    }
}
