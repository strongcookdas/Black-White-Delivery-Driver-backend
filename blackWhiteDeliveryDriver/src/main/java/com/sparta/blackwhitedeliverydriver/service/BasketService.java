package com.sparta.blackwhitedeliverydriver.service;

import com.sparta.blackwhitedeliverydriver.dto.BasketGetResponseDto;
import com.sparta.blackwhitedeliverydriver.dto.BasketAddRequestDto;
import com.sparta.blackwhitedeliverydriver.dto.BasketResponseDto;
import com.sparta.blackwhitedeliverydriver.dto.BasketUpdateRequestDto;
import com.sparta.blackwhitedeliverydriver.entity.Basket;
import com.sparta.blackwhitedeliverydriver.entity.Product;
import com.sparta.blackwhitedeliverydriver.entity.Store;
import com.sparta.blackwhitedeliverydriver.entity.User;
import com.sparta.blackwhitedeliverydriver.exception.BasketExceptionMessage;
import com.sparta.blackwhitedeliverydriver.exception.ExceptionMessage;
import com.sparta.blackwhitedeliverydriver.repository.BasketRepository;
import com.sparta.blackwhitedeliverydriver.repository.ProductRepository;
import com.sparta.blackwhitedeliverydriver.repository.StoreRepository;
import com.sparta.blackwhitedeliverydriver.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BasketService {

    private final BasketRepository basketRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;

    @Transactional
    public BasketResponseDto addProductToBasket(String username, BasketAddRequestDto request) {
        // 유저가 유효성 체크
        User user = userRepository.findById(username)
                .orElseThrow(() -> new NullPointerException(ExceptionMessage.USER_NOT_FOUND.getMessage()));

        // 같은 지점에서 담은 상품인지 체크
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new NullPointerException("점포를 찾을 수 없습니다."));

        // 상품이 유효성, 중복 체크
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NullPointerException("상품을 찾을 수 없습니다."));

        // 담은 상품이 중복된 상품인지 체크
        List<Basket> baskets = basketRepository.findAllByUser(user);
        checkDuplicatedProduct(product, baskets);

        // 담은 상품의 지점이 장바구니 상품의 지점과 다른지 체크
        checkProductStore(product, baskets);

        Basket basket = Basket.ofUserAndStoreAndRequest(user, store, product, request);

        basket = basketRepository.save(basket);

        return BasketResponseDto.fromBasket(basket);
    }


    @Transactional
    public BasketResponseDto removeProductFromBasket(String username, UUID basketId) {
        //유저 유효성 검사
        User user = userRepository.findById(username)
                .orElseThrow(() -> new NullPointerException(ExceptionMessage.USER_NOT_FOUND.getMessage()));

        //장바구니 유효성 검사
        Basket basket = basketRepository.findById(basketId).orElseThrow(() ->
                new NullPointerException(BasketExceptionMessage.BASKET_NOT_FOUND.getMessage()));

        //유저와 장바구니 유저 체크
        checkBasketUser(user, basket);

        basketRepository.delete(basket);

        return BasketResponseDto.fromBasket(basket);
    }

    public List<BasketGetResponseDto> getBaskets(String username) {
        // 유저 유효성 검증
        User user = userRepository.findById(username)
                .orElseThrow(() -> new NullPointerException(ExceptionMessage.USER_NOT_FOUND.getMessage()));

        // 유저, 장바구니 join
        List<Basket> basketList = basketRepository.findAllByUser(user);

        return basketList.stream().map(BasketGetResponseDto::fromBasket).collect(Collectors.toList());
    }

    @Transactional
    public BasketResponseDto updateBasket(String username, BasketUpdateRequestDto request) {
        //유저 유효성 검사
        User user = userRepository.findById(username)
                .orElseThrow(() -> new NullPointerException(ExceptionMessage.USER_NOT_FOUND.getMessage()));

        //장바구니 유효성 검사
        Basket basket = basketRepository.findById(request.getBasketId()).orElseThrow(() ->
                new NullPointerException(BasketExceptionMessage.BASKET_NOT_FOUND.getMessage()));

        //장바구니 유저와 api 호출 유저 체크
        checkBasketUser(user, basket);

        basket.updateBasketOfQuantity(request.getQuantity());
        basketRepository.save(basket);

        return BasketResponseDto.fromBasket(basket);
    }

    private void checkBasketUser(User user, Basket basket) {
        if (!user.getUsername().equals(basket.getUser().getUsername())) {
            throw new IllegalArgumentException(BasketExceptionMessage.BASKET_USER_NOT_EQUALS.getMessage());
        }
    }

    private void checkDuplicatedProduct(Product product, List<Basket> baskets) {
        for (Basket basket : baskets) {
            UUID productId = basket.getProduct().getProductId();
            if (productId.equals(product.getProductId())) {
                throw new IllegalArgumentException(BasketExceptionMessage.BASKET_DUPLICATED.getMessage());
            }
        }
    }

    private void checkProductStore(Product product, List<Basket> baskets) {
        for (Basket basket : baskets) {
            UUID storeId = basket.getProduct().getStore().getStoreId();
            if (!storeId.equals(product.getStore().getStoreId())) {
                throw new IllegalArgumentException(BasketExceptionMessage.BASKET_DIFFERENT_STORE.getMessage());
            }
        }
    }
}
