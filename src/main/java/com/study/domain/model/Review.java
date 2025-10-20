package com.study.domain.model;

public class Review {
    private String id;
    private String comment;
    private rStatus status;

    public enum rStatus {Good, Bad, Normal}
    public  rStatus getStatus() {return this.status;}
}
