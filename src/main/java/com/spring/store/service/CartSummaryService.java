package com.spring.store.service;

import com.spring.store.dto.CartResponse;
import com.spring.store.entity.CartItem;

import java.util.List;

public interface CartSummaryService {
    CartResponse buildResponse(List<CartItem> items);
}
