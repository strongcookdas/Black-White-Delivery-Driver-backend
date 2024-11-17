package com.sparta.blackwhitedeliverydriver.controller;

import com.sparta.blackwhitedeliverydriver.dto.OrderAddRequestDto;
import com.sparta.blackwhitedeliverydriver.dto.OrderGetDetailResponseDto;
import com.sparta.blackwhitedeliverydriver.dto.OrderGetResponseDto;
import com.sparta.blackwhitedeliverydriver.dto.OrderResponseDto;
import com.sparta.blackwhitedeliverydriver.dto.OrderUpdateRequestDto;
import com.sparta.blackwhitedeliverydriver.security.UserDetailsImpl;
import com.sparta.blackwhitedeliverydriver.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Secured({"ROLE_CUSTOMER"})
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                        @RequestBody @Valid OrderAddRequestDto request) {
        //주문서 생성
        OrderResponseDto response = orderService.createOrder(userDetails.getUsername(), request);
        //201 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Secured({"ROLE_CUSTOMER", "ROLE_MASTER", "ROLE_MANAGER"})
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderGetDetailResponseDto> getOrderDetail(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable UUID orderId) {
        //주문서 상세 조회
        OrderGetDetailResponseDto response = orderService.getOrderDetail(userDetails.getUsername(), orderId);
        //200 반환
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Secured({"ROLE_CUSTOMER", "ROLE_MASTER", "ROLE_MANAGER"})
    @GetMapping
    public ResponseEntity<List<OrderGetResponseDto>> getOrders(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        //주문 목록 조회
        List<OrderGetResponseDto> responseList = orderService.getOrders(userDetails.getUsername());
        //200 반환
        return ResponseEntity.status(HttpStatus.OK).body(responseList);
    }

    @Secured({"ROLE_OWNER", "ROEL_MASTER", "ROLE_MANAGER"})
    @PutMapping
    public ResponseEntity<OrderResponseDto> updateOrderStatus(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                              @RequestBody OrderUpdateRequestDto request) {
        //주문 상태 변경
        OrderResponseDto response = orderService.updateOrderStatus(userDetails.getUsername(), request);
        //200 반환
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Secured({"ROLE_CUSTOMER", "ROLE_MASTER", "ROLE_MANAGER"})
    @DeleteMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> deleteOrder(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                        @PathVariable UUID orderId) {
        //주문 취소
        OrderResponseDto response = orderService.deleteOrder(userDetails.getUsername(), orderId);
        //200 반환
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}