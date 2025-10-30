package com.study.domain.common;

// Sort.java —— “按什么字段、什么方向排”

import java.util.*;

public final class Sort {
    public enum Direction { ASC, DESC }

    public static final class Order {
        private final String property;
        private final Direction direction;
        public Order(String property, Direction direction) {
            if (property == null || property.isBlank()) throw new IllegalArgumentException("property 为空");
            this.property = property; this.direction = Objects.requireNonNull(direction);
        }
        public String property() { return property; }
        public Direction direction() { return direction; }
        @Override public String toString(){ return property + " " + direction; }
    }

    private final List<Order> orders;
    public Sort(List<Order> orders) { this.orders = List.copyOf(orders); }

    public static Sort by(String property) { return new Sort(List.of(new Order(property, Direction.ASC))); }
    public static Sort by(String property, Direction dir) { return new Sort(List.of(new Order(property, dir))); }
    public static Sort of(Order... orders){ return new Sort(List.of(orders)); }

    public List<Order> orders() { return orders; }
    public boolean isUnsorted(){ return orders.isEmpty(); }

    @Override public String toString(){ return orders.toString(); }
}
