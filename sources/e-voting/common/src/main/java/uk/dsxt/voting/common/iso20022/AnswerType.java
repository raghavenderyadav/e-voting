package uk.dsxt.voting.common.iso20022;

import lombok.Getter;

enum AnswerType {
    FOR("1"),
    AGAINST("2"),
    ABSTAIN("3");

    @Getter
    private final String code;

    AnswerType(String code) {
        this.code=code;
    }

    public static AnswerType getType(String code) {
        for (AnswerType type: AnswerType.values()) {
            if (type.getCode().equals(code))
                return type;
        }
        return null;
    }
}
