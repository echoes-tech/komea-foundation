/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.echoes.lab.ksf.cc.ui.views.projects;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.collect.Lists;
import com.tocea.corolla.cqrs.gate.CommandExecutionException;
import com.tocea.corolla.cqrs.gate.Gate;
import com.tocea.corolla.products.dao.IProjectDAO;
import com.tocea.corolla.products.domain.Project;
import com.tocea.corolla.products.exceptions.ProjectNotFoundException;
import com.tocea.corolla.users.dao.IUserDAO;
import com.tocea.corolla.users.domain.User;

import fr.echoes.lab.ksf.cc.extensions.gui.project.dashboard.IProjectTabPanel;
import fr.echoes.lab.ksf.cc.extensions.gui.project.dashboard.ProjectDashboardWidget;
import fr.echoes.lab.ksf.cc.extmanager.projects.ui.IProjectDashboardExtensionManager;
import fr.echoes.lab.ksf.cc.sf.commands.CreateProjectAndProductionLineCommand;
import fr.echoes.lab.ksf.cc.sf.dto.SFProjectDTO;

/**
 *
 * @author rgalerme
 */
@Controller
public class ProjectController {
	
	private static String FORM_PROJECT 	= "projects/form";
	private static String LIST_PAGE		= "projects/list";
	private static String VIEW_PAGE		= "projects/overview";
	
	@Autowired
	private IProjectDashboardExtensionManager projectDashboardExtensionManager;
	
	@Autowired
	IProjectDAO projectDao;
	
	@Autowired
	IUserDAO userDao;
	
	@Autowired
	Gate gate;
	
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
			
			final Project project = gate.dispatch(new CreateProjectAndProductionLineCommand(newProject));
			
			return new ModelAndView("redirect:/ui/projects/" + project.getKey());
			
		} catch (final CommandExecutionException ex) {
			
			_result.addError(new ObjectError("project", ex.getCause().getMessage()));
			final ModelAndView model = new ModelAndView(FORM_PROJECT);
			model.addObject(newProject);
			return model;
		}
	}
	
	@RequestMapping(value = "/ui/projects/new")
	public ModelAndView getCreatePage() {
		
		final SFProjectDTO project = new SFProjectDTO();
		final ModelAndView model = new ModelAndView(FORM_PROJECT);
		model.addObject("project", project);
		return model;
		
	}
	
	@RequestMapping(value = "/ui/projects")
	public ModelAndView getListPage() {
		final ModelAndView model = new ModelAndView(LIST_PAGE);
		final List<Project> findAll = projectDao.findAll();
		final List<ProjectPagelistDTO> projectsList = new ArrayList<>();
		
		for (final Project pr : findAll) {
			projectsList.add(createProjectPageListDTO(pr));
		}
		
		model.addObject("projects", projectsList);
		return model;
	}
	
	@RequestMapping(value = "/ui/projects/{projectKey}")
	public ModelAndView getProjectPage(@PathVariable final String projectKey) {
		
		final Project project = projectDao.findByKey(projectKey);
		
		if (project == null) {
			throw new ProjectNotFoundException();
		}
		
		final ModelAndView model = new ModelAndView(VIEW_PAGE);
		model.addObject("projectData", createProjectPageListDTO(project));
		
		final List<IProjectTabPanel> panels = Lists.newArrayList();
		final List<ProjectDashboardWidget> widgets = projectDashboardExtensionManager.getDashboardWidgets();
		
		for(final ProjectDashboardWidget widget : widgets) {
			panels.addAll(widget.getTabPanels());
		}
		
		model.addObject("widgets", widgets);
		model.addObject("panels", panels);
		
		return model;
	}
	
	@ResponseStatus(value = HttpStatus.NOT_FOUND)
	@ExceptionHandler({
		ProjectNotFoundException.class
	})
	public void handlePageNotFoundException() {
	}
	
	private ProjectPagelistDTO createProjectPageListDTO(final Project project) {
		
		User findOne = null;
		
		if (StringUtils.isNotEmpty(project.getOwnerId())) {
			findOne = userDao.findOne(project.getOwnerId());
		}
		
		if (findOne == null) {
			findOne = new User();
			findOne.setFirstName("");
			findOne.setLastName("");
		}
		
		return new ProjectPagelistDTO(project, findOne);
	}
	
}
