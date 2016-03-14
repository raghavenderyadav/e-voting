package uk.dsxt.voting.client.datamodel;

public enum APIException {
    UNKNOWN_EXCEPTION,
    
    WRONG_COOKIE,
    INCORRECT_LOGIN_OR_PASSWORD,
    
    VOTING_NOT_FOUND,
    CLIENT_NOT_FOUND,
    VOTE_NOT_FOUND,
    VOTE_RESULTS_NOT_FOUND
}
