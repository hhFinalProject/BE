package com.sparta.village.domain.product.controller;

import com.sparta.village.domain.product.dto.ProductRequestDto;
import com.sparta.village.domain.product.service.ProductService;
import com.sparta.village.domain.user.entity.User;
import com.sparta.village.global.exception.ResponseMessage;
import com.sparta.village.global.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping("/products")
    public ResponseEntity<ResponseMessage> registProduct(@AuthenticationPrincipal UserDetailsImpl userDetails, @RequestBody ProductRequestDto productRequestDto) {
        return productService.registProduct(userDetails.getUser(), productRequestDto);
    }
}