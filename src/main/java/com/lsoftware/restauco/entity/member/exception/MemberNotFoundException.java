package com.lsoftware.restauco.entity.member.exception;

import com.lsoftware.restauco.entity.exception.NotFoundException;

public class MemberNotFoundException extends NotFoundException {
    public MemberNotFoundException() {
        super("MEMBER_NOT_FOUND", "Team member not found");
    }
}
