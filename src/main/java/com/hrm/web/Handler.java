package com.hrm.web;

/** A request handler registered on a {@link Router}. */
@FunctionalInterface
public interface Handler {

    void handle(WebContext ctx) throws Exception;
}
