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
package com.tocea.corolla.users.handlers;

import javax.transaction.Transactional;
import javax.validation.Valid;

import org.javers.core.Javers;
import org.springframework.beans.factory.annotation.Autowired;

import com.tocea.corolla.cqrs.annotations.CommandHandler;
import com.tocea.corolla.cqrs.handler.ICommandHandler;
import com.tocea.corolla.users.commands.CreateRoleCommand;
import com.tocea.corolla.users.dao.IRoleDAO;
import com.tocea.corolla.users.domain.Role;
import com.tocea.corolla.users.exceptions.InvalidRoleInformationException;
import com.tocea.corolla.users.exceptions.MissingRoleInformationException;
import com.tocea.corolla.users.exceptions.RoleAlreadyExistException;
import com.tocea.corolla.users.service.RolePermissionService;

/**
 * @author sleroy
 *
 */
@CommandHandler
@Transactional
public class CreateRoleCommandHandler implements
ICommandHandler<CreateRoleCommand, Role> {

	@Autowired
	private IRoleDAO				roleDAO;

	@Autowired
	private RolePermissionService rolePermissionService;

	@Override
	public Role handle(@Valid final CreateRoleCommand _command) {
		final Role role = _command.getRole();
		if (role == null) {
			throw new MissingRoleInformationException("No data provided to create role");
		}
		if (role.getId() != null) {
			throw new InvalidRoleInformationException("No ID expected");
		}
		if (this.roleDAO.findRoleByName(role.getName()) != null) {
			throw new RoleAlreadyExistException(role.getName());
		}
		this.rolePermissionService.checkPermissions(role.getPermissions());
		this.roleDAO.save(role);
		
		return role;
	}


}
