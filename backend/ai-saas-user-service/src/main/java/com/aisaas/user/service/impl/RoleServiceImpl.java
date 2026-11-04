package com.aisaas.user.service.impl;

import com.aisaas.user.entity.UserRole;
import com.aisaas.user.entity.UserRoleRelation;
import com.aisaas.user.mapper.UserRoleMapper;
import com.aisaas.user.mapper.UserRoleRelationMapper;
import com.aisaas.user.service.RoleService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements RoleService {

    private final UserRoleMapper userRoleMapper;
    private final UserRoleRelationMapper userRoleRelationMapper;

    @Override
    public UserRole getByCode(String roleCode) {
        return userRoleMapper.selectByCode(roleCode);
    }

    @Override
    public List<UserRole> getUserRoles(Long userId) {
        return userRoleMapper.selectRolesByUserId(userId);
    }

    @Override
    public List<String> getUserRoleCodes(Long userId) {
        List<UserRole> roles = getUserRoles(userId);
        return roles.stream()
                .map(UserRole::getRoleCode)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasRole(Long userId, String roleCode) {
        return userRoleRelationMapper.countByUserIdAndRoleCode(userId, roleCode) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRole(Long userId, Long roleId, Long operatorId) {
        // 检查是否已存在
        UserRoleRelation existRelation = userRoleRelationMapper.selectByUserIdAndRoleId(userId, roleId);
        if (existRelation != null) {
            // 已存在，更新状态
            existRelation.setStatus(1);
            existRelation.setUpdatedAt(LocalDateTime.now());
            userRoleRelationMapper.updateById(existRelation);
            return;
        }

        // 创建新关联
        UserRoleRelation relation = new UserRoleRelation();
        relation.setUserId(userId);
        relation.setRoleId(roleId);
        relation.setGrantType(2); // 手动分配
        relation.setGrantedBy(operatorId);
        relation.setEffectiveAt(LocalDateTime.now());
        relation.setStatus(1);
        relation.setIsDeleted(0);
        relation.setCreatedAt(LocalDateTime.now());
        relation.setUpdatedAt(LocalDateTime.now());

        userRoleRelationMapper.insert(relation);
        log.info("为用户分配角色成功: userId={}, roleId={}", userId, roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds, Long operatorId) {
        // 移除现有的所有角色
        removeAllRoles(userId);
        
        // 分配新角色
        for (Long roleId : roleIds) {
            assignRole(userId, roleId, operatorId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRole(Long userId, Long roleId) {
        UserRoleRelation relation = userRoleRelationMapper.selectByUserIdAndRoleId(userId, roleId);
        if (relation != null) {
            relation.setStatus(0);
            relation.setIsDeleted(1);
            relation.setUpdatedAt(LocalDateTime.now());
            userRoleRelationMapper.updateById(relation);
            log.info("移除用户角色成功: userId={}, roleId={}", userId, roleId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAllRoles(Long userId) {
        List<UserRoleRelation> relations = userRoleRelationMapper.selectByUserId(userId);
        for (UserRoleRelation relation : relations) {
            relation.setStatus(0);
            relation.setIsDeleted(1);
            relation.setUpdatedAt(LocalDateTime.now());
            userRoleRelationMapper.updateById(relation);
        }
        log.info("移除用户所有角色成功: userId={}", userId);
    }

    @Override
    public List<UserRole> getAllActiveRoles() {
        return userRoleMapper.selectAllActiveRoles();
    }
}
