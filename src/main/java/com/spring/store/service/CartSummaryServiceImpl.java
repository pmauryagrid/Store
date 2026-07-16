package com.spring.store.service;

import com.spring.store.dto.CartItemView;
import com.spring.store.dto.CartResponse;
import com.spring.store.entity.CartItem;
import com.spring.store.entity.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartSummaryServiceImpl implements CartSummaryService {

    @Override
    public CartResponse buildResponse(List<CartItem> items) {
        List<CartItemView> views = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int ordinal = 1;

        for (CartItem item : items) {
            Product product = item.getProduct();
            String title = product != null ? product.getTitle() : null;
            views.add(new CartItemView(ordinal++, title, item.getQuantity()));

            if (product != null && product.getPrice() != null) {
                BigDecimal line = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                subtotal = subtotal.add(line);
            }
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        return new CartResponse(views, subtotal.toPlainString());
    }
}
