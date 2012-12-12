/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.security.authorization;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.RoleDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.RoleEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.orm.entities.UserEntityPK;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * Provides high-level access to Users and Roles in database
 */
@Singleton
public class Users {

  @Inject
  protected UserDAO userDAO;
  @Inject
  protected RoleDAO roleDAO;
  @Inject
  protected PasswordEncoder passwordEncoder;
  @Inject
  protected Configuration configuration;


  public List<User> getAllUsers() {
    List<UserEntity> userEntities = userDAO.findAll();
    List<User> users = new ArrayList<User>(userEntities.size());

    for (UserEntity userEntity : userEntities) {
      users.add(new User(userEntity));
    }

    return users;
  }
  
  public User getAnyUser(String userName) {
    UserEntity userEntity = userDAO.findLdapUserByName(userName);
    if (null == userEntity) {
      userEntity = userDAO.findLocalUserByName(userName);
    }
    
    return (null == userEntity) ? null : new User(userEntity);
  }

  public User getLocalUser(String userName) throws AmbariException{
    UserEntity userEntity = userDAO.findLocalUserByName(userName);
    if (userEntity == null) {
      throw new AmbariException("User doesn't exist");
    }
    return new User(userEntity);
  }

  public User getLdapUser(String userName) throws AmbariException{
    UserEntity userEntity = userDAO.findLdapUserByName(userName);
    if (userEntity == null) {
      throw new AmbariException("User doesn't exist");
    }
    return new User(userEntity);
  }

  /**
   * Modifies password of local user
   * @throws AmbariException
   */
  public synchronized void modifyPassword(String userName, String oldPassword, String newPassword) throws AmbariException {
    UserEntity userEntity = userDAO.findLocalUserByName(userName);
    if (userEntity != null) {
      if (passwordEncoder.matches(oldPassword, userEntity.getUserPassword())) {
        userEntity.setUserPassword(passwordEncoder.encode(newPassword));
        userDAO.merge(userEntity);
      } else {
        throw new AmbariException("Wrong password provided");
      }

    } else {
      userEntity = userDAO.findLdapUserByName(userName);
      if (userEntity != null) {
        throw new AmbariException("Password of LDAP user cannot be modified");
      } else {
        throw new AmbariException("User " + userName + " not found");
      }
    }
  }

  /**
   * Creates new local user with provided userName and password
   */
  @Transactional
  public synchronized void createUser(String userName, String password) {
    UserEntity userEntity = new UserEntity();
    userEntity.setUserName(userName);
    userEntity.setUserPassword(passwordEncoder.encode(password));
    userEntity.setRoleEntities(new HashSet<RoleEntity>());

    RoleEntity roleEntity = roleDAO.findByName(getUserRole());
    if (roleEntity == null) {
      createRole(getUserRole());
    }

    userEntity.getRoleEntities().add(roleDAO.findByName(getUserRole()));

    userDAO.create(userEntity);
  }

  @Transactional
  public synchronized void removeUser(User user) throws AmbariException {
    UserEntity userEntity = userDAO.findByPK(getPK(user));
    if (userEntity != null) {
      userDAO.remove(userEntity);
    } else {
      throw new AmbariException("User " + user + " doesn't exist");
    }
  }

  /**
   * Grants ADMIN role to provided user
   * @throws AmbariException
   */
  public synchronized void promoteToAdmin(User user) throws AmbariException{
    addRoleToUser(user, getAdminRole());
  }

  /**
   * Removes ADMIN role form provided user
   * @throws AmbariException
   */
  public synchronized void demoteAdmin(User user) throws AmbariException {
    removeRoleFromUser(user, getAdminRole());
  }

  @Transactional
  public synchronized void addRoleToUser(User user, String role) throws AmbariException {
    UserEntityPK pk = getPK(user);

    UserEntity userEntity = userDAO.findByPK(pk);
    if (userEntity == null) {
      throw new AmbariException("User " + user + " doesn't exist");
    }

    RoleEntity roleEntity = roleDAO.findByName(role);
    if (roleEntity == null) {
      throw new AmbariException("Role " + role + " doesn't exist");
    }

    if (!userEntity.getRoleEntities().contains(roleEntity)) {
      userEntity.getRoleEntities().add(roleEntity);
      userDAO.merge(userEntity);
    } else {
      throw new AmbariException("User " + user + " already owns role " + role);
    }

  }

  @Transactional
  public synchronized void removeRoleFromUser(User user, String role) throws AmbariException {
    UserEntityPK pk = getPK(user);
    UserEntity userEntity = userDAO.findByPK(pk);
    if (userEntity == null) {
      throw new AmbariException("User " + user + " doesn't exist");
    }

    RoleEntity roleEntity = roleDAO.findByName(role);
    if (roleEntity == null) {
      throw new AmbariException("Role " + role + " doesn't exist");
    }

    if (userEntity.getRoleEntities().contains(roleEntity)) {
      userEntity.getRoleEntities().remove(roleEntity);
      userDAO.merge(userEntity);
    } else {
      throw new AmbariException("User " + user + " doesn't own role " + role);
    }

  }

  public String getUserRole() {
    return configuration.getConfigsMap().get(Configuration.USER_ROLE_NAME_KEY);
  }

  public String getAdminRole() {
    return configuration.getConfigsMap().get(Configuration.ADMIN_ROLE_NAME_KEY);
  }

  /**
   * Creates new role
   */
  public void createRole(String role) {
    RoleEntity roleEntity = new RoleEntity();
    roleEntity.setRoleName(role);
    roleDAO.create(roleEntity);
  }

  /**
   * Creates ADMIN adn USER roles if not present
   */
  public synchronized void createDefaultRoles() {
    if (roleDAO.findByName(getUserRole()) == null) {
      createRole(getUserRole());
    }
    if (roleDAO.findByName(getAdminRole()) == null) {
      createRole(getAdminRole());
    }
  }

  private UserEntityPK getPK(User user) {
    UserEntityPK pk = new UserEntityPK();
    pk.setUserName(user.getUserName());
    pk.setLdapUser(user.isLdapUser());
    return pk;
  }
}
