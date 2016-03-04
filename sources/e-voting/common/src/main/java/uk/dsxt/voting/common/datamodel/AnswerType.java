package uk.dsxt.voting.common.datamodel;

import lombok.Getter;

public enum AnswerType {
    FOR(1),
    AGAINST(2),
    ABSTAIN(3);

    @Getter
    private final int code;

    private AnswerType(int code) {
        this.code=code;
    }

    public static AnswerType getType(int code) {
        for (AnswerType type: AnswerType.values()) {
            if (type.getCode() == code)
                return type;
        }
        return null;
    }
}
