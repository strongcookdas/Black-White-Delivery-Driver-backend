package com.sparta.blackwhitedeliverydriver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sparta.blackwhitedeliverydriver.dto.OrderGetResponseDto;
import com.sparta.blackwhitedeliverydriver.dto.OrderResponseDto;
import com.sparta.blackwhitedeliverydriver.dto.OrderUpdateRequestDto;
import com.sparta.blackwhitedeliverydriver.entity.Basket;
import com.sparta.blackwhitedeliverydriver.entity.Order;
import com.sparta.blackwhitedeliverydriver.entity.OrderProduct;
import com.sparta.blackwhitedeliverydriver.entity.OrderStatusEnum;
import com.sparta.blackwhitedeliverydriver.entity.User;
import com.sparta.blackwhitedeliverydriver.entity.UserRoleEnum;
import com.sparta.blackwhitedeliverydriver.exception.ExceptionMessage;
import com.sparta.blackwhitedeliverydriver.exception.OrderExceptionMessage;
import com.sparta.blackwhitedeliverydriver.repository.BasketRepository;
import com.sparta.blackwhitedeliverydriver.repository.OrderProductRepository;
import com.sparta.blackwhitedeliverydriver.repository.OrderRepository;
import com.sparta.blackwhitedeliverydriver.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.swing.text.html.Option;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderServiceTest {
    OrderService orderService;

    BasketRepository basketRepository = mock(BasketRepository.class);
    OrderRepository orderRepository = mock(OrderRepository.class);
    OrderProductRepository orderProductRepository = mock(OrderProductRepository.class);
    UserRepository userRepository = mock(UserRepository.class);

    @BeforeEach
    public void setUp() {
        orderService = new OrderService(basketRepository, orderRepository, orderProductRepository, userRepository);
    }

    @Test
    @DisplayName("주문 생성 성공")
    void createOrder() {
        //given
        String username = "user1";
        String basketId = "0eba37e5-a3bc-4053-bef6-2e087cf1e227";
        String orderId = "2b6ad274-8f98-44c2-9321-ea0de46b3ec6";
        String productId = "e7521693-c495-4699-9bc2-7c70d731d214";
        String orderProductId = "1e217cbf-e3ec-4e85-9f9b-42557c1dd079";
        User user = User.builder()
                .username(username)
                .role(UserRoleEnum.CUSTOMER)
                .build();
        Basket basket = Basket.builder()
                .id(UUID.fromString(basketId))
                .user(user)
                .productId(UUID.fromString(productId))
                .quantity(2)
                .build();
        Order order = Order.builder()
                .id(UUID.fromString(orderId))
                .user(user)
                .store(UUID.randomUUID())
                .status(OrderStatusEnum.CREATE)
                .build();
        OrderProduct orderProduct = OrderProduct.builder()
                .id(UUID.fromString(orderProductId))
                .order(order)
                .product(UUID.fromString(productId))
                .quantity(2)
                .price(5000)
                .build();

        given(userRepository.findById(any())).willReturn(Optional.ofNullable(user));
        given(basketRepository.findAllByUser(any())).willReturn(List.of(basket));
        given(orderRepository.save(any())).willReturn(order);
        when(orderProductRepository.saveAll(any())).thenReturn(List.of(orderProduct));
        doNothing().when(basketRepository).deleteAll(any());
        //when
        OrderResponseDto response = orderService.createOrder(username);
        //then
        Assertions.assertEquals(10000, order.getFinalPay());
        Assertions.assertEquals(orderId, response.getOrderId().toString());
    }

    @Test
    @DisplayName("주문 생성 실패1 : 유저가 없는 경우")
    void createOrder_fail1() {
        //given
        String username = "user1";
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        //when & then
        assertThrows(NullPointerException.class, () -> orderService.createOrder(username));
    }

    @Test
    @DisplayName("주문 생성 실패2 : 상품이 존재하지 않는 경우")
    void createOrder_fail2() {
        // Product 엔티티 생성 후 구현 예정
    }

    @Test
    @DisplayName("주문 생성 실패3 : 장바구니가 존재하지 않는 경우")
    void createOrder_fail3() {
        //given
        String username = "user1";
        User user = User.builder()
                .username(username)
                .role(UserRoleEnum.CUSTOMER)
                .build();
        List<Basket> baskets = new ArrayList<>();
        given(userRepository.findById(any())).willReturn(Optional.ofNullable(user));
        when(basketRepository.findAllById(any())).thenReturn(baskets);

        //when & then
        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(username));
    }

    @Test
    @DisplayName("주문 상세 조회 성공")
    void getOrderDetail_success() {
        //given
        UUID orderId = UUID.fromString("2b6ad274-8f98-44c2-9321-ea0de46b3ec6");
        String username = "user1";
        User user = User.builder()
                .username(username)
                .role(UserRoleEnum.CUSTOMER)
                .build();
        Order order = Order.builder()
                .id(orderId)
                .user(user)
                .store(UUID.randomUUID())
                .status(OrderStatusEnum.CREATE)
                .discountAmount(0)
                .discountRate(0)
                .finalPay(10000)
                .build();

        given(userRepository.findById(any())).willReturn(Optional.ofNullable(user));
        given(orderRepository.findById(any())).willReturn(Optional.ofNullable(order));

        //when
        OrderGetResponseDto response = orderService.getOrderDetail(username, orderId);

        //then
        assertEquals(username, response.getUsername());
        assertEquals(orderId, response.getOrderId());
        assertEquals(order.getFinalPay(), response.getFinalPay());
    }

    @Test
    @DisplayName("주문 상세 조회 실패1 : 유저가 없는 경우")
    void getOrderDetail_fail1() {
        //given
        UUID orderId = UUID.fromString("2b6ad274-8f98-44c2-9321-ea0de46b3ec6");
        String username = "user1";

        when(userRepository.findById(any())).thenReturn(Optional.empty());

        //when & then
        Exception exception = assertThrows(NullPointerException.class,
                () -> orderService.getOrderDetail(username, orderId));
        assertEquals(ExceptionMessage.USER_NOT_FOUND.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("주문 상세 조회 실패2 : 주문서가 없는 경우")
    void getOrderDetail_fail2() {
        //given
        UUID orderId = UUID.fromString("2b6ad274-8f98-44c2-9321-ea0de46b3ec6");
        String username = "user1";
        User user = User.builder()
                .username(username)
                .role(UserRoleEnum.CUSTOMER)
                .build();

        given(userRepository.findById(any())).willReturn(Optional.ofNullable(user));

        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        // when & then
        Exception exception = assertThrows(NullPointerException.class,
                () -> orderService.getOrderDetail(username, orderId));

        assertEquals(OrderExceptionMessage.ORDER_NOT_FOUND.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("주문 상세 조회 실패3 : 권한은 CUSTOMER이지만 자신의 주문서가 아닌 경우")
    void getOrderDetail_fail3() {
        //given
        UUID orderId = UUID.fromString("2b6ad274-8f98-44c2-9321-ea0de46b3ec6");
        String username = "user1";
        String username2 = "user2";
        User user = User.builder()
                .username(username)
                .role(UserRoleEnum.CUSTOMER)
                .build();
        User user2 = User.builder()
                .username(username2)
                .role(UserRoleEnum.CUSTOMER)
                .build();
        Order order = Order.builder()
                .id(orderId)
                .user(user)
                .store(UUID.randomUUID())
                .status(OrderStatusEnum.CREATE)
                .discountAmount(0)
                .discountRate(0)
                .finalPay(10000)
                .build();

        given(userRepository.findById(any())).willReturn(Optional.ofNullable(user2));
        given(orderRepository.findById(any())).willReturn(Optional.ofNullable(order));

        // when & then
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> orderService.getOrderDetail(username, orderId));

        assertEquals(OrderExceptionMessage.ORDER_USER_NOT_EQUALS.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("주문 목록 조회 성공")
    void getOrders_success() {
        //given
        UUID orderId = UUID.fromString("2b6ad274-8f98-44c2-9321-ea0de46b3ec6");
        String username = "user1";
        User user = User.builder()
                .username(username)
                .role(UserRoleEnum.CUSTOMER)
                .build();
        Order order = Order.builder()
                .id(orderId)
                .user(user)
                .store(UUID.randomUUID())
                .status(OrderStatusEnum.CREATE)
                .discountAmount(0)
                .discountRate(0)
                .finalPay(10000)
                .build();

        given(userRepository.findById(any())).willReturn(Optional.ofNullable(user));
        when(orderRepository.findAllByUser(any())).thenReturn(List.of(order));

        //when
        List<OrderGetResponseDto> responseList = orderService.getOrders(username);

        //then
        assertEquals(username, responseList.get(0).getUsername());
        assertEquals(orderId, responseList.get(0).getOrderId());
        assertEquals(order.getFinalPay(), responseList.get(0).getFinalPay());
    }

    @Test
    @DisplayName("주문 목록 조회 실패 : 유저가 존재하지 않는 경우")
    void getOrders_fail() {
        //given
        String username = "user1";

        when(userRepository.findById(any())).thenReturn(Optional.empty());

        //when & then
        Exception exception = assertThrows(NullPointerException.class,
                () -> orderService.getOrders(username));

        assertEquals(ExceptionMessage.USER_NOT_FOUND.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("주문 상태 수정 성공")
    void updateOrderStatus_success() {
        //given
        UUID orderId = UUID.randomUUID();
        String username = "owner";
        User user = User.builder()
                .username(username)
                .role(UserRoleEnum.OWNER)
                .build();
        Order order = Order.builder()
                .id(orderId)
                .user(user)
                .store(UUID.randomUUID())
                .status(OrderStatusEnum.CREATE)
                .discountAmount(0)
                .discountRate(0)
                .finalPay(10000)
                .build();
        OrderUpdateRequestDto request = new OrderUpdateRequestDto(orderId, OrderStatusEnum.ACCEPTED);

        given(userRepository.findById(any())).willReturn(Optional.ofNullable(user));
        given(orderRepository.findById(any())).willReturn(Optional.ofNullable(order));

        //when
        OrderResponseDto response = orderService.updateOrderStatus(username, request);

        //then
        assertEquals(OrderStatusEnum.ACCEPTED, order.getStatus());
        assertEquals(orderId, response.getOrderId());
    }

    @Test
    @DisplayName("주문 상태 수정 실패1 : 유저가 없는 경우")
    void updateOrderStatus_fail1() {
        //given
        String username = "owner";
        UUID orderId = UUID.fromString("2b6ad274-8f98-44c2-9321-ea0de46b3ec6");
        OrderUpdateRequestDto request = new OrderUpdateRequestDto(orderId, OrderStatusEnum.ACCEPTED);

        when(userRepository.findById(any())).thenReturn(Optional.empty());

        //when & then
        Exception exception = assertThrows(NullPointerException.class,
                () -> orderService.updateOrderStatus(username, request));
        assertEquals(ExceptionMessage.USER_NOT_FOUND.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("주문 상태 수정 실패2 : 주문서가 없는 경우")
    void updateOrderStatus_fail2() {
        //given
        UUID orderId = UUID.randomUUID();
        String username = "owner";
        User user = User.builder()
                .username(username)
                .role(UserRoleEnum.CUSTOMER)
                .build();
        OrderUpdateRequestDto request = new OrderUpdateRequestDto(orderId, OrderStatusEnum.ACCEPTED);

        given(userRepository.findById(any())).willReturn(Optional.ofNullable(user));

        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        // when & then
        Exception exception = assertThrows(NullPointerException.class,
                () -> orderService.updateOrderStatus(username, request));

        assertEquals(OrderExceptionMessage.ORDER_NOT_FOUND.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("주문 상태 수정 실패3 : 주문서의 점포 주인과 유저가 다른 경우")
    void updateOrderStatus_fail3() {
        // code
    }

    @Test
    @DisplayName("주문 취소 성공")
    void deleteOrder_success() {
        //given
        String username = "user";
        UUID basketId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID orderProductId = UUID.randomUUID();

        User user = User.builder()
                .username(username)
                .role(UserRoleEnum.CUSTOMER)
                .build();
        Basket basket = Basket.builder()
                .id(basketId)
                .user(user)
                .productId(productId)
                .quantity(2)
                .build();
        Order order = Order.builder()
                .id(orderId)
                .user(user)
                .store(UUID.randomUUID())
                .status(OrderStatusEnum.CREATE)
                .build();
        OrderProduct orderProduct = OrderProduct.builder()
                .id(orderProductId)
                .order(order)
                .product(productId)
                .quantity(2)
                .price(5000)
                .build();

        given(userRepository.findById(any())).willReturn(Optional.ofNullable(user));
        given(orderRepository.findById(any())).willReturn(Optional.ofNullable(order));
        when(orderProductRepository.findAllByOrder(any())).thenReturn(List.of(orderProduct));
        when(basketRepository.save(any())).thenReturn(Optional.ofNullable(basket));
        doNothing().when(orderProductRepository).deleteAll(any());
        doNothing().when(orderRepository).delete(any());

        //when
        OrderResponseDto response = new OrderResponseDto(order.getId());

        //then
        assertEquals(order.getId(), response.getOrderId());
    }

    @Test
    @DisplayName("주문 취소 실패1 : 유저가 존재하지 않는 경우")
    void deleteOrder_fail1() {
        //given
        String username = "user";
        UUID orderId = UUID.randomUUID();
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        //when & then
        Exception exception = assertThrows(NullPointerException.class,
                () -> orderService.deleteOrder(username, orderId));
        assertEquals(ExceptionMessage.USER_NOT_FOUND.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("주문 취소 실패2 : 주문이 존재하지 않는 경우")
    void deleteOrder_fail2() {
        //given
        UUID orderId = UUID.randomUUID();
        String username = "user";
        User user = User.builder()
                .username(username)
                .role(UserRoleEnum.CUSTOMER)
                .build();

        given(userRepository.findById(any())).willReturn(Optional.ofNullable(user));

        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        //when & then
        Exception exception = assertThrows(NullPointerException.class,
                () -> orderService.deleteOrder(username, orderId));
        assertEquals(OrderExceptionMessage.ORDER_NOT_FOUND.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("주문 취소 실패3 : 주문한 유저와 api를 호출한 유저가 일치하지 않을 때")
    void deleteOrder_fail3() {
        //given
        String username1 = "user1";
        String username2 = "user2";
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID orderProductId = UUID.randomUUID();

        User user1 = User.builder()
                .username(username1)
                .role(UserRoleEnum.CUSTOMER)
                .build();
        User user2 = User.builder()
                .username(username2)
                .role(UserRoleEnum.CUSTOMER)
                .build();
        Order order = Order.builder()
                .id(orderId)
                .user(user1)
                .store(UUID.randomUUID())
                .status(OrderStatusEnum.CREATE)
                .build();

        given(userRepository.findById(any())).willReturn(Optional.ofNullable(user2));
        given(orderRepository.findById(any())).willReturn(Optional.ofNullable(order));

        //when & then
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> orderService.deleteOrder(username2, orderId));
        assertEquals(OrderExceptionMessage.ORDER_USER_NOT_EQUALS.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("주문 취소 실패4 : 주문의 상태가 CREATE, PENDING이 아닐 때")
    void deleteOrder_fail4() {
        //given
        String username = "user1";
        UUID orderId = UUID.randomUUID();

        User user = User.builder()
                .username(username)
                .role(UserRoleEnum.CUSTOMER)
                .build();
        Order order = Order.builder()
                .id(orderId)
                .user(user)
                .store(UUID.randomUUID())
                .status(OrderStatusEnum.ACCEPTED)
                .build();

        given(userRepository.findById(any())).willReturn(Optional.ofNullable(user));
        given(orderRepository.findById(any())).willReturn(Optional.ofNullable(order));

        //when & then
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> orderService.deleteOrder(username, orderId));
        assertEquals(OrderExceptionMessage.ORDER_UNABLE_DELETE_STATUS.getMessage(), exception.getMessage());
    }
}