package fr.echoes.labs.ksf.cc.plugins.redmine.extensions;

import com.taskadapter.redmineapi.bean.Version;
import fr.echoes.labs.ksf.cc.plugins.redmine.services.IRedmineService;
import fr.echoes.labs.ksf.cc.plugins.redmine.services.RedmineConfigurationService;
import fr.echoes.labs.ksf.cc.plugins.redmine.services.RedmineErrorHandlingService;
import fr.echoes.labs.ksf.cc.plugins.redmine.utils.RedmineConstants;
import fr.echoes.labs.ksf.extensions.annotations.Extension;
import fr.echoes.labs.ksf.extensions.projects.IProjectLifecycleExtension;
import fr.echoes.labs.ksf.extensions.projects.NotifyResult;
import fr.echoes.labs.ksf.extensions.projects.ProjectDto;
import fr.echoes.labs.ksf.users.security.api.CurrentUserService;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

/**
 * @author dcollard
 *
 */
@Order(value = 1)
@Extension
public class RedmineProjectLifeCycleExtension implements IProjectLifecycleExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedmineProjectLifeCycleExtension.class);

    @Autowired
    private RedmineErrorHandlingService errorHandler;

    @Autowired
    private IRedmineService redmineService;

    @Autowired
    private RedmineConfigurationService configurationService;

    @Autowired
    private CurrentUserService currentUserService;

    @Override
    public NotifyResult notifyCreatedProject(final ProjectDto project) {
        
    	final String logginName = this.currentUserService.getCurrentUserLogin();

        if (StringUtils.isEmpty(logginName)) {
            LOGGER.error("[Redmine] No user found. Aborting project creation in Redmine module");
            return NotifyResult.CONTINUE;
        }

        LOGGER.info("[Redmine] project {} creation detected [demanded by: {}]", project.getKey(), logginName);
        try {

            this.redmineService.createProject(project, logginName);

        } catch (final Exception ex) {
            LOGGER.error("[Redmine] Failed to create project {} ", project.getName(), ex);
            this.errorHandler.registerError("Unable to create Redmine project.");
        }
        return NotifyResult.CONTINUE;
    }

    @Override
    public NotifyResult notifyDeletedProject(final ProjectDto project) {

        try {

            this.redmineService.deleteProject(project);

        } catch (final Exception ex) {
            LOGGER.error("[Redmine] Failed to delete project {} ", project.getName(), ex);
            this.errorHandler.registerError("Unable to delete Redmine project.");
        }
        return NotifyResult.CONTINUE;
    }

    @Override
    public NotifyResult notifyDuplicatedProject(ProjectDto _project) {
        return NotifyResult.CONTINUE;
    }

    @Override
    public NotifyResult notifyUpdatedProject(ProjectDto _project) {
        return NotifyResult.CONTINUE;
    }

    @Override
    public NotifyResult notifyCreatedRelease(ProjectDto project, String releaseVersion, String username) {
//		try {
//
//			final String releaseTicketSubject = createReleaseTicketSubject(project, releaseVersion);
//
//			this.redmineService.createTicket(project, releaseVersion, releaseTicketSubject, username, this.configurationService.getTaskTrackerId());
//		} catch (final RedmineExtensionException e) {
//			LOGGER.error("[Redmine] Failed to create a ticket for the release {} of the project {}", releaseVersion, project.getName());
//			this.errorHandler.registerError("Failed to create a Redmine ticket for the release.");
//		}
        return NotifyResult.CONTINUE;
    }

    private String createReleaseTicketSubject(ProjectDto project, String releaseVersion) {
        final Map<String, String> variables = new HashMap<String, String>(2);
        variables.put("projectName", project.getName());
        variables.put("releaseVersion", releaseVersion);
        return replaceVariables(this.configurationService.getReleaseTicketMessagePattern(), variables);
    }

    @Override
    public NotifyResult notifyCreatedFeature(ProjectDto project, String ticketId,
            String featureSubject, String username) {

        try {
            this.redmineService.changeStatus(ticketId, this.configurationService.getFeatureStatusAssignedId(), username);

        } catch (final Exception ex) {
            LOGGER.error("[Redmine] Failed to change ticket #{} status", ticketId, ex);
            this.errorHandler.registerError("Failed to change Redmine ticket status.");
        }
        return NotifyResult.CONTINUE;
    }

    @Override
    public NotifyResult notifyFinishedFeature(ProjectDto projectDto, String ticketId,
            String featureSubject) {
        try {
            this.redmineService.changeStatus(ticketId, this.configurationService.getFeatureStatusClosedId(), null);

        } catch (final Exception ex) {
            LOGGER.error("[Redmine] Failed to change ticket #{} status", ticketId, ex);
            this.errorHandler.registerError("Failed to change Redmine ticket status.");
        }
        return NotifyResult.CONTINUE;
    }

    @Override
    public NotifyResult notifyCanceledFeature(ProjectDto projectDto, String ticketId,
            String featureSubject) {
        try {
            this.redmineService.rejectIssue(ticketId, null);
        } catch (final Exception ex) {
            LOGGER.error("[Redmine] Failed to reject ticket #{} ", ticketId, ex);
            this.errorHandler.registerError("Failed to reject Redmine ticket #" + ticketId);
        }
        return NotifyResult.CONTINUE;
    }

    @Override
    public NotifyResult notifyFinishedRelease(ProjectDto project, String releaseName) {

        try {
            this.redmineService.changeVersionStatus(project, releaseName, Version.STATUS_CLOSED);

        } catch (final Exception ex) {
            LOGGER.error("[Redmine] Failed to change version '{}' state status", releaseName, ex);
            this.errorHandler.registerError("Failed to change Redmine version state.");
        }

        return NotifyResult.CONTINUE;
    }

    private String replaceVariables(String str, Map<String, String> variables) {
        final StrSubstitutor sub = new StrSubstitutor(variables);
        sub.setVariablePrefix("%{");
        return sub.replace(str);
    }

    @Override
    public String getName() {
        return RedmineConstants.ID;
    }

}
