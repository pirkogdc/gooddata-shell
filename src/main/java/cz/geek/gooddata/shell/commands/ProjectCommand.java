package cz.geek.gooddata.shell.commands;

import com.gooddata.project.Project;
import com.gooddata.project.ProjectService;
import cz.geek.gooddata.shell.components.GoodDataHolder;
import cz.geek.gooddata.shell.output.RowExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.Arrays.asList;

@Component
public class ProjectCommand extends AbstractGoodDataCommand {

    @Autowired
    public ProjectCommand(final GoodDataHolder holder) {
        super(holder);
    }

    @CliAvailabilityIndicator({"project list", "project create", "project delete", "project use"})
    public boolean isAvailable() {
        return holder.hasGoodData();
    }

    @CliCommand(value = "project list", help = "List GoodData projects")
    public String list() {
        return print(getGoodData().getProjectService().getProjects(), asList("URI", "Title"), new RowExtractor<Project>() {
            @Override
            public List<?> extract(Project project) {
                return asList(project.getUri(), project.getTitle());
            }
        });
    }

    @CliCommand(value = "project create", help = "Create GoodData project")
    public String create(
            @CliOption(key = {"title"}, mandatory = true, help = "Title") String title,
            @CliOption(key = {"token"}, mandatory = true, help = "Token") String token,
            @CliOption(key = {"template"}, mandatory = false, help = "Title") String template
    ) {
        final Project p = new Project(title, token);
        p.setProjectTemplate(template);
        final Project project = getGoodData().getProjectService().createProject(p).get();
        holder.setCurrentProject(project);
        return "Created project: " + project.getUri();
    }

    @CliCommand(value = "project use", help = "Get or set current GoodData project")
    public String project(@CliOption(key = {""}, mandatory = false, help = "Project id or uri") String project) {
        if (project !=  null) {
            final Project p = getProject(project);
            holder.setCurrentProject(p);
        }
        if (holder.hasCurrentProject()) {
            return "Current project: " + getCurrentProject().getUri();
        } else {
            return "No current project";
        }
    }

    @CliCommand(value = "project delete", help = "Delete GoodData project")
    public String delete(@CliOption(key = {""}, mandatory = false, help = "Project id or uri") String project) {
        final Project p = getProject(project);
        getGoodData().getProjectService().removeProject(p);
        return "Removed " + p.getUri();
    }

    private Project getProject(final String project) {
        final ProjectService service = getGoodData().getProjectService();
        return Project.TEMPLATE.matches(project) ? service.getProjectByUri(project) : service.getProjectById(project);
    }

}
