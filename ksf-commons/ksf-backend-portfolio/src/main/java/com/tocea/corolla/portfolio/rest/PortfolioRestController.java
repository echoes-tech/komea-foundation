/*
 * Corolla - A Tool to manage software requirements and test cases
 * Copyright (C) 2015 Tocea
 *
 * This file is part of Corolla.
 *
 * Corolla is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License,
 * or any later version.
 *
 * Corolla is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Corolla.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.tocea.corolla.portfolio.rest;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;
import com.tocea.corolla.cqrs.gate.Gate;
import com.tocea.corolla.portfolio.commands.ChangePortfolioFolderNodeTypeCommand;
import com.tocea.corolla.portfolio.commands.CreatePortfolioFolderNodeCommand;
import com.tocea.corolla.portfolio.commands.EditPortfolioFolderNodeCommand;
import com.tocea.corolla.portfolio.commands.MovePortfolioNodeCommand;
import com.tocea.corolla.portfolio.commands.RemovePortfolioNodeCommand;
import com.tocea.corolla.portfolio.dao.IPortfolioDAO;
import com.tocea.corolla.portfolio.domain.Portfolio;
import com.tocea.corolla.portfolio.predicates.FindNodeByProjectIDPredicate;
import com.tocea.corolla.portfolio.utils.PortfolioUtils;
import com.tocea.corolla.portfolio.visitors.PortfolioJsTree;
import com.tocea.corolla.products.dao.IProjectDAO;
import com.tocea.corolla.products.domain.Project;
import com.tocea.corolla.trees.dao.IFolderNodeTypeDAO;
import com.tocea.corolla.trees.domain.FolderNode;
import com.tocea.corolla.trees.domain.FolderNodeType;
import com.tocea.corolla.trees.domain.TreeNode;
import com.tocea.corolla.trees.dto.JsTreeNodeDTO;
import com.tocea.corolla.trees.services.ITreeManagementService;
import com.tocea.corolla.users.domain.Permission;

@RestController
@RequestMapping("/rest/portfolio")
public class PortfolioRestController {
	
	@Autowired
	private IPortfolioDAO portfolioDAO;

	@Autowired
	private IProjectDAO projectDAO;

	@Autowired
	private IFolderNodeTypeDAO folderNodeTypeDAO;

	@Autowired
	private Gate gate;

	@Autowired
	private ITreeManagementService treeManagementService;

	@RequestMapping(value = "/folders/add/{parentID}/{folderNodeTypeID}", method = RequestMethod.POST, consumes = "text/plain")
	@Secured({ Permission.PORTFOLIO_MANAGEMENT })
	public FolderNode addTextNode(@PathVariable final Integer parentID, @PathVariable final String folderNodeTypeID, @RequestBody final String text)  {

		final FolderNodeType folderNodeType = folderNodeTypeDAO.findOne(folderNodeTypeID);

		return gate.dispatch(new CreatePortfolioFolderNodeCommand(text, folderNodeType, parentID));
	}

	@RequestMapping(value = "/folders/add/{folderNodeTypeID}", method = RequestMethod.POST, consumes = "text/plain")
	@Secured({ Permission.PORTFOLIO_MANAGEMENT })
	public FolderNode addTextNode(@PathVariable final String folderNodeTypeID, @RequestBody final String text)  {

		return addTextNode(null, folderNodeTypeID, text);
	}

	@RequestMapping(value = "/folders/edit/type/{nodeID}/{typeID}")
	@Secured({ Permission.PORTFOLIO_MANAGEMENT })
	public FolderNode changeFolderNodeType(@PathVariable final Integer nodeID, @PathVariable final String typeID)  {

		return gate.dispatch(new ChangePortfolioFolderNodeTypeCommand(nodeID, typeID));
	}

	@RequestMapping(value = "/folders/edit/{nodeID}", method = RequestMethod.POST, consumes = "text/plain")
	@Secured({ Permission.PORTFOLIO_MANAGEMENT })
	public Portfolio editTextNode(@PathVariable final Integer nodeID, @RequestBody final String text)  {

		return gate.dispatch(new EditPortfolioFolderNodeCommand(nodeID, text));
	}

	@RequestMapping(value = "/jstree")
	@PreAuthorize("isAuthenticated()")
	public Collection<JsTreeNodeDTO> getJsTree() {

		final Portfolio portfolio = portfolioDAO.find();

		return buildJsTree(portfolio.getNodes());
	}

	@RequestMapping(value = "/jstree/{projectKey}")
	@PreAuthorize("isAuthenticated()")
	public Collection<JsTreeNodeDTO> getJsTreeSubtree(@PathVariable final String projectKey) {

		final Project project = projectDAO.findByKey(projectKey);

		if (project != null) {

			final Portfolio portfolio = portfolioDAO.find();

			final TreeNode node = treeManagementService.findNode(portfolio.getNodes(), new FindNodeByProjectIDPredicate(project.getId()));

			if (node != null) {
				return buildJsTree(Lists.newArrayList(node.getNodes()));
			}

		}

		return Lists.newArrayList();
	}

	@RequestMapping(value = "/")
	@PreAuthorize("isAuthenticated()")
	public Collection<TreeNode> getTree() {

		final Portfolio portfolio = portfolioDAO.find();

		return portfolio != null ? portfolio.getNodes() : new ArrayList<TreeNode>();
	}

	@RequestMapping(value = "/move/{fromID}/{toID}")
	@Secured({ Permission.PORTFOLIO_MANAGEMENT })
	public Portfolio moveNode(@PathVariable final Integer fromID, @PathVariable final Integer toID)  {

		return gate.dispatch(new MovePortfolioNodeCommand(fromID, toID != 0 ? toID : null));
	}

	@RequestMapping(value = "/remove/{nodeID}")
	@Secured({ Permission.PORTFOLIO_MANAGEMENT })
	public Portfolio removeNode(@PathVariable final Integer nodeID)  {

		return gate.dispatch(new RemovePortfolioNodeCommand(nodeID));
	}

	private Collection<JsTreeNodeDTO> buildJsTree(final Collection<TreeNode> treeNodes) {

		final Collection<String> ids = PortfolioUtils.getProjectIDs(treeNodes);

		final Collection<Project> projects = Lists.newArrayList(projectDAO.findAll(ids));

		final Collection<JsTreeNodeDTO> nodes = Lists.newArrayList();

		for(final TreeNode node : treeNodes) {

			final PortfolioJsTree visitor = new PortfolioJsTree(projects);
			node.accept(visitor);
			nodes.add(visitor.getResult());

		}
		
		return nodes;

	}

}