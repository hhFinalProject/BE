package com.sparta.village.domain.product.service;

import com.sparta.village.domain.chat.entity.ChatRoom;
import com.sparta.village.domain.chat.repository.ChatMessageRepository;
import com.sparta.village.domain.chat.repository.ChatRoomRepository;
import com.sparta.village.domain.chat.service.ChatRoomService;
import com.sparta.village.domain.image.service.ImageStorageService;
import com.sparta.village.domain.product.dto.*;
import com.sparta.village.domain.product.entity.Product;
import com.sparta.village.domain.product.repository.ProductRepository;
import com.sparta.village.domain.reservation.dto.AcceptReservationResponseDto;
import com.sparta.village.domain.reservation.repository.ReservationRepository;
import com.sparta.village.domain.reservation.service.ReservationService;
import com.sparta.village.domain.user.entity.User;
import com.sparta.village.domain.user.service.UserService;
import com.sparta.village.domain.zzim.repository.ZzimRepository;
import com.sparta.village.global.exception.CustomException;
import com.sparta.village.global.exception.ErrorCode;
import com.sparta.village.global.exception.ResponseMessage;
import com.sparta.village.global.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ZzimRepository zzimRepository;
    private final ReservationRepository reservationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ImageStorageService imageStorageService;
    private final ReservationService reservationService;
    private final UserService userService;

    @Transactional
    public ResponseEntity<ResponseMessage> getMainPage(UserDetailsImpl userDetails) {
        User user = userDetails == null ? null : userDetails.getUser();
        List<AcceptReservationResponseDto> dealList = reservationService.getAcceptedReservationList();
        List<ProductResponseDto> productList = productRepository.findRandomProduct(8).stream()
                .map(p -> new ProductResponseDto(p, searchPrimeImageUrl(p), getMostProduct(p), getZzim(user, p))).toList();
        List<ProductResponseDto> randomProduct = productRepository.findRandomProduct(6).stream()
                .map(p -> new ProductResponseDto(p, searchPrimeImageUrl(p), getMostProduct(p), getZzim(user, p))).toList();
        int zzimCount = user != null ? zzimRepository.countByUser(user) : 0;
        return ResponseMessage.SuccessResponse("메인페이지 조회되었습니다.", new MainResponseDto(dealList, productList, zzimCount, randomProduct));
    }

    @Transactional
    public ResponseEntity<ResponseMessage> registProduct(User user, ProductRequestDto productRequestDto) {
        Product newProduct = new Product(user, productRequestDto);
        productRepository.saveAndFlush(newProduct);
        imageStorageService.saveImageList(newProduct, imageStorageService.storeFiles(productRequestDto.getImages()));

        return ResponseMessage.SuccessResponse("성공적으로 제품 등록이 되었습니다.", "");
    }

    @Transactional
    public ResponseEntity<ResponseMessage> deleteProduct(Long id, User user) {
        Product product = productRepository.findById(id).orElseThrow(
                () -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.NOT_AUTHOR);
        }


        chatMessageRepository.deleteMessagesByProductId(id);
        chatRoomRepository.deleteByProductId(id);
        reservationRepository.deleteByProductId(id);
        zzimRepository.deleteByProductId(id);
        imageStorageService.deleteImagesByProductId(id);
        productRepository.deleteById(id);

        return ResponseMessage.SuccessResponse("상품 삭제가 되었습니다.", "");
    }

    @Transactional
    public ResponseEntity<ResponseMessage> updateProduct(Long id ,User user, ProductRequestDto productRequestDto) {

        Product product = findProductById(id);
        if (!product.getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.NOT_AUTHOR);
        }

        imageStorageService.deleteImagesByProductId(id);
        imageStorageService.saveImageList(product, imageStorageService.storeFiles(productRequestDto.getImages()));

        product.update(productRequestDto);
        return ResponseMessage.SuccessResponse("상품 수정이 되었습니다.", "");
    }

    @Transactional
    public ResponseEntity<ResponseMessage> detailProduct(UserDetailsImpl userDetails, Long id) {
        User user = userDetails == null ? null : userDetails.getUser();
        Product product = findProductById(id);
        User owner = userService.getUserByUserId(Long.toString(product.getUser().getId()));
        boolean checkOwner = user != null && checkProductOwner(id, user.getId());
        boolean zzimStatus = user != null && zzimRepository.findByProductAndUser(product, user).isPresent();

        ProductDetailResponseDto productDetailResponseDto = new ProductDetailResponseDto(product, checkOwner, zzimStatus,
                zzimRepository.countByProductId(id), imageStorageService.getImageUrlListByProductId(id),
                owner.getNickname(), userService.getUserProfile(owner), reservationService.getReservationList(user, id));

        return ResponseMessage.SuccessResponse("제품 조회가 완료되었습니다.", productDetailResponseDto);
    }

    public ResponseEntity<ResponseMessage> searchProductList(UserDetailsImpl userDetails, String qr, String location) {
        User user = userDetails == null ? null : userDetails.getUser();
        List<Product> productList;

        if (qr == null && location == null) {
            productList = productRepository.findAll();
        } else if (qr == null) {
            productList = productRepository.findByLocationContaining(location);
        } else if (location == null) {
            productList = productRepository.findByTitleContaining(qr);
        } else {
            productList = productRepository.findByTitleContainingAndLocationContaining(qr, location);
        }

        List<ProductResponseDto> responseList = productList.stream()
                .map(product -> new ProductResponseDto(product, searchPrimeImageUrl(product), getMostProduct(product), getZzim(user, product)))
                .toList();

        return ResponseMessage.SuccessResponse("검색 조회가 되었습니다.", responseList);
    }

    @Transactional(readOnly = true)
    public Product findProductById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private String searchPrimeImageUrl(Product product) {
        return imageStorageService.getImageUrlListByProductId(product.getId()).get(0);
    }

    //로그인한 유저가 제품 등록자가 맞는지 체크. 제품을 등록한 판매자이면 true 반환.
    private boolean checkProductOwner(Long productId, Long userId) {
        return productRepository.existsByIdAndUserId(productId, userId);
    }

    private boolean getMostProduct(Product product) {
        List<Object[]> reservationCounts = reservationRepository.countReservationWithProduct();
        reservationCounts.sort((o1, o2) -> Long.compare((Long) o2[1], (Long) o1[1]));

        return reservationCounts.stream()
                .filter(countInfo -> (Long) countInfo[1] >= (Long) reservationCounts.get((int) Math.ceil(reservationCounts.size() * 0.1) - 1)[1])
                .map(countInfo -> productRepository.findById((Long) countInfo[0]))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .anyMatch(topProduct -> topProduct.getId().equals(product.getId()));
    }

    private boolean getZzim(User user, Product product) {
        return user != null && zzimRepository.findByProductAndUser(findProductById(product.getId()), user).isPresent();
    }
}