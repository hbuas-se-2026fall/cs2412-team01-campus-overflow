package com.campusoverflow.identity.application;

/** 强制下线端口：删除某账号的全部服务端会话（实现：Spring Session 按主体名索引，见 ADR-003）。 */
public interface SessionRevoker {

    /** @return 实际删除的会话数 */
    int revokeAll(String username);
}
