/* This code was generated by Akka Serverless tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */
package shopping.cart.domain;

import com.akkaserverless.javasdk.testkit.junit.AkkaServerlessTestkitResource;
import com.google.protobuf.Empty;
import org.junit.ClassRule;
import org.junit.Test;
import shopping.Main;
import shopping.cart.api.ShoppingCartApi;
import shopping.cart.api.ShoppingCartServiceClient;
import java.util.List;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.*;

// Example of an integration test calling our service via the Akka Serverless proxy
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class ShoppingCartEntityIntegrationTest {

  /**
   * The test kit starts both the service container and the Akka Serverless proxy.
   */
  @ClassRule
  public static final AkkaServerlessTestkitResource testkit =
    new AkkaServerlessTestkitResource(Main.createAkkaServerless());

  /**
   * Use the generated gRPC client to call the service through the Akka Serverless proxy.
   */
  private final ShoppingCartServiceClient client;

  public ShoppingCartEntityIntegrationTest() {
    client = ShoppingCartServiceClient.create(testkit.getGrpcClientSettings(), testkit.getActorSystem());
  }

  ShoppingCartApi.Cart getCart(String cartId) throws Exception {
    return client
        .getCart(ShoppingCartApi.GetShoppingCart.newBuilder().setCartId(cartId).build())
        .toCompletableFuture()
        .get();
  }

  void addItem(String cartId, String productId, String name, int quantity) throws Exception {
    client
        .addItem(
            ShoppingCartApi.AddLineItem.newBuilder()
                .setCartId(cartId)
                .setProductId(productId)
                .setName(name)
                .setQuantity(quantity)
                .build())
        .toCompletableFuture()
        .get(2, SECONDS);
  }

  void removeItem(String cartId, String productId, int quantity) throws Exception {
    client
        .removeItem(
            ShoppingCartApi.RemoveLineItem.newBuilder()
                .setCartId(cartId)
                .setProductId(productId)
                .setQuantity(quantity)
                .build())
        .toCompletableFuture()
        .get(2, SECONDS);
  }

  ShoppingCartApi.LineItem item(String productId, String name, int quantity) {
    return ShoppingCartApi.LineItem.newBuilder()
        .setProductId(productId)
        .setName(name)
        .setQuantity(quantity)
        .build();
  }

  @Test
  public void emptyCartByDefault() throws Exception {
    assertEquals("shopping cart should be empty", 0, getCart("user1").getItemsCount());
  }

  @Test
  public void addItemsToCart() throws Exception {
    final String cartId = "cart2";
    addItem(cartId, "a", "Apple", 1);
    addItem(cartId, "b", "Banana", 2);
    addItem(cartId, "c", "Cantaloupe", 3);
    ShoppingCartApi.Cart cart = getCart(cartId);
    assertEquals("shopping cart should have 3 items", 3, cart.getItemsCount());
    assertEquals(
        "shopping cart should have expected items",
        List.of(item("a", "Apple", 1), item("b", "Banana", 2), item("c", "Cantaloupe", 3)),
        cart.getItemsList());
  }

  @Test
  public void removeItemsFromCart() throws Exception {
    final String cartId = "cart3";
    addItem(cartId, "a", "Apple", 1);
    addItem(cartId, "b", "Banana", 2);
    ShoppingCartApi.Cart cart1 = getCart(cartId);
    assertEquals("shopping cart should have 2 items", 2, cart1.getItemsCount());
    assertEquals(
        "shopping cart should have expected items",
        List.of(item("a", "Apple", 1), item("b", "Banana", 2)),
        cart1.getItemsList());
    removeItem(cartId, "a", 1);
    ShoppingCartApi.Cart cart2 = getCart(cartId);
    assertEquals(
        "shopping cart should have expected items after removal",
        List.of(item("b", "Banana", 2)),
        cart2.getItemsList());
    assertEquals(
        "shopping cart should have expected items",
        cart2.getItemsList(),
        List.of(item("b", "Banana", 2)));
  }
}