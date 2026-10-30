package com.lsoftware.restauco.entity.member.model;

public enum MemberRole {
    /** Everything: menu, tables, team, plus the waiter and kitchen screens. */
    OWNER,
    /** Floor: takes orders, approves QR orders, delivers, closes bills. */
    WAITER,
    /** Kitchen: prepares tickets and marks them ready. */
    COOK
}
