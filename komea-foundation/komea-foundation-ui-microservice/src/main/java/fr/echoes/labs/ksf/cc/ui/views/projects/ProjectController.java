/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.echoes.labs.ksf.cc.ui.views.projects;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.google.common.collect.Lists;
import com.tocea.corolla.cqrs.gate.CommandExecutionException;
import com.tocea.corolla.cqrs.gate.Gate;
import com.tocea.corolla.products.commands.CancelFeatureCommand;
import com.tocea.corolla.products.commands.CreateFeatureCommand;
import com.tocea.corolla.products.commands.CreateReleaseCommand;
import com.tocea.corolla.products.commands.DeleteProjectCommand;
import com.tocea.corolla.products.commands.FinishFeatureCommand;
import com.tocea.corolla.products.commands.FinishReleaseCommand;
import com.tocea.corolla.products.dao.IProjectDAO;
import com.tocea.corolla.products.domain.Project;
import com.tocea.corolla.products.exceptions.ProjectNotFoundException;
import com.tocea.corolla.products.utils.ProjectDtoFactory;
import com.tocea.corolla.users.dao.IUserDAO;
import com.tocea.corolla.users.domain.User;
import com.tocea.corolla.users.dto.UserDto;

import fr.echoes.labs.ksf.cc.extensions.gui.project.dashboard.IProjectTabPanel;
import fr.echoes.labs.ksf.cc.extensions.gui.project.dashboard.ProjectDashboardWidget;
import fr.echoes.labs.ksf.cc.extensions.services.project.IValidator;
import fr.echoes.labs.ksf.cc.extensions.services.project.IValidatorResult;
import fr.echoes.labs.ksf.cc.extmanager.projects.ui.IProjectDashboardExtensionManager;
import fr.echoes.labs.ksf.cc.releases.dao.IReleaseDAO;
import fr.echoes.labs.ksf.cc.releases.model.Release;
import fr.echoes.labs.ksf.cc.releases.model.ReleaseState;
import fr.echoes.labs.ksf.cc.sf.commands.CreateProjectAndProductionLineCommand;
import fr.echoes.labs.ksf.cc.sf.dto.SFProjectDTO;
import fr.echoes.labs.ksf.extensions.exceptions.PermissionDeniedException;
import fr.echoes.labs.ksf.extensions.projects.IDeleteProjectApprobator;
import fr.echoes.labs.ksf.extensions.projects.ProjectDto;
import fr.echoes.labs.ksf.users.security.auth.UserDetailsRetrievingService;

/**
 *
 * @author rgalerme
 */
@Controller
public class ProjectController {

    private static String FORM_PROJECT = "projects/form";
    private static String LIST_PAGE = "projects/list";
    private static String VIEW_PAGE = "projects/overview";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectController.class);

    @Autowired
    private IProjectDashboardExtensionManager projectDashboardExtensionManager;

    @Autowired
    private IProjectDAO projectDao;

    @Autowired
    private IUserDAO userDao;

    @Autowired
    private IReleaseDAO releaseDao;

    @Autowired
    private UserDetailsRetrievingService userDetailsRetrievingService;

    @Autowired
    private Gate gate;

    @Autowired(required = false)
    private IValidator[] validators;
    
    @Autowired(required = false)
    private IDeleteProjectApprobator[] deleteProjectApprobators;

    @RequestMapping(value = "/ui/projects/new", method = RequestMethod.POST)
    public ModelAndView createProject(@Valid @ModelAttribute("project") SFProjectDTO newProject, final BindingResult _result) {

        newProject = (SFProjectDTO) _result.getModel().get("project");

        if (newProject == null) {
            return new ModelAndView("redirect:/ui/projects/new");
        }

        if (_result.hasErrors()) {
            final ModelAndView model = new ModelAndView(FORM_PROJECT);
            model.addObject(newProject);
            return model;
        }

        try {

            final Project project = this.gate.dispatch(new CreateProjectAndProductionLineCommand(newProject));

            return new ModelAndView("redirect:/ui/projects/" + project.getKey());

        } catch (final CommandExecutionException ex) {

            _result.addError(new ObjectError("project", ex.getCause().getMessage()));
            final ModelAndView model = new ModelAndView(FORM_PROJECT);
            model.addObject(newProject);
            return model;
        }
    }

    @RequestMapping(value = "/ui/projects/releases/new")
    public ModelAndView createRelease(@RequestParam("projectKey") final String projectKey, @RequestParam("releaseVersion") final String releaseVersion, @RequestParam("releaseId") final String releaseId) {

        final Project project = this.projectDao.findByKey(projectKey);

        if (project == null) {
            throw new ProjectNotFoundException();
        }

        final Release release = new Release();
        release.setProjectId(project.getId());
        release.setReleaseVersion(releaseVersion);
        release.setReleaseId(releaseId);
        release.setState(ReleaseState.STARTED);

        final String currentUserName = this.userDetailsRetrievingService.getCurrentUserLogin();

        this.gate.dispatch(new CreateReleaseCommand(project, currentUserName, releaseVersion));

        this.releaseDao.save(release);

        return new ModelAndView("redirect:/ui/projects/" + project.getKey());
    }

    @RequestMapping(value = "/ui/projects/releases/finish")
    public ModelAndView finishRelease(@RequestParam("projectKey") final String projectKey, @RequestParam("releaseVersion") final String releaseVersion, RedirectAttributes redirectAttributes) {

        final Project project = this.projectDao.findByKey(projectKey);

        if (project == null) {
            throw new ProjectNotFoundException();
        }
        
        final ProjectDto projectDto = ProjectDtoFactory.convert(project);

        final List<IValidatorResult> validateResults = new ArrayList<IValidatorResult>();
        if (this.validators != null) {
            for (final IValidator validator : this.validators) {
                final List<IValidatorResult> result = validator.validateRelease(projectDto, releaseVersion);
                if (result != null) {
                    validateResults.addAll(result);
                }
            }
        }

        if (validateResults.isEmpty()) {
            this.gate.dispatch(new FinishReleaseCommand(project, releaseVersion));
            deleteFromStartedRelease(project.getId(), releaseVersion);

        } else {
            redirectAttributes.addFlashAttribute("validationErrors", validateResults);
        }

        return new ModelAndView("redirect:/ui/projects/" + project.getKey());
    }

    private void deleteFromStartedRelease(String projectId, String releaseVersion) {
        for (final Release release : this.releaseDao.findAll()) {
            if (StringUtils.equals(release.getReleaseVersion(), releaseVersion)
                    && StringUtils.equals(release.getProjectId(), projectId)) {
                this.releaseDao.delete(release);;
            }
        }
    }

    @RequestMapping(value = "/ui/projects/features/new")
    public ModelAndView createFeature(@RequestParam("projectKey") final String projectKey, @RequestParam("featureId") final String featureId, @RequestParam("featureSubject") final String featureSubject) {

        final Project project = this.projectDao.findByKey(projectKey);

        if (project == null) {
            throw new ProjectNotFoundException();
        }

        final String currentUserName = this.userDetailsRetrievingService.getCurrentUserLogin();

        this.gate.dispatch(new CreateFeatureCommand(project, currentUserName, featureId, featureSubject));

        return new ModelAndView("redirect:/ui/projects/" + project.getKey());
    }

    @RequestMapping(value = "/ui/projects/features/finish")
    public ModelAndView finishFeature(@RequestParam("projectKey") final String projectKey, @RequestParam("featureId") final String featureId, @RequestParam("featureSubject") final String featureSubject, RedirectAttributes redirectAttributes) {

        final Project project = this.projectDao.findByKey(projectKey);

        if (project == null) {
            throw new ProjectNotFoundException();
        }

        final ProjectDto projectDto = ProjectDtoFactory.convert(project);
        
        final List<IValidatorResult> validateResults = new ArrayList<IValidatorResult>();
        if (this.validators != null) {
            for (final IValidator validator : this.validators) {
                final List<IValidatorResult> result = validator.validateFeature(projectDto, featureId, featureSubject);
                if (result != null) {
                    validateResults.addAll(result);
                }
            }
        }

        if (validateResults.isEmpty()) {
            this.gate.dispatch(new FinishFeatureCommand(project, featureId, featureSubject));
        } else {
            redirectAttributes.addFlashAttribute("validationErrors", validateResults);
        }

        return new ModelAndView("redirect:/ui/projects/" + project.getKey());
    }

    @RequestMapping(value = "/ui/projects/features/cancel")
    public ModelAndView cancelFeature(@RequestParam("projectKey") final String projectKey, @RequestParam("featureId") final String featureId, @RequestParam("featureSubject") final String featureSubject, RedirectAttributes redirectAttributes) {

        final Project project = this.projectDao.findByKey(projectKey);
        if (project == null) {
            throw new ProjectNotFoundException();
        }

        this.gate.dispatch(new CancelFeatureCommand(project, featureId, featureSubject));

        return new ModelAndView("redirect:/ui/projects/" + project.getKey());
    }

    @RequestMapping(value = "/ui/projects/new")
    public ModelAndView getCreatePage() {

        final SFProjectDTO project = new SFProjectDTO();
        final ModelAndView model = new ModelAndView(FORM_PROJECT);
        model.addObject("project", project);

        final List<Project> projects = this.projectDao.findAllByOrderByNameAsc();

        model.addObject("projects", projects);

        return model;

    }

    @RequestMapping(value = "/ui/projects")
    public ModelAndView getListPage() {
        final ModelAndView model = new ModelAndView(LIST_PAGE);
        final List<Project> findAllProjectsOrderedByName = this.projectDao.findAllByOrderByNameAsc();
        final List<ProjectPagelistDTO> dtoProjectPageList = new ArrayList<>();

        for (final Project pr : findAllProjectsOrderedByName) {
            dtoProjectPageList.add(this.createProjectPageListDTO(pr));
        }

        model.addObject("projects", dtoProjectPageList);
        return model;
    }

    @RequestMapping(value = "/ui/projects/{projectKey}")
    public ModelAndView getProjectPage(@PathVariable final String projectKey) {

        final Project project = this.projectDao.findByKey(projectKey);

        if (project == null) {
            throw new ProjectNotFoundException();
        }

        final ModelAndView model = new ModelAndView(VIEW_PAGE);
        model.addObject("projectData", createProjectPageListDTO(project));

        final List<IProjectTabPanel> panels = Lists.newArrayList();
        final List<ProjectDashboardWidget> widgets = this.projectDashboardExtensionManager.getDashboardWidgets();

        for (final ProjectDashboardWidget widget : widgets) {
            panels.addAll(widget.getTabPanels(projectKey));
        }

        final List<Project> parentProjects = getParentProjects(project);

        model.addObject("widgets", widgets);
        model.addObject("panels", panels);
        model.addObject("parentProjects", parentProjects);

        return model;
    }

    private List<Project> getParentProjects(Project project) {
        final List<Project> parents = new ArrayList<Project>();
        String parentId;
        Project currentProject = project;
        while ((parentId = currentProject.getParentId()) != null) {
            final Project parent = this.projectDao.findOne(parentId);
            parents.add(parent);
            currentProject = parent;
        }

        return Lists.reverse(parents);
    }

    @RequestMapping(value = "/ui/projects/delete/{projectKey}")
    public ModelAndView DeleteProjectPage(@PathVariable final String projectKey) throws PermissionDeniedException {
    	
    	final Project project = this.projectDao.findByKey(projectKey);    	
    	if (project == null) {
    		throw new ProjectNotFoundException();
    	}
    	
    	final UserDto user = this.userDetailsRetrievingService.getCurrentUser();
    	
    	for (final IDeleteProjectApprobator approbator : this.deleteProjectApprobators) {
    		approbator.approve(ProjectDtoFactory.convert(project));
    	}
    	
        this.gate.dispatch(new DeleteProjectCommand(project.getId()));
        return new ModelAndView("redirect:/ui/projects/");
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler({
        ProjectNotFoundException.class
    })
    public void handlePageNotFoundException() {
    }
    
//    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    @ExceptionHandler({ PermissionDeniedException.class })
    public ModelAndView handlePermissionDeniedException(final Exception ex) {
    	ModelAndView model = new ModelAndView("401");
    	LOGGER.error("Permission denied.", ex);
    	model.addObject("exception", ex);
    	return model;
    }

    private ProjectPagelistDTO createProjectPageListDTO(final Project project) {

        User findOne = null;

        if (StringUtils.isNotEmpty(project.getOwnerId())) {
            findOne = this.userDao.findOne(project.getOwnerId());
        }

        if (findOne == null) {
            findOne = new User();
            findOne.setFirstName("");
            findOne.setLastName("");
            findOne.setLogin(project.getOwnerId());
        }

        return new ProjectPagelistDTO(project, findOne);
    }

}
