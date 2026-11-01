package com.lavrel.user.domain;

/**
 * What a user may do in the app. Everybody starts as USER; an ADMIN promotes people
 * (PATCH /api/admin/users/{publicId}/role) or the database is edited by hand.
 * SUPPORT has no extra permissions yet: it is reserved for the support tools that will come later.
 */
public enum Role {
    USER, SUPPORT, ADMIN
}
