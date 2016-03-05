package uk.dsxt.voting.common.registries;

import uk.dsxt.voting.common.domain.dataModel.Participant;
import uk.dsxt.voting.common.utils.InternalLogicException;
import uk.dsxt.voting.common.utils.PropertiesHelper;

import java.util.Properties;

public class FileRegisterServer extends SimpleRegisterServer {

    public FileRegisterServer(Properties properties, String subdirectory) throws InternalLogicException {
        super(PropertiesHelper.loadResource(properties, subdirectory, "participants.filepath", Participant[].class));
    }
}
