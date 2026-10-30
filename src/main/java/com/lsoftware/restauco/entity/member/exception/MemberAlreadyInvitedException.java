package com.lsoftware.restauco.entity.member.exception;

import com.lsoftware.restauco.entity.exception.ConflictException;

public class MemberAlreadyInvitedException extends ConflictException {
    public MemberAlreadyInvitedException() {
        super("MEMBER_EXISTS", "That email is already on the team");
    }
}
