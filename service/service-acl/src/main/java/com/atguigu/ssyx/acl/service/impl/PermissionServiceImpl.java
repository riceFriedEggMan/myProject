package com.atguigu.ssyx.acl.service.impl;

import com.atguigu.ssyx.acl.mapper.PermissionMapper;
import com.atguigu.ssyx.acl.service.PermissionService;
import com.atguigu.ssyx.acl.service.RolePermissionService;
import com.atguigu.ssyx.acl.utils.PermissionHelper;
import com.atguigu.ssyx.model.acl.Permission;
import com.atguigu.ssyx.model.acl.RolePermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    @Autowired
    private RolePermissionService rolePermissionService;

    //查询所有菜单
    @Override
    public List<Permission> queryAllPermission() {
        //1 查询所有菜单
        List<Permission> allPermissionList =
                baseMapper.selectList(null);

        //2 转换要求数据格式
        List<Permission> result = PermissionHelper.buildPermission(allPermissionList);
        return result;
    }

    //递归删除菜单
    @Override
    public void removeChildById(Long id) {
        //1 创建list集合idList，封装所有要删除菜单id
        List<Long> idList = new ArrayList<>();

        //根据当前菜单id，获取当前菜单下面所有子菜单，
        //如果子菜单下面还有子菜单，都要获取到
        //重点：递归找当前菜单子菜单
        this.getAllPermissionId(id,idList);

        //设置当前菜单id
        idList.add(id);

        //调用方法根据多个菜单id删除
        baseMapper.deleteBatchIds(idList);
    }

    @Override
    public List<Permission> getPermissionByRoleId(Long roleId) {
        List<Permission> permissions = baseMapper.selectList(null);

        LambdaQueryWrapper<RolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePermission::getRoleId, roleId);
        List<RolePermission> rolePermissionList = rolePermissionService.list(wrapper);
        List<Long> permissionId = rolePermissionList.stream()
                .map(RolePermission::getPermissionId).collect(Collectors.toList());
        List<Permission> assignPermissionList = new ArrayList<>();
        for (Permission permission : permissions) {
            if (permissionId.contains(permission.getId())) {
                assignPermissionList.add(permission);
            }
        }

        //用户分配角色列表
        return permissions;
    }

    @Override
    public void saveRolePermissions(Long roleId, Long[] permissions) {
        LambdaQueryWrapper<RolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePermission::getRoleId, roleId);
        rolePermissionService.remove(wrapper);

        List<RolePermission> rolePermissionList = new ArrayList<>();
        for (Long permissionId : permissions) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(roleId);
            rolePermission.setPermissionId(permissionId);
            rolePermissionList.add(rolePermission);
        }
        rolePermissionService.saveBatch(rolePermissionList);
    }

    //重点：递归找当前菜单下面的所有子菜单
    //第一个参数是当前菜单id
    //第二个参数最终封装list集合，包含所有菜单id
    private void getAllPermissionId(Long id, List<Long> idList) {
        //根据当前菜单id查询下面子菜单
        //select * from permission where pid=2
        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Permission::getPid,id);
        List<Permission> childList = baseMapper.selectList(wrapper);

        //递归查询是否还有子菜单，有继续递归查询
        childList.stream().forEach(item -> {
            //封装菜单id到idList集合里面
            idList.add(item.getId());
            //递归
            this.getAllPermissionId(item.getId(),idList);
        });
    }
}
