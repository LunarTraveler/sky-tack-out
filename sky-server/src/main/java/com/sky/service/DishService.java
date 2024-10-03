package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    void saveWithFlavor(DishDTO dishDTO);

    PageResult page(DishPageQueryDTO dishPageQueryDTO);

    void deleteBatch(List<Long> ids);

    DishVO getDishById(Long id);

    void updateDishWithFlavor(DishDTO dishDTO);

    void startOrStop(Integer status, Long id);
}
