package com.hrm.web;

@FunctionalInterface
public interface Handler {

    void handle(WebContext ctx) throws Exception;
}
