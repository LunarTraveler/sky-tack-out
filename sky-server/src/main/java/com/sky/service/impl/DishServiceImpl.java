package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DishServiceImpl implements DishService {

    private final DishMapper dishMapper;

    private final DishFlavorMapper dishFlavorMapper;

    private final SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品和对应的口味
     * @param dishDTO
     * @return
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        log.info("dish {}",dish);

        // 向菜品表插入一条数据(主键回显)
        dishMapper.insert(dish);
        Long dishId = dish.getId();
        // 向口味表插入多条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(flavor -> flavor.setDishId(dishId));
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品分页展示
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult page(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.page(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 批量删除菜品
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        // 首先判断这些菜品是否存在起售中的菜品
        for (Long id : ids) {
            Dish dish = dishMapper.selectById(id);
            if (dish.getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        // 其次不能和套餐表有相连的关系
        List<Long> setmealIds = setmealDishMapper.getSetmealDishIdsByDishId(ids);
        if (setmealIds != null && setmealIds.size() > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

//        // 删除菜品表中的菜品数据
//        for (Long id : ids) {
//            dishMapper.delete(id);
//            // 删除与这些菜品相关联的口味表的数据
//            dishFlavorMapper.deleteByDishId(id);
//        }

        // 进行优化，发送多条sql语句压力太大，合并为一条sql语句
        dishMapper.deleteBatch(ids);
        dishFlavorMapper.deleteBatchByDishId(ids);

    }

    /**
     * 根据id获取菜品信息以及对应的口味信息
     * @param id
     */
    @Override
    public DishVO getDishById(Long id) {
        // 查找菜品信息
        Dish dish = dishMapper.selectById(id);
        // 查找对应的口味信息
        List<DishFlavor> flavors = dishFlavorMapper.selectByDisId(id);

        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(flavors);

        return dishVO;
    }

    /**
     * 更新菜品和对应的口味
     * @param dishDTO
     */
    @Override
    public void updateDishWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        // 先修改dish表的基本信息
        dishMapper.update(dish);
        // 再把flavor表的信息删除在添加
        Long dishId = dish.getId();
        dishFlavorMapper.deleteByDishId(dishId);

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(flavor -> flavor.setDishId(dishId));
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 启用和禁用变换
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
            .status(status)
            .id(id)
            .build();
        
        dishMapper.update(dish);
    }

    /**
     * 根据分类id查询菜品集合
     * 好像只要是查询出来的都要按照时间排序一下，那么都是要写xml文件的
     * @param categoryId
     * @return
     */
    @Override
    public List<Dish> list(Long categoryId) {
        // 注意状态应该是启用的状态
        // 传递的参数要不然是实体类，要么是map集合
        Map<String, Object> paramters = new HashMap<>();
        paramters.put("categoryId", categoryId);
        paramters.put("status", StatusConstant.ENABLE);
        List<Dish> dishList = dishMapper.list(paramters);
        return dishList;
    }

    /**
     * 条件查询菜品和口味
     * @param params
     * @return
     */
    @Override
    public List<DishVO> listWithFlavor(Map<String, Object> params) {
        List<Dish> dishList = dishMapper.list(params);
        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish dish : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(dish, dishVO);

            // 根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.selectByDisId(dish.getId());
            dishVO.setFlavors(flavors);

            dishVOList.add(dishVO);
        }
        return dishVOList;
    }
}
