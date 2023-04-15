package com.sparta.village.domain.reservation.service;

import com.sparta.village.domain.image.service.ImageStorageService;
import com.sparta.village.domain.product.entity.Product;
import com.sparta.village.domain.product.repository.ProductRepository;
import com.sparta.village.domain.reservation.dto.*;
import com.sparta.village.domain.reservation.entity.Reservation;
import com.sparta.village.domain.reservation.repository.ReservationRepository;
import com.sparta.village.domain.user.entity.User;
import com.sparta.village.global.exception.CustomException;
import com.sparta.village.global.exception.ErrorCode;
import com.sparta.village.global.exception.ResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;


@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ProductRepository productRepository;
    private final ImageStorageService imageStorageService;

    @Transactional
    public ResponseEntity<ResponseMessage> reserve(Long productId, ReservationRequestDto requestDto, User user) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        if (reservationRepository.checkOverlapDateByProduct(product, requestDto.getStartDate(), requestDto.getEndDate())) {
            throw new CustomException(ErrorCode.DUPLICATE_RESERVATION_DATE);
        }
        reservationRepository.saveAndFlush(new Reservation(product, user, requestDto));
        return ResponseMessage.SuccessResponse("예약 되었습니다.", "");
    }

    @Transactional
    public ResponseEntity<ResponseMessage> deleteReservation(Long id, User user) {
        Reservation reservation = findReservationById(id);
        if (!checkReservationOwner(reservation, user)) {
            throw new CustomException(ErrorCode.NOT_AUTHOR);
        }
        reservationRepository.deleteById(id);
        return ResponseMessage.SuccessResponse("예약 취소되었습니다.", "");
    }

    @Transactional
    public ResponseEntity<ResponseMessage> changeStatus(Long id, StatusRequestDto requestDto, User user) {
        Reservation reservation = findReservationById(id);
        if (reservationRepository.checkProductOwner(id, user)) {
            throw new CustomException(ErrorCode.NOT_SELLER);
        }
        reservationRepository.updateStatus(reservation.getId(), requestDto.getStatus());
        return ResponseMessage.SuccessResponse("상태 변경되었습니다.", "");
    }

    private Reservation findReservationById(Long id) {
        return reservationRepository.findById(id).orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND));
    }

    private boolean checkReservationOwner(Reservation reservation, User user) {
        return user != null && reservation.getUser().getId().equals(user.getId()) && reservation.getStatus().equals("waiting");
    }


    public List<ReservationResponseDto> getReservationList(User user, Long id){
        return reservationRepository.findByProductId(id).stream()
                .map(r -> new ReservationResponseDto(r.getId(), r.getStartDate(), r.getEndDate(), r.getStatus(),
                        r.getUser().getNickname(), checkReservationOwner(r, user))).toList();
    }

    public List<AcceptReservationResponseDto> getAcceptedReservationList() {
        return reservationRepository.findByStatus("accepted").stream()
                .map(r -> new AcceptReservationResponseDto(r.getId(), r.getUser().getNickname(), r.getProduct().getUser().getNickname())).toList();
    }
}









