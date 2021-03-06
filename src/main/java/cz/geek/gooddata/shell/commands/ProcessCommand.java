package cz.geek.gooddata.shell.commands;

import com.gooddata.FutureResult;
import com.gooddata.dataload.processes.DataloadProcess;
import com.gooddata.dataload.processes.ProcessExecution;
import com.gooddata.dataload.processes.ProcessExecutionDetail;
import com.gooddata.dataload.processes.ProcessService;
import cz.geek.gooddata.shell.components.GoodDataHolder;
import cz.geek.gooddata.shell.output.RowExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;

import static java.util.Arrays.asList;

/**
 */
@Component
public class ProcessCommand extends AbstractGoodDataCommand {

    @Autowired
    public ProcessCommand(final GoodDataHolder holder) {
        super(holder);
    }

    @CliAvailabilityIndicator({"process upload", "process download", "process list", "process execute"})
    public boolean isAvailable() {
        return holder.hasCurrentProject();
    }

    @CliCommand(value = "process list", help = "List processes")
    public String list() {
        return print(getGoodData().getProcessService().listProcesses(getCurrentProject()), asList("URI", "Name", "Executables"),
                new RowExtractor<DataloadProcess>() {
            @Override
            public List<?> extract(DataloadProcess process) {
                return asList(process.getUri(), process.getName(), process.getExecutables());
            }
        });
    }


    @CliCommand(value = "process upload", help = "Create or update process")
    public String load(
            @CliOption(key = {"uri"}, mandatory = false, help = "Process URI of existing process to be updated") String processUri,
            @CliOption(key = {"name"}, mandatory = false, help = "Process name") String name,
            @CliOption(key = {"type"}, mandatory = false, help = "Process type") String type,
            @CliOption(key = {"source"}, mandatory = true, help = "Process file or directory") File data) {

        final ProcessService service = getGoodData().getProcessService();
        DataloadProcess process;
        if (processUri != null) {
            process = service.getProcessByUri(processUri);
            if (name != null) {
                process.setName(name);
            }
            if (type != null) {
                process.setType(type);
            }
            process = service.updateProcess(getCurrentProject(), process, data);
        } else {
            process = service.createProcess(getCurrentProject(), new DataloadProcess(name, type), data);
        }

        return "Uploaded process: " + process.getUri();
    }


    @CliCommand(value = "process download", help = "Download process")
    public String download(@CliOption(key = {"uri"}, mandatory = true, help = "Process URI") String processUri,
                           @CliOption(key = {"target"}, mandatory = false, help = "Target dir") File target) throws FileNotFoundException {
        final ProcessService service = getGoodData().getProcessService();
        final DataloadProcess process = service.getProcessByUri(processUri);
        if (target == null) {
            target = new File(process.getName() + ".zip");
        }
        service.getProcessSource(process, new FileOutputStream(target));
        return "Downloaded to " + target.getAbsolutePath();
    }

    @CliCommand(value = "process execute", help = "Execute process")
    public String execute(@CliOption(key = {"uri"}, mandatory = true, help = "Process URI") final String processUri,
                          @CliOption(key = {"executable"}, mandatory = true, help = "Executable") final String executable,
                          @CliOption(key = {"wait"}, mandatory = false, help = "Wait for completion",
                                   unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") final boolean wait,
                          @CliOption(key = {"log"}, mandatory = false, help = "Show execution log",
                                  unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") final boolean log) {
        final ProcessService service = getGoodData().getProcessService();
        final DataloadProcess process = service.getProcessByUri(processUri);
        final FutureResult<ProcessExecutionDetail> futureResult = service.executeProcess(new ProcessExecution(process, executable));
        if (wait) {
            System.out.println(futureResult.getPollingUri());
            final ProcessExecutionDetail detail = futureResult.get();
            String result = "Executed process " + detail.getUri() + ": " + detail.getStatus();
            if (log) {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                service.getExecutionLog(detail, out);
                result += "\n" + out.toString();
            }
            return result;
        } else {
            return futureResult.getPollingUri();
        }
    }

}
