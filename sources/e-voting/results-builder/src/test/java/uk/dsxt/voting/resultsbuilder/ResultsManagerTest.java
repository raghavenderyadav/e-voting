package uk.dsxt.voting.resultsbuilder;

import org.junit.Ignore;
import org.junit.Test;

public class ResultsManagerTest {

    @Test
    @Ignore
    public void testCheck() {
        ResultsManager manager = new ResultsManager();
        manager.addVote("1,1,2 3 4");
        manager.checkVoting("1");
        manager.checkVoting("2");
        manager.addVote("1,1,2 3 5,6 7 8");
        manager.checkVoting("1");
        manager.addResult("2", "1,,2 3 9,6 7 8");
        manager.addResult("3", "1,,2 3 9");
        manager.checkVoting("1");
    }

}
