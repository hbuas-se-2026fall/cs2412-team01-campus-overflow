package com.campusoverflow.identity.application;

/**
 * 自助修改个人资料的入参。
 * <p>{@code null} 表示「该项不变」，空串表示「清空该项」——前者用于只改一个字段，
 * 后者用于用户主动删除已填内容。两项都传 {@code null} 时是一次空操作（仍会返回最新资料）。</p>
 */
public record UpdateProfileCommand(String college, String className) {
}
